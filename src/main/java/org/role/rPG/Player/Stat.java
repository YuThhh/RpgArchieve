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

        // 1. 공격이 플레이어가 쏜 '투사체'인 경우
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player attacker) {

            // 투사체 공격은 항상 무기의 'ATTACK_DAMAGE' 스탯을 기본 피해량으로 사용합니다.
            double weaponDamage = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_DAMAGE");
            double final_damage = (weaponDamage > 0) ? weaponDamage : event.getDamage();

            // --- 공통 스탯 적용 (힘, 치명타) ---
            final_damage = applyPlayerModifiers(attacker, final_damage);
            event.setDamage(final_damage);

            // --- 공통 공격 속도 적용 ---
            applyAttackSpeed(attacker, event.getEntity());

            // 2. 공격이 '플레이어의 근접 공격'인 경우
        } else if (event.getDamager() instanceof Player attacker) {

            double final_damage;
            org.bukkit.inventory.ItemStack weapon = attacker.getInventory().getItemInMainHand();
            org.bukkit.Material weaponType = weapon.getType();

            // 근접 공격 시, 손에 든 무기가 활/쇠뇌인지 확인합니다.
            double weaponDamage = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_DAMAGE");
            if (weaponType == org.bukkit.Material.BOW || weaponType == org.bukkit.Material.CROSSBOW) {

                // 활/쇠뇌로 직접 때린 경우, 기본 근접 피해량을 무기의 ATTACK_DAMAGE로 나눕니다.
                final_damage = event.getDamage(); // 매우 낮은 기본 피해량 (예: 1)

                if (weaponDamage > 0) {
                    final_damage = final_damage / weaponDamage; // (예: 1 / 50 = 0.02)
                }
                // 참고: 이 공격에는 힘, 치명타 등 추가 스탯이 적용되지 않아 피해가 거의 없습니다.

            } else {
                // 그 외 무기(검, 도끼 등)는 정상적으로 'ATTACK_DAMAGE' 스탯을 사용하고 보너스를 적용합니다.
                final_damage = (weaponDamage > 0) ? weaponDamage : event.getDamage();
                final_damage = applyPlayerModifiers(attacker, final_damage);
            }

            // --- 공통 스탯 적용 (힘, 치명타) ---
            final_damage = applyPlayerModifiers(attacker, final_damage);
            event.setDamage(final_damage);

            // --- 공통 공격 속도 적용 ---
            applyAttackSpeed(attacker, event.getEntity());
        }
    }

    /**
     * 힘, 치명타 등 플레이어의 스탯 보너스를 계산하여 최종 피해량을 반환합니다.
     * (코드 중복을 줄이기 위한 도우미 메서드)
     * @param attacker 공격자
     * @param initialDamage 스탯 적용 전 기본 피해량
     * @return 스탯 보너스가 적용된 최종 피해량
     */
    private double applyPlayerModifiers(Player attacker, double initialDamage) {
        double final_damage = initialDamage;

        // 힘 스탯 적용
        double str = statManager.getFinalStat(attacker.getUniqueId(), "STRENGTH");
        if (str > 0.0) {
            final_damage *= (1 + str * STRENGTH_MUPLTPLIER);
        }

        // 커스텀 치명타 계산
        double critChance = statManager.getFinalStat(attacker.getUniqueId(), "CRIT_CHANCE");
        if (RANDOM.nextInt(100) < critChance) {
            double critDamage = statManager.getFinalStat(attacker.getUniqueId(), "CRIT_DAMAGE");
            final_damage *= (1.0 + critDamage / 100.0);
        }

        return final_damage;
    }

    /**
     * 공격 속도 스탯에 따라 피격자의 무적 시간을 조절합니다.
     * (코드 중복을 줄이기 위한 도우미 메서드)
     * @param attacker 공격자
     * @param entity 피격자
     */
    private void applyAttackSpeed(Player attacker, org.bukkit.entity.Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            double atkspd = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_SPEED");
            if (atkspd > 0.0) {
                final int ticks = (int) (10 * 100 / (100 + atkspd));
                plugin.getServer().getScheduler().runTask(plugin, () -> livingEntity.setNoDamageTicks(ticks));
            }
        }
    }
}