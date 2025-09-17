package org.role.rPG.Item;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public class ItemUpdateListener implements Listener {

    private final ItemManager itemManager;

    public ItemUpdateListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (itemManager.updateItemIfNecessary(item)) {
            player.sendMessage(Component.text("손에 든 아이템이 최신 버전으로 업데이트되었습니다."));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (itemManager.updateItemIfNecessary(item)) {
            event.getWhoClicked().sendMessage(Component.text("인벤토리의 아이템이 최신 버전으로 업데이트되었습니다."));
        }
    }
}