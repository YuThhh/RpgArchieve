package org.role.rPG;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class IndicatorManager {

    private final JavaPlugin plugin;

    public IndicatorManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showDamageIndicator(Location location, double damage) {
        // Add a random offset to the location to make indicators appear in a small radius
        Location spawnLocation = location.clone().add(
                ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                -0.5, // Start the indicator slightly below the entity's feet
                ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
        );

        // Spawn the Armor Stand
        spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class, armorStand -> {
            armorStand.setGravity(false); // So it doesn't fall
            armorStand.setVisible(false); // So we only see the name
            armorStand.setMarker(true); // Makes it have a very small hitbox and not interact with the world
            armorStand.setInvulnerable(true); // Prevents it from being destroyed
            armorStand.setCustomNameVisible(true); // Makes the name always visible
            armorStand.customName(Component.text(damage, NamedTextColor.RED)); // Set the name to the damage value

            // Create a task to move the indicator up and then remove it
            new BukkitRunnable() {
                private int ticksLived = 0;
                private final int duration = 30; // The indicator will last for 1.5 seconds (30 ticks)

                @Override
                public void run() {
                    if (ticksLived > duration || armorStand.isDead()) {
                        armorStand.remove();
                        this.cancel();
                        return;
                    }

                    // Move the armor stand up smoothly
                    armorStand.teleport(armorStand.getLocation().add(0, 0.05, 0));
                    ticksLived++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }
}