package org.role.rPG.Indicator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
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

        spawnLocation.getWorld().spawn(spawnLocation, TextDisplay.class, textDisplay -> {
            // 1. 텍스트 내용과 색상을 설정합니다.
            textDisplay.text(Component.text(String.format("-%.1f", damage), NamedTextColor.RED));

            // 2. 항상 플레이어를 바라보도록 설정합니다 (빌보드 효과).
            textDisplay.setBillboard(Display.Billboard.CENTER);

            // 3. (선택) 텍스트 그림자 효과를 줍니다.
            textDisplay.setShadowed(true);

            // 4. (선택) 텍스트 배경을 투명하게 만듭니다.
            textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            // 5. 텍스트가 즉시 사라지지 않도록 지속시간을 설정합니다.
            // (TextDisplay는 Invulnerable, Gravity, Marker 등이 필요 없습니다.)

            // 위로 올라가다 사라지는 효과는 동일하게 적용됩니다.
            new BukkitRunnable() {
                private int ticksLived = 0;
                private final int duration = 25; // 지속시간을 약간 줄여도 좋습니다.

                @Override
                public void run() {
                    if (ticksLived > duration || textDisplay.isDead()) {
                        textDisplay.remove();
                        this.cancel();
                        return;
                    }

                    textDisplay.teleport(textDisplay.getLocation().add(0, 0.08, 0));
                    ticksLived++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }
}