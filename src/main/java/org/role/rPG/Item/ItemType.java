package org.role.rPG.Item;

public enum ItemType {
    EQUIPMENT, // 장비
    ARMOR,
    MELEE,
    RANGE,
    MAGIC,
    ACTIVE_ACCESSORY,  // 액티브 장신구
    PASSIVE_ACCESSORY, // 패시브 장신구
    MISC,      // 잡템 (Miscellaneous)
    CONSUMABLE,// 소비 아이템
    UNKNOWN    // 알 수 없는 타입 (기본값)
}