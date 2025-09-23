package org.role.rPG.Effect;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EffectManager {

    private final JavaPlugin plugin;
    private final Map<String, Effect> effects = new HashMap<>();

    public EffectManager(JavaPlugin plugin) {
        this.plugin = plugin;
        registerEffects();
    }

    /**
     * 관리할 모든 효과들을 등록합니다.
     */
    private void registerEffects() {
        registerEffect(new Bleeding(plugin));
        // 여기에 다른 효과들(중독, 기절 등)을 추가할 수 있습니다.
    }

    private void registerEffect(Effect effect) {
        effects.put(effect.getName(), effect);
    }

    /**
     * 이름으로 등록된 효과를 가져옵니다.
     * @param name 효과 이름
     * @return Effect 객체, 없으면 null
     */
    public Effect getEffect(String name) {
        return effects.get(name);
    }

    /**
     * [추가] 등록된 모든 효과의 목록을 반환합니다.
     * @return 모든 Effect 객체를 담은 Collection
     */
    public Collection<Effect> getAllEffects() {
        return effects.values();
    }
}