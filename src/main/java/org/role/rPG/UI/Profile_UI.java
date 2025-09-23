package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.role.rPG.Player.StatManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 1. BaseUI를 상속받도록 변경합니다.
public class Profile_UI extends BaseUI {

    private final JavaPlugin plugin;
    private final Player viewer; // UI를 보고 있는 플레이어
    private final StatManager statManager;
    private BukkitTask refreshTask; // 1초마다 UI를 갱신할 스케줄러 작업

    public Profile_UI(JavaPlugin plugin, Player viewer, StatManager statManager) {
        // 2. 부모 생성자를 호출하여 GUI 기본 틀을 만듭니다.
        super(54, Component.text("프로필", NamedTextColor.BLUE));
        this.plugin = plugin;
        this.viewer = viewer;
        this.statManager = statManager;
    }

    // 3. UI가 열릴 때 갱신 작업을 시작합니다.
    @Override
    public void openInventory(Player p) {
        super.openInventory(p); // 인벤토리를 엽니다.

        // 1초(20틱)마다 updateItems() 메소드를 실행하는 작업을 시작합니다.
        this.refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateItems, 0L, 20L);
    }

    // 4. UI가 닫힐 때 갱신 작업을 중지하는 메소드를 만듭니다. (GUI_manager에서 호출)
    public void cancelRefreshTask() {
        if (this.refreshTask != null && !this.refreshTask.isCancelled()) {
            this.refreshTask.cancel();
        }
    }

    // 5. 1초마다 호출되어 아이템(스탯 정보)을 업데이트하는 핵심 로직입니다.
    private void updateItems() {
        // 플레이어가 다른 창을 열었다면 작업을 중지합니다.
        if (!viewer.getOpenInventory().getTopInventory().equals(getInventory())) {
            cancelRefreshTask();
            return;
        }

        UUID uuid = viewer.getUniqueId();

        // 플레이어 머리 아이템 생성
        ItemStack profileHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) profileHead.getItemMeta();
        headMeta.setOwningPlayer(viewer);
        headMeta.displayName(Component.text(viewer.getName() + "님의 정보", NamedTextColor.YELLOW));

        // 스탯 정보를 담을 설명(lore) 생성
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("")); // 공백 한 줄

        // StatManager에서 최종 계산된 스탯을 가져와서 추가
        double health = statManager.getFinalStat(uuid, "MAX_HEALTH");
        double defense = statManager.getFinalStat(uuid, "DEFENSE");
        double strength = statManager.getFinalStat(uuid, "STRENGTH");
        double maxMana = statManager.getFinalStat(uuid, "MAX_MANA");
        double currentMana = statManager.getFinalStat(uuid, "CURRENT_MANA");
        double critChance = statManager.getFinalStat(uuid, "CRIT_CHANCE");
        double critDamage = statManager.getFinalStat(uuid, "CRIT_DAMAGE");
        double speed = statManager.getFinalStat(uuid, "SPEED");

        lore.add(createStatLine("체력", health, "❤"));
        lore.add(createStatLine("방어력", defense, "⛨"));
        lore.add(createStatLine("힘", strength, "❁"));
        lore.add(createStatLine("마나", maxMana, "\uD83D\uDD2E"));
        lore.add(createStatLine("치명타 확률", critChance, "%"));
        lore.add(createStatLine("치명타 피해", critDamage, "%"));
        lore.add(createStatLine("이동 속도", speed, "%"));

        headMeta.lore(lore);
        profileHead.setItemMeta(headMeta);

        // GUI의 13번 슬롯에 아이템을 설정합니다.
        getInventory().setItem(13, profileHead);
    }

    // 스탯 설명(lore) 한 줄을 쉽게 만들기 위한 헬퍼 메소드
    private Component createStatLine(String name, double value, String symbol) {
        return Component.text(" " + symbol + " " + name + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", value), NamedTextColor.GREEN));
    }

    // 이 UI에서는 아이템을 클릭(이동)할 수 없도록 막습니다.
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    // 이 메소드는 더 이상 직접 사용하지 않습니다. updateItems()가 실질적인 역할을 합니다.
    // UI가 처음 열릴 때 배경과 고정된 장비 아이템을 설정합니다.
    @Override
    protected void initializeItems(Player player) {
        Graypanefiller.fillBackground(getInventory());
        displayPlayerEquipment(player);
    }

    // 플레이어의 장비를 가져와 GUI에 한 번만 표시하는 메서드
    private void displayPlayerEquipment(Player player) {
        ItemStack[] armor = player.getEquipment().getArmorContents();
        ItemStack handItem = player.getEquipment().getItemInMainHand();

        // 슬롯 번호: 헬멧(11), 상의(20), 하의(29), 신발(38), 손(47)
        getInventory().setItem(11, armor[3] != null && armor[3].getType() != Material.AIR ? armor[3] : createEmptySlotItem()); // Helmet
        getInventory().setItem(20, armor[2] != null && armor[2].getType() != Material.AIR ? armor[2] : createEmptySlotItem()); // Chestplate
        getInventory().setItem(29, armor[1] != null && armor[1].getType() != Material.AIR ? armor[1] : createEmptySlotItem()); // Leggings
        getInventory().setItem(38, armor[0] != null && armor[0].getType() != Material.AIR ? armor[0] : createEmptySlotItem()); // Boots
        getInventory().setItem(47, handItem.getType() != Material.AIR ? handItem : createEmptySlotItem()); // Main Hand
    }

    private ItemStack createEmptySlotItem() {
        return createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "비었음", NamedTextColor.GRAY);
    }

    private ItemStack createItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}