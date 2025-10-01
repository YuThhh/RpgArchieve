package org.role.rPG.Player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Item.ItemType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 플레이어의 장신구 데이터를 관리하고, 장착/해제 로직을 처리하는 클래스입니다.
 */
public class AccessoryManager {

    private final ItemManager itemManager;
    private final StatManager statManager;
    // 플레이어 UUID와 장착한 장신구 배열(슬롯 0: 액티브, 1-3: 패시브)을 매핑하여 저장합니다.
    private final Map<UUID, ItemStack[]> equippedAccessories = new HashMap<>();

    public AccessoryManager(ItemManager itemManager, StatManager statManager) {
        this.itemManager = itemManager;
        this.statManager = statManager;
    }

    /**
     * 지정된 슬롯에 장신구를 장착합니다.
     * @param player 대상 플레이어
     * @param slot UI에서의 실제 인벤토리 슬롯 번호 (11, 14, 15, 16)
     * @param accessory 장착할 장신구 아이템
     */
    public void equipAccessory(Player player, int slot, ItemStack accessory) {
        if (itemManager.getItemType(accessory) != ItemType.ACCESSORY) {
            return; // 장신구 타입이 아니면 장착 불가
        }

        UUID playerUUID = player.getUniqueId();
        equippedAccessories.putIfAbsent(playerUUID, new ItemStack[4]); // 플레이어 데이터가 없으면 초기화

        int accessoryIndex = getAccessoryIndex(slot);
        if (accessoryIndex == -1) return; // 유효하지 않은 슬롯이면 중단

        // 기존에 아이템이 있었다면 해제 처리 (플레이어에게 아이템을 돌려주는 로직은 UI에서 처리)
        if (getEquippedAccessories(player)[accessoryIndex] != null) {
            unequipAccessory(player, slot);
        }

        equippedAccessories.get(playerUUID)[accessoryIndex] = accessory.clone();
        statManager.updatePlayerStats(player); // 스탯 업데이트
    }

    /**
     * 지정된 슬롯의 장신구를 해제합니다.
     * @param player 대상 플레이어
     * @param slot UI에서의 실제 인벤토리 슬롯 번호
     */
    public void unequipAccessory(Player player, int slot) {
        UUID playerUUID = player.getUniqueId();
        if (!equippedAccessories.containsKey(playerUUID)) {
            return;
        }

        int accessoryIndex = getAccessoryIndex(slot);
        if (accessoryIndex == -1) return;

        equippedAccessories.get(playerUUID)[accessoryIndex] = null;
        statManager.updatePlayerStats(player); // 스탯 업데이트
    }

    /**
     * 플레이어가 장착한 모든 장신구 아이템 배열을 반환합니다.
     * @param player 대상 플레이어
     * @return 장신구 ItemStack 배열 (크기 4), 데이터가 없으면 빈 배열
     */
    public ItemStack[] getEquippedAccessories(Player player) {
        return equippedAccessories.getOrDefault(player.getUniqueId(), new ItemStack[4]);
    }

    /**
     * UI 인벤토리 슬롯 번호를 내부 데이터 배열 인덱스(0-3)로 변환합니다.
     * @param slot UI 슬롯 번호
     * @return 내부 배열 인덱스, 유효하지 않으면 -1
     */
    private int getAccessoryIndex(int slot) {
        switch (slot) {
            case 11: return 0; // 액티브
            case 14: return 1; // 패시브 1
            case 15: return 2; // 패시브 2
            case 16: return 3; // 패시브 3
            default: return -1;
        }
    }
}