package org.role.rPG;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;


public class STAT implements Listener {

    private final double CUSTOM_MAX_HEALTH = 100.0;
    private final PER_DATA data = PER_DATA.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 1. 플레이어의 실제 최대 체력을 100으로 설정합니다.
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(CUSTOM_MAX_HEALTH);

        // 2. 플레이어의 현재 체력을 최대로 채웁니다.
        player.setHealth(CUSTOM_MAX_HEALTH);

        // 3. 클라이언트에게 보여지는 체력 바를 20(하트 10칸)으로 고정합니다.
        player.setHealthScale(20.0);
        player.setHealthScaled(true); // 체력 스케일링 활성화

        // 플레이어가 접속하면 방어력 데이터가 있는지 확인하고 없으면 0으로 설정
        // data.getPlayerDefense()를 호출하여 PER_DATA에서 값을 가져옴
        if (data.getPlayerDefense(playerUUID) == 0.0) {
            data.setPlayerDefense(playerUUID, 0.0);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            double def = data.getPlayerDefense(p.getUniqueId());

            if (def > 0.0) {
                double original_damage = e.getDamage();
                double DR = -Math.pow(0.5,def/500);
                double final_damage = original_damage * DR;

                e.setDamage(final_damage);
            }
        }
    }

    @EventHandler
    public void EntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            double cirt = data.getPlayerCritDamage(p.getUniqueId());
            double critDamage = data.getPlayerCritDamage(p.getUniqueId());

            if (cirt > 0.0) {
                int random = new Random().nextInt(100) + 1;
                if (random <= cirt) {
                    double original_damage = e.getDamage();
                    double final_damage = original_damage * critDamage * 0.01;

                    e.setDamage(final_damage);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            if (e.isCancelled()) {
                double original_damage = e.getDamage() / 1.5;
                e.setDamage(original_damage);
            }
        }
    }
}
