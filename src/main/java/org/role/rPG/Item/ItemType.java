package org.role.rPG.Item;

public enum ItemType {
    EQUIPMENT, // 장비
    ARMOR,
    MELEE,
    RANGE,
    MAGIC,
    ACCESSORY, // **[추가]** 장신구 타입
    MISC,      // 잡템 (Miscellaneous)
    CONSUMABLE,// 소비 아이템
    UNKNOWN    // 알 수 없는 타입 (기본값)
}