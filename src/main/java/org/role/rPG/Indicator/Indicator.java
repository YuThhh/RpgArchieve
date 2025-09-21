package org.role.rPG.Indicator;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource; // 투사체 발사자를 확인하기 위해 임포트
import org.jetbrains.annotations.Nullable;

public class Indicator implements Listener {

    private final IndicatorManager indicatorManager;

    public Indicator(IndicatorManager indicatorManager) {
        this.indicatorManager = indicatorManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 공격의 최종 출처가 될 플레이어를 담을 변수
        Player attacker = getPlayer(event);

        // 최종 공격자가 플레이어가 아니면 인디케이터를 표시하지 않습니다.
        if (attacker == null) {
            return;
        }

        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        // 피해를 입은 대상이 살아있는 개체가 아니거나 Display 개체일 경우 무시
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Display) {
            return;
        }

        Entity damagedEntity = event.getEntity();
        double damage = event.getFinalDamage();

        indicatorManager.showDamageIndicator(damagedEntity.getLocation(), damage);
    }

    private static @Nullable Player getPlayer(EntityDamageByEntityEvent event) {
        Player attacker = null;

        // ▼▼▼ 이 부분이 수정되었습니다 ▼▼▼

        // 경우 1: 공격자가 플레이어인 경우 (근접 공격)
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // 경우 2: 공격자가 투사체인 경우 (원거리 공격)
        else if (event.getDamager() instanceof Projectile projectile) {
            // 투사체 정보를 가져옵니다.
            // 투사체를 쏜 존재(Shooter)를 가져옵니다.
            ProjectileSource shooter = projectile.getShooter();

            // 쏜 존재가 플레이어인지 확인합니다.
            if (shooter instanceof Player) {
                attacker = (Player) shooter;
            }
        }
        return attacker;
    }
}