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
    private final Map<UUID, ItemStack[]> equippedAccessories = new HashMap<>();

    public AccessoryManager(ItemManager itemManager, StatManager statManager) {
        this.itemManager = itemManager;
        this.statManager = statManager;
    }

    /**
     * 지정된 슬롯에 장신구를 장착하고 스탯을 즉시 업데이트합니다.
     */
    public void equipAccessory(Player player, int slot, ItemStack accessory) {
        if (itemManager.getItemType(accessory) != ItemType.ACCESSORY) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        equippedAccessories.putIfAbsent(playerUUID, new ItemStack[4]);

        int accessoryIndex = getAccessoryIndex(slot);
        if (accessoryIndex == -1) return;

        equippedAccessories.get(playerUUID)[accessoryIndex] = accessory.clone();
        statManager.updatePlayerStats(player); // 즉시 스탯 업데이트
    }

    /**
     * 지정된 슬롯의 장신구를 해제하고 스탯을 즉시 업데이트합니다.
     */
    public void unequipAccessory(Player player, int slot) {
        UUID playerUUID = player.getUniqueId();
        if (!equippedAccessories.containsKey(playerUUID)) {
            return;
        }

        int accessoryIndex = getAccessoryIndex(slot);
        if (accessoryIndex == -1) return;

        // 해당 슬롯에 아이템이 있을 때만 스탯 업데이트 호출
        if (equippedAccessories.get(playerUUID)[accessoryIndex] != null) {
            equippedAccessories.get(playerUUID)[accessoryIndex] = null;
            statManager.updatePlayerStats(player); // 즉시 스탯 업데이트
        }
    }

    public ItemStack[] getEquippedAccessories(Player player) {
        return equippedAccessories.getOrDefault(player.getUniqueId(), new ItemStack[4]);
    }

    private int getAccessoryIndex(int slot) {
        return switch (slot) {
            case 11 -> 0; // 액티브
            case 14 -> 1; // 패시브 1
            case 15 -> 2; // 패시브 2
            case 16 -> 3; // 패시브 3
            default -> -1;
        };
    }
}