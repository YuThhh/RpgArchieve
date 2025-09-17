package org.role.rPG.Player;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Random;

// [수정] 자바 네이밍 컨벤션에 따라 클래스 이름은 대문자로 시작 (STAT -> Stat)
public class Stat implements Listener {

    // [수정] Random 객체는 한 번만 생성해서 재사용하는 것이 성능에 유리합니다.
    private static final Random RANDOM = new Random();

    // [수정] 코드의 가독성과 유지보수를 위해 '매직 넘버'들을 상수로 정의합니다.
    private static final double DEFENSE_CONSTANT = 500.0;
    private static final double STRENGTH_MUPLTPLIER = 0.003;


    private final JavaPlugin plugin;
    private final StatManager statManager;

    public Stat(JavaPlugin plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            double def = statManager.getFinalStat(p.getUniqueId(), "DEFENSE");

            if (def > 0.0) {
                double original_damage = e.getDamage();
                // [수정] 상수를 사용하여 가독성 향상
                double damageReduction = Math.pow(0.5, def / DEFENSE_CONSTANT);
                double final_damage = original_damage * damageReduction;

                e.setDamage(final_damage);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {

        Player attacker = null;
        double final_damage = event.getDamage();

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                attacker = (Player) shooter;
            }
        }

        if (attacker == null) {
            return;
        }

        // 힘 스탯 적용
        double str = statManager.getFinalStat(attacker.getUniqueId(), "STRENGTH");
        if (str > 0.0) {
                final_damage *= (1 + str * STRENGTH_MUPLTPLIER);
        }


        // 2. 커스텀 치명타 계산
        double critChance = statManager.getFinalStat(attacker.getUniqueId(),"CRIT_CHANCE");

        if (critChance > 0) {
            // [수정] 미리 생성해둔 RANDOM 객체를 사용합니다.
            // [수정] nextInt(100)은 0~99 사이의 정수를 반환하므로, 'critChance'가 20일 때 0~19가 나오면 성공하는 방식이 정확합니다.
            //        (random < critChance) 조건으로 20% 확률을 올바르게 구현할 수 있습니다.
            if (RANDOM.nextInt(100) < critChance) {
                double critDamage = statManager.getFinalStat(attacker.getUniqueId(),"CRIT_DAMAGE");
                // 치명타 발동!
                // [수정] 상수를 사용하여 가독성 향상
                final_damage *= (1.0 + critDamage / 100.0);

            }
        }

        event.setDamage(final_damage);

        if (event.getEntity() instanceof LivingEntity livingEntity) {
            double atkspd = statManager.getFinalStat(attacker.getUniqueId(),"ATTACK_SPEED");
                if (atkspd > 0.0) {
                    final int ticks = (int) (10 * 100 / (100 + atkspd));
                    // 1틱(0.05초) 뒤에 livingEntity의 무적 시간을 ticks로 설정하는 작업을 예약합니다.
                    livingEntity.setNoDamageTicks(ticks);
                }
        }
    }
}