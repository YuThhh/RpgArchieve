package org.role.rPG;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class LEVEL_UI implements Listener, InventoryHolder {

    private final Inventory inv;

    public LEVEL_UI() {
        Component titleComponent = Component.text("프로필", NamedTextColor.BLUE); //GUI 이름
        inv = Bukkit.createInventory(this, 54, titleComponent); // GUI 칸 개수
        initializeItems();
    }

    private void initializeItems() {
        // inv.setItem(슬롯, 아이템);
        // 슬롯 번호는 0부터 시작
        ItemStack grayGlassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlassPane.getItemMeta();
        grayMeta.setHideTooltip(true);
        grayGlassPane.setItemMeta(grayMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, grayGlassPane);
        }

    }

    public void openInventory(final Player p) {
        p.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if(event.getInventory().getHolder() instanceof LEVEL_UI) {
            event.setCancelled(true);

            if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
                event.setCancelled(true);
            }
        }
    }
}
