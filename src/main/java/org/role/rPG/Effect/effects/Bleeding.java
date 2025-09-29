package org.role.rPG.Effect.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.role.rPG.Effect.Effect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Bleeding implements Effect {

    private final JavaPlugin plugin;

    // --- ▼▼▼ 설정값 ▼▼▼ ---
    private static final String EFFECT_NAME = "출혈";
    private static final double DURATION_SECONDS = 10.0;
    private static final int MAX_STACKS = 5; // 최대 중첩 수
    private static final double DAMAGE_PER_STACK = 0.5; // 1스택당 1초마다 입힐 데미지
    // --- ▲▲▲▲▲▲▲▲▲▲▲ ---

    public static final NamespacedKey BLEEDING_DAMAGE_KEY = new NamespacedKey("rpg", "bleeding_damage_marker");

    // <피격자 UUID, 현재 실행 중인 출혈 효과 Task>를 저장하는 추적용 Map
    private static final Map<UUID, BleedRunnable> activeBleeds = new HashMap<>();

    public Bleeding(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return EFFECT_NAME;
    }

    @Override
    public double getDuration() {
        return DURATION_SECONDS;
    }

    @Override
    public NamespacedKey getMarkerKey() {
        return BLEEDING_DAMAGE_KEY;
    }

    @Override
    public void getEffect(LivingEntity target, Player caster) {
        UUID targetUUID = target.getUniqueId();

        // 1. 대상이 이미 출혈에 걸려있는지 확인
        if (activeBleeds.containsKey(targetUUID)) {
            // 이미 걸려있다면, 기존 효과의 중첩을 올리고 시간을 초기화
            BleedRunnable existingBleed = activeBleeds.get(targetUUID);
            existingBleed.addStackAndRefreshDuration();
        } else {
            // 2. 대상이 출혈 상태가 아니라면, 새로운 출혈 효과를 생성하고 시작
            BleedRunnable newBleed = new BleedRunnable(target, caster);
            BukkitTask task = newBleed.runTaskTimer(plugin, 0L, 1L);
            activeBleeds.put(targetUUID, newBleed);

            // 최초 메시지
            if (target instanceof Player) {
                target.sendMessage(Component.text(caster.getName()).append(Component.text("에게서 " + getName() + " 효과를 받았습니다!", NamedTextColor.RED)));
            }
        }
    }

    // 출혈 효과의 실제 로직을 담고 있는 내부 클래스
    private class BleedRunnable extends BukkitRunnable {
        private final LivingEntity target;
        private final Player caster;
        private int stacks;
        private int ticksLived;

        public BleedRunnable(LivingEntity target, Player caster) {
            this.target = target;
            this.caster = caster;
            this.stacks = 1; // 처음 생성 시 1스택
            this.ticksLived = 0;
        }

        public void addStackAndRefreshDuration() {
            // 최대 중첩 수보다 작을 때만 스택 증가
            if (this.stacks < MAX_STACKS) {
                this.stacks++;
            }
            // 지속시간 초기화
            this.ticksLived = 0;

            // 중첩 정보 메시지
            caster.sendMessage(Component.text(target.getName() + "의 " + getName() + " 중첩이 증가했습니다. (" + this.stacks + "/" + MAX_STACKS + ")", NamedTextColor.YELLOW));
        }

        @Override
        public void run() {
            final int durationInTicks = (int) (DURATION_SECONDS * 20);

            // 1. 효과 종료 조건 확인
            if (ticksLived >= durationInTicks || target.isDead() || !target.isValid()) {
                // 종료 시 추적 Map에서 제거 (매우 중요!)
                activeBleeds.remove(target.getUniqueId());
                if (target instanceof Player && target.isValid()) {
                    target.sendMessage(Component.text(getName() + " 효과가 종료되었습니다.", NamedTextColor.GRAY));
                }
                this.cancel();
                return;
            }

            // 2. 1초(20틱)마다 데미지 적용
            if (ticksLived % 20 == 0) {
                // 데미지 = 스택당 데미지 * 현재 중첩 수
                double totalDamage = DAMAGE_PER_STACK * this.stacks;

                try {
                    target.setMetadata(getMarkerKey().getKey(), new FixedMetadataValue(plugin, true));
                    target.damage(totalDamage, caster);
                    target.setNoDamageTicks(0);
                } finally {
                    target.removeMetadata(getMarkerKey().getKey(), plugin);
                }
            }
            ticksLived++;
        }
    }
}

//test