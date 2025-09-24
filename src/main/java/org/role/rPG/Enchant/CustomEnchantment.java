package org.role.rPG.Enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class CustomEnchantment {

    private final String id;
    private final String name;
    private final int maxLevel;
    private final NamespacedKey key;

    public CustomEnchantment(JavaPlugin plugin, String id, String name, int maxLevel) {
        this.id = id;
        this.name = name;
        this.maxLevel = maxLevel;
        // NamespacedKey는 이제 한글이 없는 id를 사용합니다.
        this.key = new NamespacedKey(plugin, "enchant_" + id);
    }

    public String getId() { // ◀◀◀ 새로 추가
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public NamespacedKey getKey() {
        return key;
    }

    // 인챈트 설명을 위한 추상 메서드 (레벨별로 다른 설명을 가질 수 있도록)
    public abstract String getDescription(int level);
}