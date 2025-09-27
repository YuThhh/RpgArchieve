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
import org.role.rPG.Item.ItemType;
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
        // GUI 아이템 배치
        Graypanefiller.fillBackground(inv);

        ItemStack reforgeButton = new ItemStack(Material.ANVIL);
        ItemMeta reforgeMeta = reforgeButton.getItemMeta();
        reforgeMeta.displayName(Component.text("클릭하여 재련", NamedTextColor.YELLOW));
        reforgeButton.setItemMeta(reforgeMeta);

        ItemStack slot = new ItemStack(Material.AIR);

        inv.setItem(ITEM_SLOT, slot);
        inv.setItem(REFORGE_BUTTON_SLOT, reforgeButton);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // 1. 클릭된 슬롯 번호와 아이템을 가져옵니다.
        int clickedSlot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        // 2. 만약 회색 유리판을 클릭했다면, 이벤트만 취소하고 아무것도 하지 않습니다.
        if (clickedItem != null && clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true);
            return;
        }

        // 3. 만약 재련 버튼을 클릭했다면, 재련 로직을 실행합니다.
        if (clickedSlot == REFORGE_BUTTON_SLOT) {
            // 버튼 자체를 움직일 수 없도록 이벤트를 취소합니다.
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            ItemStack itemToReforge = event.getInventory().getItem(ITEM_SLOT); // 재련될 아이템 가져오기

            // 재련할 아이템이 있는지 확인
            if (itemToReforge == null || itemManager.getItemType(itemToReforge) != ItemType.EQUIPMENT) {
                player.sendMessage(Component.text("장비 아이템만 재련할 수 있습니다.", NamedTextColor.RED));
                return;
            }

            // 비용 확인
            int cost = reforgeManager.getReforgeCost();
            if (Cash.getMoney(player) < cost) {
                player.sendMessage(Component.text("재련 비용이 부족합니다. (" + cost + "G)", NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            // 재련 실행
            ReforgeManager.ReforgeModifier modifier = reforgeManager.getRandomModifier();
            if (modifier == null) {
                player.sendMessage(Component.text("재련 정보가 없습니다. 관리자에게 문의하세요.", NamedTextColor.RED));
                return;
            }

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.0F);
            Cash.removeMoney(player, cost);
            itemManager.reforgeItem(itemToReforge, modifier);
            statManager.updatePlayerStats(player);

            player.sendMessage(Component.text()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(modifier.getName(), NamedTextColor.YELLOW))
                    .append(Component.text("] 접두사가 부여되었습니다!", NamedTextColor.GRAY)));
        }

        // 4. 위의 두 조건(유리판, 재련 버튼)에 해당하지 않는 모든 클릭은
        //    event.setCancelled(true)가 호출되지 않으므로 정상적으로 처리됩니다.
        //    (예: 아이템 슬롯, 비어있는 다른 슬롯 등)
    }

    public void handleClose(InventoryCloseEvent event) {
        ItemStack item = event.getInventory().getItem(ITEM_SLOT);

        if (item != null && item.getType() != Material.AIR) {
            Player player = (Player) event.getPlayer();
            player.getInventory().addItem(item);
        }
    }
}