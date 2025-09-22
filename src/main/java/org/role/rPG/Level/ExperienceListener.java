package org.role.rPG.Level;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.role.rPG.Mob.CustomMob;
import org.role.rPG.Mob.MobManager;
import org.role.rPG.Player.PER_DATA;

public class ExperienceListener implements Listener {

    private final LevelManager levelManager;
    private final MobManager mobManager;

    public ExperienceListener(LevelManager levelManager, MobManager mobManager) {
        this.levelManager = levelManager;
        this.mobManager = mobManager;
    }


    // 몬스터 사냥 시 경험치 획득
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            Entity deadEntity = event.getEntity();

            // 몬스터를 죽였을 때만 경험치 지급
            CustomMob customMob = mobManager.getCustomMob(deadEntity);

            if (customMob != null) {
                // 커스텀 몹일 경우, 해당 몹이 가진 경험치를 가져옵니다.
                double experienceToGive = customMob.getProficiencyExp();
                if (experienceToGive > 0) {
                    levelManager.addProficiencyExp(killer, PER_DATA.COMBAT_PROFICIENCY, experienceToGive);
                }

            } else if (deadEntity instanceof Monster) {
                // 커스텀 몹이 아닌 일반 몬스터일 경우, 기본 경험치를 지급합니다.
                levelManager.addExperience(killer, 5); // 예: 일반 몬스터는 5 경험치
            }
        }
    }

    // 블록 파괴 시 경험치 획득
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        Material blockType = event.getBlock().getType();

        // 특정 광물을 캤을 때 채광 숙련도 경험치 지급
        if (blockType == Material.COAL_ORE || blockType == Material.DEEPSLATE_COAL_ORE) {
            levelManager.addProficiencyExp(player, PER_DATA.MINING_PROFICIENCY, 3);
        } else if (blockType == Material.IRON_ORE || blockType == Material.DEEPSLATE_IRON_ORE) {
            levelManager.addProficiencyExp(player, PER_DATA.MINING_PROFICIENCY, 5);
        } else if (blockType == Material.DIAMOND_ORE || blockType == Material.DEEPSLATE_DIAMOND_ORE) {
            levelManager.addProficiencyExp(player, PER_DATA.MINING_PROFICIENCY, 20);
        } else if (blockType == Material.STONE) {
            levelManager.addProficiencyExp(player, PER_DATA.MINING_PROFICIENCY, 0.5);
        }
    }
}