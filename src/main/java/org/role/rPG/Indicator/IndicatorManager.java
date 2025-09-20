package org.role.rPG.Indicator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

public class IndicatorManager {

    private final JavaPlugin plugin;

    private static final TextColor NORMAL_DAMAGE_COLOR = TextColor.fromHexString("#FF3333");
    private static final TextColor CRITICAL_DAMAGE_COLOR = TextColor.fromHexString("#F72020");

    public IndicatorManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showDamageIndicator(Location location, double damage) {
        // Add a random offset to the location to make indicators appear in a small radius
        Location spawnLocation = location.clone().add(
                ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                0.5, // Start the indicator slightly below the entity's feet
                ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
        );

        spawnLocation.getWorld().spawn(spawnLocation, TextDisplay.class, textDisplay -> {
            // 1. 텍스트 내용과 색상을 설정합니다.
            textDisplay.text(Component.text(String.format("-%.1f", damage), NORMAL_DAMAGE_COLOR));

            // 2. 항상 플레이어를 바라보도록 설정합니다 (빌보드 효과).
            textDisplay.setBillboard(Display.Billboard.CENTER);

            // 3. (선택) 텍스트 그림자 효과를 줍니다.
            textDisplay.setShadowed(true);

            // 4. (선택) 텍스트 배경을 투명하게 만듭니다.
            textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            // 위로 올라가다 사라지는 효과는 동일하게 적용됩니다.
            new BukkitRunnable() {
                private int ticksLived = 0;
                private final int duration = 20; // 지속시간을 약간 줄여도 좋습니다.

                @Override
                public void run() {
                    if (ticksLived > duration || textDisplay.isDead()) {
                        textDisplay.remove();
                        this.cancel();
                        return;
                    }

                    textDisplay.teleport(textDisplay.getLocation().add(0, 0.08, 0));
                    textDisplay.setTeleportDuration(1);
                    ticksLived++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }

    /**
     * 크리티컬 대미지를 위한 더 눈에 띄는 인디케이터를 표시합니다.
     * @param location 표시될 위치
     * @param damage 표시될 대미지
     */
    public void showCriticalDamageIndicator(Location location, double damage) {
        Location spawnLocation = location.clone().add(
                ThreadLocalRandom.current().nextDouble(-0.7, 0.7), // 더 넓게 퍼짐
                0.0,
                ThreadLocalRandom.current().nextDouble(-0.7, 0.7)
        );

        spawnLocation.getWorld().spawn(spawnLocation, TextDisplay.class, textDisplay -> {
            // 1. 텍스트를 금색으로, 더 눈에 띄는 형식으로 설정합니다.
            textDisplay.text(Component.text(String.format("-%.1f", damage), CRITICAL_DAMAGE_COLOR));

            // 2. 빌보드 효과
            textDisplay.setBillboard(Display.Billboard.CENTER);

            // 3. 그림자 효과
            textDisplay.setShadowed(true);

            // 4. 배경 투명
            textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            // 5. 텍스트를 더 크게 만듭니다.
            Transformation transformation = textDisplay.getTransformation();
            textDisplay.setTransformation(new Transformation(
                    transformation.getTranslation(),
                    transformation.getLeftRotation(),
                    new Vector3f(1.2f, 1.2f, 1.2f), // 40% 더 크게
                    transformation.getRightRotation()
            ));

            new BukkitRunnable() {
                private int ticksLived = 0;
                private final int duration = 25; // 약간 더 오래 지속

                @Override
                public void run() {
                    if (ticksLived > duration || textDisplay.isDead()) {
                        textDisplay.remove();
                        this.cancel();
                        return;
                    }

                    // 더 빠르게 위로 올라갑니다.
                    textDisplay.teleport(textDisplay.getLocation().add(0, 0.1, 0));
                    textDisplay.setTeleportDuration(1);
                    ticksLived++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }
}