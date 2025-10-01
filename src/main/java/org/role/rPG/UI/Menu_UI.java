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
import org.role.rPG.Craft.CraftManager;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Level.LevelManager;
import org.role.rPG.Player.AccessoryManager;
import org.role.rPG.Player.PER_DATA;
import org.role.rPG.Player.StatManager;

public class Menu_UI extends BaseUI {

    private final JavaPlugin plugin;
    private final StatManager statManager;
    private final Player viewer; // UI를 보고 있는 플레이어
    private final LevelManager levelManager;
    private final CraftManager craftManager;
    private final AccessoryManager accessoryManager; // <-- Add this
    private final ItemManager itemManager;

    public Menu_UI(JavaPlugin plugin, StatManager statManager, Player viewer, LevelManager levelManager, CraftManager craftManager, AccessoryManager accessoryManager, ItemManager itemManager) {
        super(54, Component.text("메뉴", NamedTextColor.BLUE));
        this.plugin = plugin;
        this.statManager = statManager;
        this.viewer = viewer;
        this.levelManager = levelManager;
        this.craftManager = craftManager;
        this.accessoryManager = accessoryManager; // <-- Add this
        this.itemManager = itemManager;
    }

    @Override
    protected void initializeItems(Player player) {
        // 기존 initializeItems() 로직과 동일
        Graypanefiller.fillBackground(inv);

        inv.setItem(13, createHeadItem(Material.PLAYER_HEAD, "프로필", viewer, NamedTextColor.YELLOW));
        inv.setItem(20, createItem(Material.END_CRYSTAL, "칭호", NamedTextColor.BLUE));
        inv.setItem(21, createItem(Material.NETHER_STAR, "퀘스트", NamedTextColor.GREEN));
        inv.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, "숙련도", NamedTextColor.RED));
        inv.setItem(23, createItem(Material.CRAFTING_TABLE, "제작대", NamedTextColor.DARK_GREEN));
        inv.setItem(24, createItem(Material.CHEST, "창고", NamedTextColor.GREEN));
        inv.setItem(31, createItem(Material.BOOK, "도감", NamedTextColor.DARK_PURPLE));
        inv.setItem(32, createItem(Material.BUNDLE, "장신구", NamedTextColor.YELLOW));
        inv.setItem(45, createItem(Material.OAK_SIGN, "건의사항", NamedTextColor.DARK_GREEN));
        inv.setItem(46, createItem(Material.REDSTONE, "버그 제보", NamedTextColor.DARK_RED));
        inv.setItem(53, createItem(Material.BARRIER, "뒤로가기", NamedTextColor.RED));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true); // 기본적으로 아이템 이동 방지

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 클릭된 아이템의 타입으로 분기 처리
        switch (clickedItem.getType()) {
            case PLAYER_HEAD:
                new Profile_UI(plugin, player, statManager).openInventory(player);
                break;
            case CHEST:
                ItemStack[] playerData = PER_DATA.getInstance().getPlayerStorage(player.getUniqueId());
                new Storage_UI(plugin, statManager, playerData, viewer, levelManager, craftManager).openInventory(player);
                break;
            case BARRIER:
                player.closeInventory();
                break;
            case CRAFTING_TABLE:
                new Craft_UI(plugin, craftManager).openInventory(player);
                break;
            case EXPERIENCE_BOTTLE:
                new Level_UI(levelManager).openInventory(player);
                break;
            case BUNDLE:
                new Accessories_UI(accessoryManager, itemManager).openInventory(player);
                break;
            default:
                break;
        }
    }

    // 아이템 생성 중복을 줄이기 위한 유틸리티 메서드
    private ItemStack createItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHeadItem(Material material, String name, Player viewer, NamedTextColor color) {
        ItemStack Head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) Head.getItemMeta();
        headMeta.setOwningPlayer(viewer);
        headMeta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        Head.setItemMeta(headMeta);
        return Head;
    }
}