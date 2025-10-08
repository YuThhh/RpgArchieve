package org.role.rPG.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.role.rPG.Effect.Effect;
import org.role.rPG.Effect.EffectManager;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Item.ItemType;

import java.util.*;

public class AccessoryManager {

    private final ItemManager itemManager;
    private final StatManager statManager;
    private final EffectManager effectManager;
    private final Map<UUID, ItemStack[]> equippedAccessories = new HashMap<>();
    private final Map<UUID, List<String>> activePassiveEffects = new HashMap<>();

    public AccessoryManager(ItemManager itemManager, StatManager statManager, EffectManager effectManager) {
        this.itemManager = itemManager;
        this.statManager = statManager;
        this.effectManager = effectManager;
    }

    public void loadAccessories(Player player) {
        UUID playerUUID = player.getUniqueId();
        ItemStack[] loadedAccessories = StatDataManager.getPlayerAccessories(playerUUID);
        equippedAccessories.put(playerUUID, loadedAccessories);
    }

    public void saveAccessories(Player player) {
        UUID playerUUID = player.getUniqueId();
        ItemStack[] accessoriesToSave = equippedAccessories.getOrDefault(playerUUID, new ItemStack[4]);
        StatDataManager.setPlayerAccessories(playerUUID, accessoriesToSave);
    }

    public void equipAccessory(Player player, int slot, ItemStack accessory) {
        ItemType type = itemManager.getItemType(accessory);
        // [수정] || (OR) 가 아닌 && (AND) 를 사용해야 합니다.
        // "패시브도 아니고, 액티브도 아닐 때" 장착을 막아야 합니다.
        if (type != ItemType.PASSIVE_ACCESSORY && type != ItemType.ACTIVE_ACCESSORY) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        equippedAccessories.putIfAbsent(playerUUID, new ItemStack[4]);
        int accessoryIndex = getAccessoryIndex(slot);
        if (accessoryIndex == -1) return;
        equippedAccessories.get(playerUUID)[accessoryIndex] = accessory.clone();
        statManager.updatePlayerStats(player);
        updatePassiveEffects(player);
    }

    // ... (unequipAccessory, updatePassiveEffects 등 나머지 코드는 모두 동일) ...
    public void unequipAccessory(Player player, int slot) {
        UUID playerUUID = player.getUniqueId();
        if (!equippedAccessories.containsKey(playerUUID)) {
            return;
        }

        int accessoryIndex = getAccessoryIndex(slot);
        if (accessoryIndex == -1) return;

        if (equippedAccessories.get(playerUUID)[accessoryIndex] != null) {
            equippedAccessories.get(playerUUID)[accessoryIndex] = null;
            statManager.updatePlayerStats(player);

            updatePassiveEffects(player);
        }
    }

    public void updatePassiveEffects(Player player) {
        UUID playerUUID = player.getUniqueId();

        List<String> previouslyActive = activePassiveEffects.getOrDefault(playerUUID, new ArrayList<>());
        for (String effectName : previouslyActive) {
            Effect effect = effectManager.getEffect(effectName);
            if (effect != null) {
                effect.removeEffect(player);
            }
        }
        activePassiveEffects.put(playerUUID, new ArrayList<>());

        for (ItemStack accessory : getEquippedAccessories(player)) {
            if (accessory == null || !accessory.hasItemMeta() || !accessory.getItemMeta().hasLore()) {
                continue;
            }

            for (Component lineComponent : Objects.requireNonNull(accessory.getItemMeta().lore())) {
                String plainLine = PlainTextComponentSerializer.plainText().serialize(lineComponent);
                if (plainLine.startsWith("효과:")) {
                    String effectName = plainLine.substring(plainLine.indexOf(":") + 1).trim();
                    Effect effect = effectManager.getEffect(effectName);

                    if (effect != null && !(effect instanceof org.role.rPG.Effect.effects.Bleeding)) {
                        effect.getEffect(player, player);
                        activePassiveEffects.get(playerUUID).add(effectName);
                    }
                }
            }
        }
    }

    public ItemStack[] getEquippedAccessories(Player player) {
        return equippedAccessories.getOrDefault(player.getUniqueId(), new ItemStack[4]);
    }

    private int getAccessoryIndex(int slot) {
        return switch (slot) {
            case 11 -> 0;
            case 14 -> 1;
            case 15 -> 2;
            case 16 -> 3;
            default -> -1;
        };
    }
}