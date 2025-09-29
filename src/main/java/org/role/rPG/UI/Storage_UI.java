package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin; // JavaPlugin 임포트
import org.role.rPG.Level.LevelManager;
import org.role.rPG.Player.PER_DATA;
import org.role.rPG.Player.StatManager; // StatManager 임포트

public class Storage_UI extends BaseUI {

    // ▼▼▼ [수정] 필드 추가 ▼▼▼
    private final JavaPlugin plugin;
    private final StatManager statManager;
    private final ItemStack[] storage;
    private final Player viewer;
    private final LevelManager levelManager;

    // ▼▼▼ [수정] 생성자 변경 ▼▼▼
    public Storage_UI(JavaPlugin plugin, StatManager statManager, ItemStack[] playerStorageData, Player viewer, LevelManager levelManager) {
        super(54, Component.text("창고", NamedTextColor.BLUE));
        this.plugin = plugin;
        this.statManager = statManager;
        this.storage = playerStorageData;
        this.viewer = viewer;
        this.levelManager = levelManager;
    }

    @Override
    protected void initializeItems(Player player) {
        // ... (기존 코드와 동일)
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, createGrayPane());
        }

        if (storage != null) {
            for (int i = 0; i < Math.min(storage.length, 45); i++) {
                inv.setItem(i, storage[i]);
            }
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("뒤로가기", NamedTextColor.RED));
        back.setItemMeta(backMeta);
        inv.setItem(49, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        Material clickedType = clickedItem.getType();

        if (clickedType == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true);
        }

        if (clickedType == Material.BARRIER) {
            event.setCancelled(true);
            // ▼▼▼ [수정] Menu_UI를 열 때 plugin과 statManager를 전달합니다. ▼▼▼
            new Menu_UI(plugin, statManager, viewer, levelManager).openInventory(player);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        // ... (기존 코드와 동일)
        Player player = (Player) event.getPlayer();
        ItemStack[] contents = new ItemStack[45];
        for (int i = 0; i < 45; i++) {
            contents[i] = event.getInventory().getItem(i);
        }
        PER_DATA.getInstance().savePlayerStorage(player.getUniqueId(), contents);
    }

    private ItemStack createGrayPane() {
        // ... (기존 코드와 동일)
        ItemStack grayGlassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlassPane.getItemMeta();
        grayMeta.displayName(Component.text(""));
        grayMeta.setHideTooltip(true);
        grayGlassPane.setItemMeta(grayMeta);
        return grayGlassPane;
    }
}