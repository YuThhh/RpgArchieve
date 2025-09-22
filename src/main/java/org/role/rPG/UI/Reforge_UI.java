package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Player.Cash;
import org.role.rPG.Player.StatManager;
import org.role.rPG.Item.ReforgeManager;

public class Reforge_UI extends BaseUI {

    // 매니저 의존성은 그대로 유지합니다.
    private final ItemManager itemManager;
    private final StatManager statManager;
    private final ReforgeManager reforgeManager;

    private static final int ITEM_SLOT = 22;
    private static final int REFORGE_BUTTON_SLOT = 31;

    public Reforge_UI(ItemManager itemManager, StatManager statManager, ReforgeManager reforgeManager) {
        // 부모 생성자 호출
        super(54, Component.text("재련", NamedTextColor.BLUE));
        this.itemManager = itemManager;
        this.statManager = statManager;
        this.reforgeManager = reforgeManager;
    }

    @Override
    protected void initializeItems(Player player) {
        Graypanefiller.fillBackground(inv);

        ItemStack reforgeButton = new ItemStack(Material.ANVIL);
        ItemMeta reforgeMeta = reforgeButton.getItemMeta();
        reforgeMeta.displayName(Component.text("클릭하여 재련", NamedTextColor.YELLOW));
        reforgeButton.setItemMeta(reforgeMeta);

        inv.setItem(REFORGE_BUTTON_SLOT, reforgeButton);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // 아이템을 넣는 칸(ITEM_SLOT)은 자유롭게 클릭할 수 있도록 허용합니다.
        if (event.getSlot() == ITEM_SLOT) {
            return; // 이벤트를 취소하지 않고 종료
        }

        // 나머지 모든 칸은 기본적으로 클릭을 막습니다.
        event.setCancelled(true);

        // 재련 버튼을 클릭했을 때의 로직만 처리합니다.
        if (event.getSlot() == REFORGE_BUTTON_SLOT) {
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getInventory().getItem(ITEM_SLOT);

            // 기존 재련 로직과 동일
            if (item == null || itemManager.isNotCustomItem(item)) {
                player.sendMessage(Component.text("재련할 아이템을 지정된 칸에 올려주세요.", NamedTextColor.RED));
                return;
            }

            int cost = reforgeManager.getReforgeCost();
            if (Cash.getMoney(player) < cost) {
                player.sendMessage(Component.text("재련 비용이 부족합니다. (" + cost + "G)", NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            ReforgeManager.ReforgeModifier modifier = reforgeManager.getRandomModifier();
            if (modifier == null) {
                player.sendMessage(Component.text("재련 정보가 없습니다. 관리자에게 문의하세요.", NamedTextColor.RED));
                return;
            }

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
            Cash.removeMoney(player, cost);
            itemManager.reforgeItem(item, modifier);
            statManager.updatePlayerStats(player);

            player.sendMessage(Component.text()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(modifier.getName(), NamedTextColor.YELLOW))
                    .append(Component.text("] 접두사가 부여되었습니다!", NamedTextColor.GRAY)));
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        ItemStack item = event.getInventory().getItem(ITEM_SLOT);

        if (item != null && item.getType() != Material.AIR) {
            Player player = (Player) event.getPlayer();
            player.getInventory().addItem(item);
        }
    }
}