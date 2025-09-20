package org.role.rPG.Magic;

import org.bukkit.entity.Player;

public interface Spell {
    String getName(); // 마법의 이름 (예: "워터 볼트")
    void cast(Player caster); // 마법을 시전하는 로직
}