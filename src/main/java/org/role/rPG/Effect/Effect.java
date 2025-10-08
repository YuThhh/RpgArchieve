package org.role.rPG.Effect;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface Effect {
    String getName(); // 효과 이름
    double getDuration(); // 효과 지속시간
    void getEffect(LivingEntity entity, Player caster); // 효과 적용
    void removeEffect(LivingEntity entity);
    NamespacedKey getMarkerKey(); // 효과 표식
}
