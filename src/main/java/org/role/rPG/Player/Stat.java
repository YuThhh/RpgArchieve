package org.role.rPG.Player;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Indicator.IndicatorManager;

import java.util.Random;

// [수정] 자바 네이밍 컨벤션에 따라 클래스 이름은 대문자로 시작 (STAT -> Stat)
public class Stat implements Listener {

    // [수정] Random 객체는 한 번만 생성해서 재사용하는 것이 성능에 유리합니다.
    private static final Random RANDOM = new Random();

    // [수정] 코드의 가독성과 유지보수를 위해 '매직 넘버'들을 상수로 정의합니다.
    private static final double DEFENSE_CONSTANT = 500.0;
    private static final double STRENGTH_MUPLTPLIER = 0.003;
    private static final int MINIMUM_INVINCIBILITY_TICKS = 5;

    private final JavaPlugin plugin;
    private final StatManager statManager;
    private final IndicatorManager indicatorManager; // 인디케이터를 직접 제어하기 위해 추가

    public Stat(JavaPlugin plugin, StatManager statManager, IndicatorManager indicatorManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.indicatorManager = indicatorManager;
    }

    // 대미지 계산 결과와 치명타 여부를 함께 반환하기 위한 내부 클래스
    private static class DamageResult {
        final double damage;
        final boolean isCritical;

        DamageResult(double damage, boolean isCritical) {
            this.damage = damage;
            this.isCritical = isCritical;
        }
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
        // ▼▼▼ [핵심 수정] 메서드 최상단에 아래 내용을 추가하세요 ▼▼▼
        // 피격자가 살아있는 생명체이고, 현재 무적 시간이 남아있다면,
        if (event.getEntity() instanceof LivingEntity livingEntity && livingEntity.getNoDamageTicks() > 0) {
            // 이 공격 이벤트를 취소하고 즉시 모든 처리를 중단합니다.
            event.setCancelled(true);
            return;
        }
        // ▲▲▲ 여기까지가 추가된 내용입니다 ▲▲▲


        // --- 아래는 기존 코드 ---
        Player attacker = null;
        double baseDamage = event.getDamage();

        // 1. 공격이 플레이어가 쏜 '투사체'인 경우
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p; // 공격자 설정
            double weaponDamage = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_DAMAGE");
            baseDamage = (weaponDamage > 0) ? weaponDamage : event.getDamage();

            // 2. 공격이 '플레이어의 근접 공격'인 경우
        } else if (event.getDamager() instanceof Player p) {
            attacker = p; // 공격자 설정
            org.bukkit.inventory.ItemStack weapon = attacker.getInventory().getItemInMainHand();
            org.bukkit.Material weaponType = weapon.getType();
            double weaponDamage = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_DAMAGE");

            if (weaponType == org.bukkit.Material.BOW || weaponType == org.bukkit.Material.CROSSBOW) {
                if (weaponDamage > 0) {
                    baseDamage = baseDamage / weaponDamage;
                }
            } else {
                baseDamage = (weaponDamage > 0) ? weaponDamage : event.getDamage();
            }
        }

        // --- 최종 처리: 공격자가 플레이어일 경우에만 실행 ---
        if (attacker != null) {
            // 대미지 계산과 함께 치명타 여부를 확인
            DamageResult result = applyPlayerModifiers(attacker, baseDamage);

            event.setDamage(result.damage);
            applyAttackSpeed(attacker, event.getEntity());

            // 치명타 여부에 따라 다른 인디케이터를 표시
            Location loc = event.getEntity().getLocation();
            if (result.isCritical) {
                indicatorManager.showCriticalDamageIndicator(loc, result.damage);
            } else {
                indicatorManager.showDamageIndicator(loc, result.damage);
            }
        }
    }

    /**
     * 힘, 치명타 등 플레이어의 스탯 보너스를 계산하여 최종 피해량과 치명타 여부를 반환합니다.
     * (코드 중복을 줄이기 위한 도우미 메서드)
     * @param attacker 공격자
     * @param initialDamage 스탯 적용 전 기본 피해량
     * @return 스탯 보너스가 적용된 최종 피해량과 치명타 여부를 담은 DamageResult 객체
     */
    private DamageResult applyPlayerModifiers(Player attacker, double initialDamage) {
        double final_damage = initialDamage;
        boolean isCritical = false;

        // 힘 스탯 적용
        double str = statManager.getFinalStat(attacker.getUniqueId(), "STRENGTH");
        if (str > 0.0) {
            final_damage *= (1 + str * STRENGTH_MUPLTPLIER);
        }

        // 커스텀 치명타 계산
        double critChance = statManager.getFinalStat(attacker.getUniqueId(), "CRIT_CHANCE");
        if (RANDOM.nextInt(100) < critChance) {
            isCritical = true;
            double critDamage = statManager.getFinalStat(attacker.getUniqueId(), "CRIT_DAMAGE");
            final_damage *= (1.0 + critDamage / 100.0);
        }

        return new DamageResult(final_damage, isCritical);
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

                // 초반 스탯 효율이 개선된 계산식
                int calculated_ticks = (int) (10 * 100 / (100 + atkspd));

                // 계산된 틱이 설정한 최소치보다 낮아지지 않도록 보정
                int final_ticks = Math.max(MINIMUM_INVINCIBILITY_TICKS, calculated_ticks);

                // 다음 틱에 무적 시간을 최종 적용
                plugin.getServer().getScheduler().runTask(plugin, () -> livingEntity.setNoDamageTicks(final_ticks));

        }
    }
}