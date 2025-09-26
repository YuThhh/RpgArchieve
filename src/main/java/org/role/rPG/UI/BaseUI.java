package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * 모든 GUI 클래스의 기반이 되는 추상 클래스(설계도)입니다.
 */
public abstract class BaseUI implements InventoryHolder {

    protected final Inventory inv;

    public BaseUI(int size, Component title) {
        inv = Bukkit.createInventory(this, size, title);
    }

    // ▼▼▼ [필수 규칙 1] 이 클래스를 상속받는 모든 UI는 아이템 배치 방법을 반드시 구현해야 합니다. ▼▼▼
    protected abstract void initializeItems(Player player);

    // ▼▼▼ [필수 규칙 2] 이 클래스를 상속받는 모든 UI는 클릭 처리 방법을 반드시 구현해야 합니다. ▼▼▼
    public abstract void handleClick(InventoryClickEvent event);

    // 공통 기능: 모든 UI는 이 메서드를 통해 인벤토리를 열 수 있습니다.
    public void openInventory(Player player) {
        initializeItems(player);
        player.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}