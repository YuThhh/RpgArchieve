package org.role.rPG.Effect.effects;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.role.rPG.Effect.Effect;

public class FireResistance implements Effect {

    private static final String EFFECT_NAME = "화염저항";
    // 네임스페이스 키는 플러그인 내에서 고유해야 합니다.
    private static final NamespacedKey MARKER_KEY = new NamespacedKey("rpg", "fire_resistance_marker");

    @Override
    public String getName() {
        return EFFECT_NAME;
    }

    @Override
    public double getDuration() {
        // 지속 효과이므로, 아이템을 착용하고 있는 동안 계속 적용됩니다.
        // 특정 시간 후 사라지는 것이 아니므로 이 값은 큰 의미가 없습니다.
        return Double.POSITIVE_INFINITY;
    }

    /**
     * 대상에게 마인크래프트의 기본 화염 저항 포션 효과를 부여합니다.
     */
    @Override
    public void getEffect(LivingEntity entity, Player caster) {
        // Integer.MAX_VALUE는 사실상 무한 지속을 의미합니다.
        // 증폭(amplifier)은 0부터 시작하므로, 0은 화염 저항 I을 의미합니다.
        PotionEffect fireResistanceEffect = new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE,
                Integer.MAX_VALUE,
                0, // 증폭 레벨 (0 = I)
                true, // 주변 파티클 숨김
                false // 아이콘 표시
        );
        entity.addPotionEffect(fireResistanceEffect);
    }

    /**
     * 대상에게서 화염 저항 효과를 제거합니다.
     */
    @Override
    public void removeEffect(LivingEntity entity) {
        entity.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    @Override
    public NamespacedKey getMarkerKey() {
        return MARKER_KEY;
    }
}