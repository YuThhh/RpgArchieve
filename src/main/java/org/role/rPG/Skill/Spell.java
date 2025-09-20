package org.role.rPG.Skill;

import org.bukkit.entity.Player;

public interface Spell {
    String getName(); // 마법의 이름 (예: "워터 볼트")
    double getCoolDown();
    void cast(Player caster); // 마법을 시전하는 로직
}