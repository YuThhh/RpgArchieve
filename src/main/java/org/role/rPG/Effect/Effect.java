package org.role.rPG.Effect;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface Effect {
    String getName();
    double getDuration();
    void getEffect(LivingEntity entity, Player caster);
    NamespacedKey getMarkerKey();
}
