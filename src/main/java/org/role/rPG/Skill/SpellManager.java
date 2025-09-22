package org.role.rPG.Skill;

import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Player.StatManager;

import java.util.HashMap;
import java.util.Map;

public class SpellManager {
    private final Map<String, Spell> spells = new HashMap<>();

    public SpellManager(JavaPlugin plugin, StatManager statManager) {
        // 여기에 새로운 마법들을 등록합니다.
        registerSpell(new WaterBoltSpell(plugin, statManager));
        registerSpell(new FireBallSpell(plugin,statManager));
        // registerSpell(new FireBallSpell(plugin, statManager)); // 나중에 파이어볼 추가시
    }

    private void registerSpell(Spell spell) {
        spells.put(spell.getName(), spell);
    }

    public Spell getSpell(String name) {
        return spells.get(name);
    }
}