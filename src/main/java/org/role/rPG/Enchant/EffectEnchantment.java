package org.role.rPG.Enchant;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * 특정 Effect를 발동시키는 역할을 하는 커스텀 인챈트의 추상 클래스입니다.
 */
public abstract class EffectEnchantment extends CustomEnchantment {

    private final String effectName;

    public EffectEnchantment(JavaPlugin plugin, String enchantName, int maxLevel, String effectName) {
        super(plugin, enchantName, maxLevel);
        this.effectName = effectName; // 이 인챈트가 발동시킬 효과의 이름
    }

    /**
     * 이 인챈트와 연결된 효과의 이름을 반환합니다.
     * @return 효과 이름 (예: "출혈")
     */
    public String getEffectName() {
        return effectName;
    }

    @Override
    public String getDescription(int level) {
        // 기본 설명을 제공합니다. 필요 시 각 인챈트 클래스에서 재정의할 수 있습니다.
        return "공격 시 " + effectName + " 효과를 부여합니다.";
    }
}