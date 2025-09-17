package org.role.rPG.Item;

import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.role.rPG.Player.StatManager;
import org.role.rPG.RPG;

public class EquipmentListener implements Listener {

    private final StatManager statManager;
    private final RPG plugin;

    private static final double VANILLA_HEALTH_SCALE = 20.0;

    public EquipmentListener(RPG plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    // 스탯 업데이트를 약간 지연시켜 여러 이벤트가 동시에 발생해도 한 번만 계산하도록 합니다.
    private void scheduleStatUpdate(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                statManager.updatePlayerStats(player);
            }
        }.runTaskLater(plugin, 1L); // 1틱(0.05초) 뒤에 실행
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleStatUpdate(event.getPlayer());
        event.getPlayer().setHealthScale(VANILLA_HEALTH_SCALE);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            scheduleStatUpdate((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            scheduleStatUpdate((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        scheduleStatUpdate(event.getPlayer());
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        scheduleStatUpdate(event.getPlayer());
    }

    @EventHandler
    public void onItemPickup(PlayerPickItemEvent event) {
        scheduleStatUpdate(event.getPlayer());
    }
}