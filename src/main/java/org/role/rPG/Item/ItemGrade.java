package org.role.rPG.Item;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * 아이템의 등급을 나타내는 Enum입니다.
 * 각 등급은 고유한 이름 색상과 표시 이름을 가집니다.
 */
public enum ItemGrade {
    COMMON(NamedTextColor.GRAY, "흔함"),      // 흔함 (회색)
    RARE(NamedTextColor.AQUA, "희귀"),        // 희귀 (하늘색)
    EPIC(NamedTextColor.LIGHT_PURPLE, "에픽"),// 에픽 (보라색)
    LEGENDARY(NamedTextColor.GOLD, "전설");   // 전설 (금색)

    private final TextColor color;
    private final String displayName; // 표시 이름을 저장할 필드

    ItemGrade(TextColor color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public TextColor getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }
}