package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Graypanefiller {

    private Graypanefiller() {
        throw new UnsupportedOperationException("이 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * GUI 배경을 채울 새 회색 유리판 아이템을 생성합니다.
     * @return 이름과 툴팁이 없는 새 회색 유리판 ItemStack
     */
    private static ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();

        // 1. 이름 서식을 명확하게 지정합니다.
        meta.displayName(Component.text("Empty").decoration(TextDecoration.ITALIC, false));

        meta.setHideTooltip(true);
        pane.setItemMeta(meta);
        return pane;
    }

    /**
     * 인벤토리의 비어있는 모든 칸을 회색 유리판으로 채웁니다.
     * @param inventory 배경을 채울 인벤토리
     */
    public static void fillBackground(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGrayPane());
            }
        }
    }
}