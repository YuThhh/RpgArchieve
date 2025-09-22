package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.role.rPG.Player.PER_DATA;

// 1. BaseUI를 상속받고 Listener 구현을 제거합니다.
public class Storage_UI extends BaseUI {

    private final ItemStack[] storage;

    public Storage_UI(ItemStack[] playerStorageData) {
        // 2. 부모 생성자를 호출하여 GUI를 생성합니다.
        super(54, Component.text("창고", NamedTextColor.BLUE)); // "프로필"에서 "창고"로 이름 수정
        this.storage = playerStorageData;
    }

    @Override
    protected void initializeItems(Player player) {
        // 기존 initializeItems 로직과 동일
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
        // 3. onInventoryClick 로직을 handleClick으로 옮깁니다.
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) return;

        Material clickedType = clickedItem.getType();

        // 장식용 아이템 클릭 시 이벤트 취소
        if (clickedType == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true);
        }

        // 뒤로가기 버튼 클릭
        if (clickedType == Material.BARRIER) {
            event.setCancelled(true);
            new Menu_UI().openInventory(player);
        }
    }

    /**
     * GUIManager가 인벤토리가 닫힐 때 호출할 메서드입니다.
     * @param event InventoryCloseEvent
     */
    public void handleClose(InventoryCloseEvent event) {
        // 4. InventoryCloseEvent 로직을 handleClose로 옮깁니다.
        Player player = (Player) event.getPlayer();
        // 창고 영역(0-44 슬롯)의 아이템만 저장합니다.
        ItemStack[] contents = new ItemStack[45];
        for (int i = 0; i < 45; i++) {
            contents[i] = event.getInventory().getItem(i);
        }
        PER_DATA.getInstance().savePlayerStorage(player.getUniqueId(), contents);
    }

    private ItemStack createGrayPane() {
        ItemStack grayGlassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlassPane.getItemMeta();
        grayMeta.displayName(Component.text(" "));
        grayGlassPane.setItemMeta(grayMeta);
        return grayGlassPane;
    }
}