package org.role.rPG.Level;

import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.role.rPG.Effect.Effect;
import org.role.rPG.Effect.EffectManager;
import org.role.rPG.Mob.CustomMob;
import org.role.rPG.Mob.MobManager;
import org.role.rPG.Player.PER_DATA;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExperienceListener implements Listener {

    private final LevelManager levelManager;
    private final MobManager mobManager;
    private final EffectManager effectManager;

    // ▼▼▼ [추가] 플레이어별 타격 경험치 쿨다운을 관리하기 위한 Map ▼▼▼
    // Key: 플레이어 UUID, Value: 마지막으로 경험치를 획득한 시간 (밀리초)
    private final Map<UUID, Long> lastHitExpTime = new HashMap<>();
    private static final long HIT_EXP_COOLDOWN = 500L; // 0.5초 (밀리초 단위)

    public ExperienceListener(LevelManager levelManager, MobManager mobManager, EffectManager effectManager) {
        this.levelManager = levelManager;
        this.mobManager = mobManager;
        this.effectManager = effectManager; // [추가]
    }


    // 몬스터 사냥 시 경험치 획득
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 이 데미지가 출혈 등 커스텀 효과로 인한 것인지 먼저 확인합니다.
        if (event.getEntity() instanceof LivingEntity victim) {
            for (Effect effect : effectManager.getAllEffects()) {
                if (victim.hasMetadata(effect.getMarkerKey().getKey())) {
                    // 효과 데미지이므로, 숙련도 경험치를 주지 않고 즉시 종료합니다.
                    return;
                }
            }
        }

        // 공격자가 플레이어가 아니면 무시
        if (!(event.getDamager() instanceof Player) && !(event.getDamager() instanceof Arrow)) return;
        // 피격자가 몬스터 또는 커스텀 몹이 아니면 무시 (플레이어간 전투 등 제외)
        if (!(event.getEntity() instanceof Monster) && mobManager.getCustomMob(event.getEntity()) == null) return;

        Player attacker;
        // 공격자가 화살인 경우, 쏜 사람을 공격자로 설정
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else {
            return; // 플레이어의 공격이 아니면 종료
        }

        UUID attackerUUID = attacker.getUniqueId();
        CustomMob customMob = mobManager.getCustomMob(event.getEntity());

        // 1. 쿨다운 확인
        long currentTime = System.currentTimeMillis();
        long lastTime = lastHitExpTime.getOrDefault(attackerUUID, 0L);

        if (currentTime - lastTime < HIT_EXP_COOLDOWN) {
            return; // 쿨다운이 지나지 않았으면 경험치를 주지 않고 종료
        }

        // 2. 쿨다운 통과 시, 경험치 지급 및 시간 기록
        lastHitExpTime.put(attackerUUID, currentTime);

        // 3. 근접/원거리 숙련도에 따라 다른 경험치 지급
        // (이 값들은 나중에 config.yml 등으로 옮겨서 관리하는 것이 좋습니다)
        int playerMeleeProficiencyLevel = PER_DATA.getInstance().getProficiencyLevel(attackerUUID,"MELEE_COMBAT");
        int playerRangedProficiencyLevel = PER_DATA.getInstance().getProficiencyLevel(attackerUUID,"RANGED_COMBAT");

        double meleeExp = customMob.getProficiencyExp();
        double rangedExp = customMob.getProficiencyExp();

        if (event.getDamager() instanceof Arrow) {
            // 원거리 공격일 경우
            levelManager.addProficiencyExp(attacker, PER_DATA.RANGED_COMBAT_PROFICIENCY, Math.min(playerRangedProficiencyLevel * 5, rangedExp)); // RANGED_PROFICIENCY는 PER_DATA에 추가 필요
            attacker.sendMessage(Component.text("원거리 숙련도 " + Math.min(playerRangedProficiencyLevel * 5, rangedExp) + "획득"));
        } else {
            // 근접 공격일 경우
            levelManager.addProficiencyExp(attacker, PER_DATA.MELEE_COMBAT_PROFICIENCY, Math.min(playerRangedProficiencyLevel * 5, meleeExp)); // MELEE_PROFICIENCY는 PER_DATA에 추가 필요
            attacker.sendMessage(Component.text("근거리 숙련도 " + Math.min(playerRangedProficiencyLevel * 5, meleeExp) + "획득"));
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