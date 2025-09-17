package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.role.rPG.Player.PER_DATA;

public class Storage_UI implements Listener, InventoryHolder {

    private final Inventory inv;
    private final ItemStack[] storage;

    public Storage_UI(ItemStack[] playerStorageData) {
        Component titleComponent = Component.text("프로필", NamedTextColor.BLUE); //GUI 이름
        inv = Bukkit.createInventory(this, 54, titleComponent); // GUI 칸 개수
        // 주입받은 데이터를 클래스 내부 변수(storage)에 저장
        this.storage = playerStorageData;
        initializeItems();
    }

    private void initializeItems() {
        // inv.setItem(슬롯, 아이템);
        // 슬롯 번호는 0부터 시작
        ItemStack grayGlassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlassPane.getItemMeta();
        grayMeta.setHideTooltip(true);
        grayGlassPane.setItemMeta(grayMeta);
        for (int i = 45; i < inv.getSize(); i++) {
            inv.setItem(i, grayGlassPane);
        }

        for (int i = 0; i < 45; i++) {
            if (storage != null && i < storage.length && storage[i] != null) {
                inv.setItem(i, storage[i]);
            }
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("뒤로가기", NamedTextColor.RED));
        back.setItemMeta(backMeta);

        inv.setItem(49,back);
    }

    public void openInventory(final Player p) {
        p.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getInventory().getHolder() instanceof Storage_UI)) {
            return;
        }

        // 클릭한 아이템이 없으면 무시
        if (event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Material clickedType = event.getCurrentItem().getType();

        // 1. 뒤로가기 버튼(BARRIER)을 클릭했을 때
        if (clickedType == Material.BARRIER) {
            event.setCancelled(true); // 아이템 이동 방지
            Menu_UI menu = new Menu_UI();
            menu.openInventory(player);
            return; // 다른 검사를 할 필요가 없으므로 여기서 종료
        }

        // 2. 장식용 유리판을 클릭했을 때
        if (clickedType == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true); // 아이템 이동 방지
        }
    }

    @EventHandler
    public void InventoryCloseEvent(InventoryCloseEvent event) {
        Player player =  (Player) event.getPlayer();

        if (event.getInventory().getHolder() instanceof Storage_UI) {
            // 중앙 관리소(RPG)에 접근해서 "이 플레이어의 창고는 이제 이 상태입니다"라고 알려줌
            PER_DATA.getInstance().savePlayerStorage(player.getUniqueId(), event.getInventory().getContents());
        }
    }
}
