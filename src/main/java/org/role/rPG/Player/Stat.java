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
import org.bukkit.scheduler.BukkitRunnable;
import org.role.rPG.Effect.Effect;
import org.role.rPG.Effect.EffectManager;
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

    public static final String MAGIC_DAMAGE_KEY = "CUSTOM_MAGIC_DAMAGE";

    private final JavaPlugin plugin;
    private final StatManager statManager;
    private final IndicatorManager indicatorManager; // 인디케이터를 직접 제어하기 위해 추가
    private final EffectManager effectManager;

    public Stat(JavaPlugin plugin, StatManager statManager, IndicatorManager indicatorManager, EffectManager effectManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.indicatorManager = indicatorManager;
        this.effectManager = effectManager; // [추가]
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
        if (e.getEntity() instanceof LivingEntity entity) {

            // 서버의 기본 데미지 처리 로직이 끝난 후 (1틱 뒤) 무적 시간을 0으로 설정합니다.
            // 이렇게 해야 서버가 설정하는 기본 무적 시간을 덮어쓸 수 있습니다.
            new BukkitRunnable() {
                @Override
                public void run() {
                    entity.setNoDamageTicks(0);
                }
            }.runTaskLater(plugin, 1L); // 1L = 1틱 후 실행

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
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 1. 공격자가 플레이어인지 확인합니다. 아니면 메서드를 종료합니다.
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) return;

        // --- 변수 초기화 ---
        double finalDamage;
        boolean isCritical = false;
        boolean isMagic = event.getEntity().hasMetadata(MAGIC_DAMAGE_KEY);

        // 2. 데미지 종류(마법/물리)에 따라 계산 방식을 나눕니다.
        if (isMagic) {
            // --- 마법 데미지 계산 경로 ---
            finalDamage = event.getDamage(); // 스킬에서 설정한 기본 데미지를 가져옵니다.

            // 치명타 스탯을 가져와 적용합니다.
            double critChance = statManager.getFinalStat(attacker.getUniqueId(), "CRIT_CHANCE");
            if (RANDOM.nextInt(100) < critChance) {
                isCritical = true;
                double critDamage = statManager.getFinalStat(attacker.getUniqueId(), "CRIT_DAMAGE");
                finalDamage *= (1.0 + critDamage / 100.0);
            }
            // (마법 데미지는 힘 스탯의 영향을 받지 않습니다)

        } else {
            // --- 물리 / 기타 효과 데미지 계산 경로 ---

            // '출혈' 같은 효과 데미지인지 확인합니다.
            boolean isEffectDamage = false;
            for (Effect effect : effectManager.getAllEffects()) {
                if (event.getEntity().hasMetadata(effect.getMarkerKey().getKey())) {
                    isEffectDamage = true;
                    break;
                }
            }

            // 순수 물리 공격에만 공격 쿨다운과 바닐라 무적 시간을 적용합니다.
            if (!isEffectDamage) {
                String cooldownKey = "ATTACK_COOLDOWN_" + attacker.getUniqueId();
                if (event.getEntity().hasMetadata(cooldownKey)) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getEntity() instanceof LivingEntity livingEntity && livingEntity.getNoDamageTicks() > 0) {
                    event.setCancelled(true);
                    return;
                }
            }

            // 기본 공격력을 계산합니다.
            double baseDamage;
            if (event.getDamager() instanceof Projectile) {
                double weaponDamage = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_DAMAGE");
                baseDamage = (weaponDamage > 0) ? weaponDamage : event.getDamage();
            } else { // 근접 공격
                org.bukkit.inventory.ItemStack weapon = attacker.getInventory().getItemInMainHand();
                org.bukkit.Material weaponType = weapon.getType();
                double weaponDamage = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_DAMAGE");

                if (weaponType == org.bukkit.Material.BOW || weaponType == org.bukkit.Material.CROSSBOW) {
                    baseDamage = (weaponDamage > 0) ? (event.getDamage() / weaponDamage) : event.getDamage();
                } else {
                    baseDamage = (weaponDamage > 0) ? weaponDamage : event.getDamage();
                }
            }

            if (!isEffectDamage) {
                // 순수 물리 공격에만 힘과 치명타를 적용합니다.
                DamageResult result = applyPlayerModifiers(attacker, baseDamage);
                finalDamage = result.damage;
                isCritical = result.isCritical;
                applyAttackCoolDown(attacker, event.getEntity());
            } else {
                // 효과 데미지는 계산 없이 그대로 적용합니다.
                finalDamage = event.getFinalDamage();
            }
        }

        // 3. 최종 계산된 데미지를 이벤트에 적용합니다.
        event.setDamage(finalDamage);

        // 4. 데미지 인디케이터를 표시합니다. (치명타 여부에 따라 색상 변경)
        Location loc = event.getEntity().getLocation();
        if (isCritical) {
            indicatorManager.showCriticalDamageIndicator(loc, finalDamage);
        } else {
            indicatorManager.showDamageIndicator(loc, finalDamage);
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
     * @param victim 피격자
     */
    private void applyAttackCoolDown(Player attacker, org.bukkit.entity.Entity victim) {
        if (victim instanceof LivingEntity livingVictim) {
            double atkspd = statManager.getFinalStat(attacker.getUniqueId(), "ATTACK_SPEED");
            livingVictim.setNoDamageTicks(0);

            atkspd = Math.max(0.0, atkspd);

            // 공격 속도 스탯을 기반으로 쿨다운 시간(틱)을 계산합니다.
            // 이 계산식은 기존의 것을 그대로 활용하거나 원하는 방식으로 수정할 수 있습니다.
            int cooldownTicks = (int) (10 * 100 / (100 + atkspd));
            int finalCooldownTicks = Math.max(MINIMUM_INVINCIBILITY_TICKS, cooldownTicks);

            // 1. 공격자에게 고유한 메타데이터 키를 생성합니다.
            String cooldownKey = "ATTACK_COOLDOWN_" + attacker.getUniqueId();

            // 2. 피격자에게 메타데이터 '표식'을 부여합니다.
            // FixedMetadataValue는 플러그인이 비활성화되면 자동으로 사라집니다.
            victim.setMetadata(cooldownKey, new org.bukkit.metadata.FixedMetadataValue(plugin, true));

            // 3. 계산된 시간 후에 메타데이터를 제거하도록 스케줄러 작업을 예약합니다.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> victim.removeMetadata(cooldownKey, plugin), finalCooldownTicks);

            // 4. 바닐라 무적 시간을 즉시 제거하여 다른 플레이어가 바로 공격할 수 있게 합니다.
            // 다음 틱에 실행하여 현재 데미지가 정상적으로 들어간 후에 무적 시간을 초기화합니다.
            plugin.getServer().getScheduler().runTask(plugin, () -> livingVictim.setNoDamageTicks(0));
        }
    }
}