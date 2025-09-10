package org.role.rPG;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

// [수정] 자바 네이밍 컨벤션에 따라 클래스 이름은 대문자로 시작 (STAT -> Stat)
public class Stat implements Listener {

    // [수정] Random 객체는 한 번만 생성해서 재사용하는 것이 성능에 유리합니다.
    private static final Random RANDOM = new Random();

    // [수정] 코드의 가독성과 유지보수를 위해 '매직 넘버'들을 상수로 정의합니다.
    private static final double CUSTOM_MAX_HEALTH = 100.0;
    private static final double VANILLA_HEALTH_SCALE = 20.0;
    private static final double VANILLA_CRIT_MULTIPLIER = 1.5;
    private static final double DEFENSE_CONSTANT = 500.0;
    private static final double PERCENTAGE_CONSTANT = 100.0;

    private final PER_DATA data = PER_DATA.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(CUSTOM_MAX_HEALTH);
        player.setHealth(CUSTOM_MAX_HEALTH);

        player.setHealthScale(VANILLA_HEALTH_SCALE);
        player.setHealthScaled(true);

        // 참고: 현재 로직은 플레이어의 방어력 데이터가 없을 때 기본값 0을 반환한다고 가정합니다.
        // PER_DATA 클래스의 구현에 따라 이 부분은 달라질 수 있습니다.
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
                // [수정] 상수를 사용하여 가독성 향상
                double damageReduction = Math.pow(0.5, def / DEFENSE_CONSTANT);
                double final_damage = original_damage * damageReduction;

                e.setDamage(final_damage);
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        double damage = event.getDamage();

        // 크리티컬 조건
        if (attacker.getAttackCooldown() >= 0.849 &&
                !attacker.isOnGround() &&
                !attacker.isInWater() &&
                !attacker.isInsideVehicle() &&
                !attacker.hasPotionEffect(PotionEffectType.BLINDNESS)) {

            // 크리티컬 무효화 → 일반 공격으로 바꾸기
            double baseDamage = event.getDamage() / 1.5; // 1.5배 증가 제거
            event.setDamage(baseDamage);

        }

        // 2. 커스텀 치명타 계산
        double critChance = data.getPlayerCrit(attacker.getUniqueId());

        if (critChance > 0) {
            // [수정] 미리 생성해둔 RANDOM 객체를 사용합니다.
            // [수정] nextInt(100)은 0~99 사이의 정수를 반환하므로, 'critChance'가 20일 때 0~19가 나오면 성공하는 방식이 정확합니다.
            //        (random < critChance) 조건으로 20% 확률을 올바르게 구현할 수 있습니다.
            if (RANDOM.nextInt(100) < critChance) {
                double critDamage = data.getPlayerCritDamage(attacker.getUniqueId());
                // 치명타 발동!
                // [수정] 상수를 사용하여 가독성 향상
                damage *= (1.0 + critDamage / PERCENTAGE_CONSTANT);

            }
        }

        event.setDamage(damage);
    }
}