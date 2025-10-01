package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Item.ItemType;
import org.role.rPG.Player.AccessoryManager;

import java.util.Arrays;
import java.util.List;

/**
 * 장신구 UI를 관리하는 클래스입니다. (즉시 적용 방식으로 수정됨)
 */
public class Accessories_UI extends BaseUI {

    private final AccessoryManager accessoryManager;
    private final ItemManager itemManager;
    private static final List<Integer> ACCESSORY_SLOTS = Arrays.asList(11, 14, 15, 16);

    public Accessories_UI(AccessoryManager accessoryManager, ItemManager itemManager) {
        super(27, Component.text("장신구", NamedTextColor.GOLD));
        this.accessoryManager = accessoryManager;
        this.itemManager = itemManager;
    }

    @Override
    protected void initializeItems(Player player) {
        Graypanefiller.fillBackground(inv);
        ItemStack[] equipped = accessoryManager.getEquippedAccessories(player);
        if (equipped.length == 4) {
            inv.setItem(11, equipped[0]);
            inv.setItem(14, equipped[1]);
            inv.setItem(15, equipped[2]);
            inv.setItem(16, equipped[3]);
        }
    }

    /**
     * [수정] 아이템을 클릭할 때마다 즉시 스탯이 적용되도록 로직을 변경합니다.
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true); // 기본 동작을 막고 아래 로직으로만 제어

        Player player = (Player) event.getWhoClicked();
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();
        int clickedSlot = event.getRawSlot(); // 플레이어 인벤토리 클릭 감지를 위해 getRawSlot() 사용

        // UI 바깥 (플레이어 인벤토리 등)을 클릭했다면 아무것도 하지 않음
        if (clickedSlot >= inv.getSize()) {
            event.setCancelled(false); // 플레이어 인벤토리 내에서의 움직임은 허용
            return;
        }

        // 클릭한 슬롯이 장신구 슬롯이 아니면 동작을 막음 (회색 유리판 등)
        if (!ACCESSORY_SLOTS.contains(clickedSlot)) {
            return;
        }

        boolean cursorIsEmpty = (cursorItem == null || cursorItem.getType().isAir());
        boolean slotIsEmpty = (clickedItem == null || clickedItem.getType().isAir());

        // Case 1: 장신구 해제 (슬롯에 아이템 O, 커서 X)
        if (!slotIsEmpty && cursorIsEmpty) {
            accessoryManager.unequipAccessory(player, clickedSlot);
            player.setItemOnCursor(clickedItem);
            inv.setItem(clickedSlot, null);
        }
        // Case 2: 장신구 장착 (슬롯에 아이템 X, 커서 O)
        else if (slotIsEmpty && !cursorIsEmpty) {
            if (itemManager.getItemType(cursorItem) == ItemType.ACCESSORY) {
                accessoryManager.equipAccessory(player, clickedSlot, cursorItem);
                inv.setItem(clickedSlot, cursorItem);
                player.setItemOnCursor(null);
            }
        }
        // Case 3: 장신구 교체 (슬롯에 아이템 O, 커서 O)
        else if (!slotIsEmpty && !cursorIsEmpty) {
            if (itemManager.getItemType(cursorItem) == ItemType.ACCESSORY) {
                // 기존 아이템(clickedItem) 해제
                accessoryManager.unequipAccessory(player, clickedSlot);
                // 새 아이템(cursorItem) 장착
                accessoryManager.equipAccessory(player, clickedSlot, cursorItem);

                // 아이템 스왑
                inv.setItem(clickedSlot, cursorItem);
                player.setItemOnCursor(clickedItem);
            }
        }
    }
}