package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private static final int ACTIVE_SLOT = 11;
    private static final List<Integer> PASSIVE_SLOTS = Arrays.asList(14, 15, 16);

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
     * [최종 수정] IDE 경고를 제거하고 로직을 최적화한 최종 버전입니다.
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true); // 기본 동작 제어

        Player player = (Player) event.getWhoClicked();
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();
        int clickedSlot = event.getRawSlot();

        if (clickedSlot >= inv.getSize()) {
            event.setCancelled(false);
            return;
        }

        if (clickedSlot != ACTIVE_SLOT && !PASSIVE_SLOTS.contains(clickedSlot)) {
            return;
        }

        boolean cursorIsEmpty = (cursorItem == null || cursorItem.getType().isAir());
        boolean slotIsEmpty = (clickedItem == null || clickedItem.getType().isAir());

        // Case 1: 장신구 해제 (슬롯 O, 커서 X) - 가장 간단한 경우를 먼저 처리하고 종료
        if (!slotIsEmpty && cursorIsEmpty) {
            accessoryManager.unequipAccessory(player, clickedSlot);
            player.setItemOnCursor(clickedItem);
            inv.setItem(clickedSlot, null);
            return; // 작업이 끝났으므로 더 이상 진행하지 않음
        }

        // Case 2 & 3: 커서에 아이템이 있는 모든 경우 (장착 또는 교체)
        if (!cursorIsEmpty) {
            ItemType cursorItemType = itemManager.getItemType(cursorItem);
            boolean canEquip = (clickedSlot == ACTIVE_SLOT && cursorItemType == ItemType.ACTIVE_ACCESSORY) ||
                    (PASSIVE_SLOTS.contains(clickedSlot) && cursorItemType == ItemType.PASSIVE_ACCESSORY);

            // 커서의 아이템을 해당 슬롯에 놓을 수 없다면 아무것도 하지 않음
            if (!canEquip) {
                return;
            }

            // 아이템을 놓을 수 있는 것이 확실해졌으므로, 장착(빈 슬롯)과 교체(찬 슬롯)를 구분
            if (slotIsEmpty) { // Case 2: 장착
                accessoryManager.equipAccessory(player, clickedSlot, cursorItem);
                inv.setItem(clickedSlot, cursorItem);
                player.setItemOnCursor(null);
            } else { // Case 3: 교체
                // 'canEquip' 검사를 통과했으므로 슬롯에 있는 아이템은 신경 쓸 필요 없이 교체
                accessoryManager.unequipAccessory(player, clickedSlot);
                accessoryManager.equipAccessory(player, clickedSlot, cursorItem);
                inv.setItem(clickedSlot, cursorItem);
                player.setItemOnCursor(clickedItem);
            }
        }
    }
}