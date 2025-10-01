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
 * 장신구 UI를 관리하는 클래스입니다.
 */
public class Accessories_UI extends BaseUI {

    private final AccessoryManager accessoryManager;
    private final ItemManager itemManager;
    // 클릭 가능한 장신구 슬롯 목록
    private static final List<Integer> ACCESSORY_SLOTS = Arrays.asList(11, 14, 15, 16);

    /**
     * Accessories_UI의 생성자입니다.
     */
    public Accessories_UI(AccessoryManager accessoryManager, ItemManager itemManager) {
        super(27, Component.text("장신구", NamedTextColor.GOLD));
        this.accessoryManager = accessoryManager;
        this.itemManager = itemManager;
    }

    /**
     * 장신구 UI의 아이템을 초기화하고 배치합니다.
     * @param player UI를 보는 플레이어
     */
    @Override
    protected void initializeItems(Player player) {
        // 1. 배경을 회색 유리판으로 채웁니다.
        Graypanefiller.fillBackground(inv);

        // 2. 플레이어가 이미 장착한 장신구를 불러와서 배치합니다.
        ItemStack[] equipped = accessoryManager.getEquippedAccessories(player);
        if (equipped.length == 4) {
            inv.setItem(11, equipped[0]); // 액티브
            inv.setItem(14, equipped[1]); // 패시브 1
            inv.setItem(15, equipped[2]); // 패시브 2
            inv.setItem(16, equipped[3]); // 패시브 3
        }
    }

    /**
     * 장신구 UI 내에서 발생하는 클릭 이벤트를 처리합니다.
     * @param event 인벤토리 클릭 이벤트
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true); // 기본적으로 모든 클릭은 취소

        Player player = (Player) event.getWhoClicked();
        ItemStack cursorItem = event.getCursor(); // 플레이어가 들고 있는 아이템
        ItemStack clickedItem = event.getCurrentItem(); // 클릭한 슬롯의 아이템
        int clickedSlot = event.getSlot();

        // 클릭한 슬롯이 장신구 슬롯이 아니면 아무것도 하지 않음
        if (!ACCESSORY_SLOTS.contains(clickedSlot)) {
            return;
        }

        // Case 1: 장신구 해제 (클릭한 슬롯에 아이템이 있고, 커서가 비어있음)
        if (clickedItem != null && cursorItem.getType() == Material.AIR) {
            accessoryManager.unequipAccessory(player, clickedSlot); // 매니저를 통해 해제
            player.getInventory().addItem(clickedItem); // 인벤토리로 아이템 반환
            inv.setItem(clickedSlot, null); // UI 슬롯 비우기
        }
        // Case 2: 장신구 장착 (클릭한 슬롯이 비어있고, 커서에 장신구 아이템이 있음)
        else if (clickedItem == null && cursorItem.getType() != Material.AIR) {
            if (itemManager.getItemType(cursorItem) == ItemType.ACCESSORY) {
                accessoryManager.equipAccessory(player, clickedSlot, cursorItem); // 매니저를 통해 장착
                inv.setItem(clickedSlot, cursorItem.clone()); // UI에 아이템 복사본 배치
                event.getWhoClicked().setItemOnCursor(null); // 커서의 아이템 제거
            }
        }
    }
}