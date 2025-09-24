package org.role.rPG.Enchant.enchants;

import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Enchant.EffectEnchantment;

public class BleedingEnchant extends EffectEnchantment {

    public BleedingEnchant(JavaPlugin plugin) {
        // ▼▼▼ 생성자 호출 방식 수정 ▼▼▼
        // 내부 ID: "bleeding_blade" (영어), 표시 이름: "흡혈의 칼날" (한글), 최대 레벨: 1, 효과 이름: "출혈"
        super(plugin, "bleeding", "출혈", 1, "출혈");
    }

    @Override
    public String getDescription(int level) {
        return "공격 시 대상에게 10초간 지속되는 출혈을 일으킵니다.";
    }
}