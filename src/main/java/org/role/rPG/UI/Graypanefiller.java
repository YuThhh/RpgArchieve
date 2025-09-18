package org.role.rPG.UI; // 본인 프로젝트 경로에 맞게 수정

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GUI와 관련된 편의 기능을 모아둔 정적 유틸리티 클래스입니다.
 */
public final class Graypanefiller { // final 키워드로 상속을 막아 유틸리티 클래스임을 명확히 합니다.

    // 1. 회색 유리판 아이템을 미리 생성해둡니다. (효율성)
    // static final로 선언하여 프로그램 시작 시 단 한 번만 생성되도록 합니다.
    private static final ItemStack GRAY_PANE;

    static {
        GRAY_PANE = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = GRAY_PANE.getItemMeta();
        meta.displayName(Component.text(" ")); // 이름은 공백으로 설정
        // meta.setHideTooltip(true); // 1.19.4+ 버전이라면 이 방법을 권장합니다.
        GRAY_PANE.setItemMeta(meta);
    }

    // 2. 생성자를 private으로 막아 객체 생성을 방지합니다.
    private Graypanefiller() {
        throw new UnsupportedOperationException("이 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 인벤토리의 비어있는 모든 칸을 회색 유리판으로 채웁니다.
     * @param inventory 배경을 채울 인벤토리
     */
    public static void fillBackground(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            // 3. 해당 슬롯이 비어있을 경우에만 아이템을 설정합니다. (중요)
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, GRAY_PANE);
            }
        }
    }
}