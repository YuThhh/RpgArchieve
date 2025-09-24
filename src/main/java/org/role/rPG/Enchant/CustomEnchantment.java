package org.role.rPG.Enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class CustomEnchantment {

    private final String name;
    private final int maxLevel;
    private final NamespacedKey key;

    public CustomEnchantment(JavaPlugin plugin, String name, int maxLevel) {
        this.name = name;
        this.maxLevel = maxLevel;
        this.key = new NamespacedKey(plugin, "enchant_" + name.toLowerCase().replace(" ", "_"));
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