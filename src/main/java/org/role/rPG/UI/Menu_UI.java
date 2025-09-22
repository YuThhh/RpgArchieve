package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.role.rPG.Player.PER_DATA;

public class Menu_UI extends BaseUI {

    public Menu_UI() {
        // 부모 클래스(BaseUI)의 생성자를 호출하여 54칸짜리 GUI를 만듭니다.
        super(54, Component.text("메뉴", NamedTextColor.BLUE));
    }

    @Override
    protected void initializeItems(Player player) {
        // 기존 initializeItems() 로직과 동일
        Graypanefiller.fillBackground(inv);

        inv.setItem(13, createItem(Material.PLAYER_HEAD, "프로필", NamedTextColor.YELLOW));
        inv.setItem(20, createItem(Material.END_CRYSTAL, "칭호", NamedTextColor.GREEN));
        inv.setItem(21, createItem(Material.NETHER_STAR, "퀘스트", NamedTextColor.BLUE));
        inv.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, "숙련도", NamedTextColor.RED));
        inv.setItem(23, createItem(Material.CRAFTING_TABLE, "제작대", NamedTextColor.DARK_GREEN));
        inv.setItem(24, createItem(Material.CHEST, "창고", NamedTextColor.GREEN));
        inv.setItem(31, createItem(Material.BOOK, "도감", NamedTextColor.DARK_PURPLE));
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
                new Profile_UI().openInventory(player);
                break;
            case CHEST:
                ItemStack[] playerData = PER_DATA.getInstance().getPlayerStorage(player.getUniqueId());
                new Storage_UI(playerData).openInventory(player);
                break;
            case BARRIER:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    // 아이템 생성 중복을 줄이기 위한 유틸리티 메서드
    private ItemStack createItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        item.setItemMeta(meta);
        return item;
    }
}