package org.role.rPG.Level;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.role.rPG.Player.PER_DATA;
import org.role.rPG.Player.StatManager;
import org.role.rPG.RPG;

public class LevelManager {

    private final RPG plugin;
    private final PER_DATA perData;
    private final StatManager statManager;

    public LevelManager(RPG plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.perData = PER_DATA.getInstance();
    }

    /**
     * 특정 레벨에 도달하기 위해 필요한 총 경험치를 계산합니다.
     * @param level 목표 레벨
     * @return 필요한 경험치
     */
    public double getRequiredExperience(int level) {
        // 유명한 RPG 게임에서 자주 쓰이는 레벨업 공식 (조정 가능)
        // 5 * (level^2) + 50 * level + 100
        return 5 * Math.pow(level, 2) + 50 * level + 100;
    }

    /**
     * 플레이어에게 메인 경험치를 추가하고 레벨업을 확인합니다.
     * @param player 경험치를 받을 플레이어
     * @param amount 획득할 경험치 양
     */
    public void addExperience(Player player, double amount) {
        int currentLevel = perData.getPlayerLevel(player.getUniqueId());
        double currentExp = perData.getPlayerExperience(player.getUniqueId());

        currentExp += amount;

        double requiredExp = getRequiredExperience(currentLevel);

        // 여러 번 레벨업 할 수도 있으므로 while 문 사용
        while (currentExp >= requiredExp) {
            currentExp -= requiredExp;
            currentLevel++;

            // 레벨업 처리
            player.sendMessage(NamedTextColor.AQUA + "=============================");
            player.sendMessage(NamedTextColor.GOLD + "          LEVEL UP!          ");
            player.sendMessage(NamedTextColor.WHITE + "         Level " + (currentLevel - 1) + " -> " + NamedTextColor.GREEN + "Level " + currentLevel);
            player.sendMessage(NamedTextColor.AQUA + "=============================");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // 다음 레벨업에 필요한 경험치 재계산
            requiredExp = getRequiredExperience(currentLevel);
        }

        // 변경된 데이터 저장
        perData.setPlayerLevel(player.getUniqueId(), currentLevel);
        perData.setPlayerExperience(player.getUniqueId(), currentExp);

        // 스탯 업데이트 호출 (레벨업 보너스 적용을 위해)
        statManager.updatePlayerStats(player);
    }

    /**
     * 플레이어에게 특정 숙련도 경험치를 추가하고 레벨업을 확인합니다.
     * @param player 경험치를 받을 플레이어
     * @param proficiency 숙련도 종류 (예: PER_DATA.COMBAT_PROFICIENCY)
     * @param amount 획득할 경험치 양
     */
    public void addProficiencyExp(Player player, String proficiency, double amount) {
        int currentLevel = perData.getProficiencyLevel(player.getUniqueId(), proficiency);
        double currentExp = perData.getProficiencyExperience(player.getUniqueId(), proficiency);

        currentExp += amount;
        double requiredExp = getRequiredExperience(currentLevel); // 메인 레벨과 같은 공식 사용

        while (currentExp >= requiredExp) {
            currentExp -= requiredExp;
            currentLevel++;

            // 숙련도 레벨업 처리
            player.sendMessage(NamedTextColor.GREEN + proficiency + " 숙련도 레벨업! " + (currentLevel - 1) + " -> " + currentLevel);
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

            requiredExp = getRequiredExperience(currentLevel);
        }

        // 변경된 데이터 저장
        perData.setProficiencyLevel(player.getUniqueId(), proficiency, currentLevel);
        perData.setProficiencyExperience(player.getUniqueId(), proficiency, currentExp);

        // 스탯 업데이트 호출 (숙련도 보너스 적용을 위해)
        statManager.updatePlayerStats(player);
    }
}