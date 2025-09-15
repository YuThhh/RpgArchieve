package org.role.rPG;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Indicater implements Listener {

    private final IndicatorManager indicatorManager;

    public Indicater(IndicatorManager indicatorManager) {
        this.indicatorManager = indicatorManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // We only want to show an indicator if the damage was caused by a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        // We also only want to show it on living entities that are not Armor Stands
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof TextDisplay) {
            return;
        }

        // Get the entity that was damaged
        Entity damagedEntity = event.getEntity();
        double damage = event.getFinalDamage();

        // Show the indicator at the entity's location
        indicatorManager.showDamageIndicator(damagedEntity.getLocation(), damage);
    }
}