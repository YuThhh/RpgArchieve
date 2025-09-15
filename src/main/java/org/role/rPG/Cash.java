package org.role.rPG;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cash implements Listener {

    private static final Map<UUID, Integer> playerMoney = new HashMap<>();

    public static void register(RPG rpg) {
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack itemInHand = event.getItem();

        if ((action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) || itemInHand == null) {
            return;
        }
        if (!itemInHand.hasItemMeta()) return;
        ItemMeta meta = itemInHand.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            Integer amountObj = container.get(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER);
            if (amountObj == null) return;
            int amount = amountObj;

            if (player.isSneaking()) {
                int totalAmount = 0;
                int totalItemsUsed = 0;
                PlayerInventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack currentItem = inventory.getItem(i);
                    if (currentItem != null && currentItem.hasItemMeta()) {
                        PersistentDataContainer currentContainer = currentItem.getItemMeta().getPersistentDataContainer();
                        if (currentContainer.has(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER)) {
                            Integer currentAmountObj = currentContainer.get(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER);
                            if (currentAmountObj != null && currentAmountObj == amount) {
                                totalAmount += currentAmountObj * currentItem.getAmount();
                                totalItemsUsed += currentItem.getAmount();
                                inventory.setItem(i, null);
                            }
                        }
                    }
                }
                if (totalAmount > 0) {
                    addMoney(player, totalAmount);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
                    player.sendMessage("§e수표 " + totalItemsUsed + "장을 모두 사용하여 §6" + String.format("%,d", totalAmount) + "G§f를 획득했습니다.");
                }
            } else {
                addMoney(player, amount);
                itemInHand.setAmount(itemInHand.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage("§e" + String.format("%,d", amount) + "G§f를 획득했습니다.");
            }
        }
    }

    public static void addMoney(Player player, int amount) {
        playerMoney.put(player.getUniqueId(), getMoney(player) + amount);
    }
    public static void removeMoney(Player player, int amount) {
        playerMoney.put(player.getUniqueId(), getMoney(player) - amount);
    }
    public static int getMoney(Player player) {
        return playerMoney.getOrDefault(player.getUniqueId(), 0);
    }
    public static void unloadPlayerData(Player player) {
        playerMoney.remove(player.getUniqueId());
    }
}