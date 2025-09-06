package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class GUI implements InventoryHolder {

    private final Inventory inv;

    public GUI() {
        // InventoryHolder 인터페이스의 메서드인 getInventory()만 @Override를 사용합니다.
        Component titleComponent = Component.text("RPG 메뉴", NamedTextColor.YELLOW);
        inv = Bukkit.createInventory(this, 54, titleComponent);
        initializeItems();
    }

    private void initializeItems() {
        // 아이템 추가 예시
        // inv.setItem(슬롯, 아이템);
        // 슬롯 번호는 0부터 시작
        inv.setItem(0, new ItemStack(org.bukkit.Material.IRON_SWORD));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public void openInventory(final Player p) {
        p.openInventory(inv);
    }
}
