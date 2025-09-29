package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.role.rPG.Level.LevelManager;
import org.role.rPG.Player.PER_DATA;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 플레이어의 메인 레벨과 숙련도 정보를 보여주는 GUI 클래스입니다.
 */
public class Level_UI extends BaseUI {

    private final LevelManager levelManager;
    private final PER_DATA perData;

    /**
     * 레벨 UI를 생성합니다.
     * @param levelManager 경험치 및 레벨 계산을 위한 LevelManager 인스턴스
     */
    public Level_UI(LevelManager levelManager) {
        // 4줄(36칸) 크기의 인벤토리를 생성하고 제목을 "레벨 정보"로 설정합니다.
        super(36, Component.text("레벨 정보", NamedTextColor.AQUA));
        this.levelManager = levelManager;
        this.perData = PER_DATA.getInstance();
    }

    /**
     * UI에 표시될 아이템들을 설정합니다.
     * @param player UI를 열람하는 플레이어
     */
    @Override
    protected void initializeItems(Player player) {
        // UI 배경을 회색 유리판으로 채웁니다.
        Graypanefiller.fillBackground(inv);
        UUID playerUUID = player.getUniqueId();

        // 1. 메인 레벨 아이템 생성 및 배치
        int mainLevel = perData.getPlayerLevel(playerUUID);
        double mainCurrentExp = perData.getPlayerExperience(playerUUID);
        double mainRequiredExp = levelManager.getRequiredExperience(mainLevel);
        inv.setItem(13, createLevelItem(Material.EXPERIENCE_BOTTLE, "메인 레벨", mainLevel, mainCurrentExp, mainRequiredExp, NamedTextColor.AQUA));

        // 2. 전투 숙련도 아이템들 생성 및 배치
        // 근접 숙련도
        int meleeLevel = perData.getProficiencyLevel(playerUUID, PER_DATA.MELEE_COMBAT_PROFICIENCY);
        double meleeCurrentExp = perData.getProficiencyExperience(playerUUID, PER_DATA.MELEE_COMBAT_PROFICIENCY);
        double meleeRequiredExp = levelManager.getRequiredExperience(meleeLevel);
        inv.setItem(19, createLevelItem(Material.IRON_SWORD, "근접 숙련도", meleeLevel, meleeCurrentExp, meleeRequiredExp, NamedTextColor.RED));

        // 원거리 숙련도
        int rangedLevel = perData.getProficiencyLevel(playerUUID, PER_DATA.RANGED_COMBAT_PROFICIENCY);
        double rangedCurrentExp = perData.getProficiencyExperience(playerUUID, PER_DATA.RANGED_COMBAT_PROFICIENCY);
        double rangedRequiredExp = levelManager.getRequiredExperience(rangedLevel);
        inv.setItem(20, createLevelItem(Material.BOW, "원거리 숙련도", rangedLevel, rangedCurrentExp, rangedRequiredExp, NamedTextColor.GREEN));

        // 마법 숙련도 (PER_DATA에 "MAGIC_PROFICIENCY" 상수가 정의되어 있어야 합니다)
        int magicLevel = perData.getProficiencyLevel(playerUUID, PER_DATA.MAGIC_PROFICIENCY);
        double magicCurrentExp = perData.getProficiencyExperience(playerUUID, "MAGIC_PROFICIENCY");
        double magicRequiredExp = levelManager.getRequiredExperience(magicLevel);
        inv.setItem(21, createLevelItem(Material.ENCHANTING_TABLE, "마법 숙련도", magicLevel, magicCurrentExp, magicRequiredExp, NamedTextColor.LIGHT_PURPLE));


        // 3. 생활 숙련도 아이템들 생성 및 배치
        // 벌목 숙련도 (PER_DATA에 "WOODCUTTING_PROFICIENCY" 상수가 정의되어 있어야 합니다)
        int woodcuttingLevel = perData.getProficiencyLevel(playerUUID, PER_DATA.TIMBER_PROFICIENCY);
        double woodcuttingCurrentExp = perData.getProficiencyExperience(playerUUID, PER_DATA.TIMBER_PROFICIENCY);
        double woodcuttingRequiredExp = levelManager.getRequiredExperience(woodcuttingLevel);
        inv.setItem(23, createLevelItem(Material.IRON_AXE, "벌목 숙련도", woodcuttingLevel, woodcuttingCurrentExp, woodcuttingRequiredExp, NamedTextColor.DARK_GREEN));

        // 채집 숙련도 (PER_DATA에 "GATHERING_PROFICIENCY" 상수가 정의되어 있어야 합니다)
        int gatheringLevel = perData.getProficiencyLevel(playerUUID, PER_DATA.GATHERING_PROFICIENCY);
        double gatheringCurrentExp = perData.getProficiencyExperience(playerUUID, PER_DATA.GATHERING_PROFICIENCY);
        double gatheringRequiredExp = levelManager.getRequiredExperience(gatheringLevel);
        inv.setItem(24, createLevelItem(Material.WHEAT_SEEDS, "채집 숙련도", gatheringLevel, gatheringCurrentExp, gatheringRequiredExp, NamedTextColor.YELLOW));

        // 채광 숙련도
        int miningLevel = perData.getProficiencyLevel(playerUUID, PER_DATA.MINING_PROFICIENCY);
        double miningCurrentExp = perData.getProficiencyExperience(playerUUID, PER_DATA.MINING_PROFICIENCY);
        double miningRequiredExp = levelManager.getRequiredExperience(miningLevel);
        inv.setItem(25, createLevelItem(Material.IRON_PICKAXE, "채광 숙련도", miningLevel, miningCurrentExp, miningRequiredExp, NamedTextColor.GRAY));
    }

    /**
     * UI 내에서 아이템 클릭 시 이벤트를 처리합니다.
     * 이 UI는 정보만 보여주므로 모든 클릭 이벤트를 취소하여 아이템을 가져가지 못하게 막습니다.
     * @param event 인벤토리 클릭 이벤트
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    /**
     * 레벨 정보를 표시할 아이템을 생성하는 유틸리티 메서드입니다.
     * @param material 아이템 종류
     * @param name 레벨 이름 (예: "메인 레벨")
     * @param level 현재 레벨
     * @param currentExp 현재 경험치
     * @param requiredExp 다음 레벨업에 필요한 경험치
     * @param nameColor 이름에 적용할 색상
     * @return 생성된 ItemStack
     */
    private ItemStack createLevelItem(Material material, String name, int level, double currentExp, double requiredExp, NamedTextColor nameColor) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // 퍼센테이지 계산 및 포맷팅 (소수점 첫째 자리까지)
        double percentage = (requiredExp <= 0) ? 100.0 : (currentExp / requiredExp) * 100.0;
        String formattedPercentage = String.format("%.1f", percentage);

        // 아이템 이름 설정: "메인 레벨 LV.?[??.?%]" 형식
        meta.displayName(Component.text(name + " LV." + level, nameColor)
                .append(Component.text(" [" + formattedPercentage + "%]", NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));

        // 아이템 설명(Lore) 생성
        List<Component> lore = new ArrayList<>();
        Component progressBar = createProgressBar(currentExp, requiredExp, 10, '■');

        // 아이템 설명 설정: "LV.?[----------](??.?%)" 형식
        lore.add(Component.text("LV." + level + " ", NamedTextColor.GRAY)
                .append(progressBar)
                .append(Component.text(" (" + formattedPercentage + "%)", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        // 현재/필요 경험치 정보 추가
        lore.add(Component.text(String.format("   (EXP: %.1f / %.1f)", currentExp, requiredExp), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 경험치 바 컴포넌트를 생성합니다.
     * @param current 현재 값
     * @param max 최대 값
     * @param totalBars 전체 바의 길이
     * @param barChar 바를 채울 문자
     * @return 생성된 Component
     */
    private Component createProgressBar(double current, double max, int totalBars, char barChar) {
        if (max <= 0) max = 1; // 0으로 나누기 방지
        int progress = (int) ((current / max) * totalBars);

        return Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(barChar).repeat(Math.max(0, progress)), NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(barChar).repeat(Math.max(0, totalBars - progress)), NamedTextColor.DARK_GRAY))
                .append(Component.text("]", NamedTextColor.GRAY));
    }
}