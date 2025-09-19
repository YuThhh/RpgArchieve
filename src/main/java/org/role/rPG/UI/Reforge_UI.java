package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Player.Cash;
import org.role.rPG.Player.StatManager;
import org.role.rPG.Item.ReforgeManager;

public class Reforge_UI implements InventoryHolder, Listener {

    private final Inventory inv;
    // 리포지에 필요한 매니저들을 저장할 필드 추가
    private final ItemManager itemManager;
    private final StatManager statManager;
    private final ReforgeManager reforgeManager;

    // 아이템과 버튼의 위치를 상수로 정의하여 관리 용이
    private static final int ITEM_SLOT = 22;
    private static final int REFORGE_BUTTON_SLOT = 31;

    // 생성자에서 매니저들을 받아옵니다.
    public Reforge_UI(ItemManager itemManager, StatManager statManager, ReforgeManager reforgeManager) {
        this.itemManager = itemManager;
        this.statManager = statManager;
        this.reforgeManager = reforgeManager;

        Component titleComponent = Component.text("재련", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false);
        inv = Bukkit.createInventory(this, 54, titleComponent);
        initializeItems();
    }

    private void initializeItems() {
        Graypanefiller.fillBackground(inv);

        ItemStack reforgeButton = new ItemStack(Material.ANVIL);
        ItemMeta reforgeMeta = reforgeButton.getItemMeta();
        reforgeMeta.displayName(Component.text("클릭하여 재련", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        reforgeButton.setItemMeta(reforgeMeta);

        ItemStack blank = new ItemStack(Material.AIR);

        inv.setItem(REFORGE_BUTTON_SLOT, reforgeButton);
        inv.setItem(ITEM_SLOT, blank);
    }

    // GUI 클릭 이벤트를 처리하는 핵심 로직
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 이 UI가 아닌 다른 인벤토리라면 무시
        if (!(event.getInventory().getHolder() instanceof Reforge_UI)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // 플레이어 자신의 인벤토리를 클릭한 경우 허용
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        // 아이템을 넣는 칸(22번 슬롯)을 클릭한 경우 허용
        if (event.getSlot() == ITEM_SLOT) {
            return;
        }

        // 그 외 모든 UI 클릭은 취소시켜 아이템을 가져가지 못하게 함
        event.setCancelled(true);

        // 모루(재련 버튼)를 클릭했을 때의 로직
        if (event.getSlot() == REFORGE_BUTTON_SLOT) {
            ItemStack item = event.getInventory().getItem(ITEM_SLOT);

            // 1. 아이템이 유효한지 검사
            if (item == null || itemManager.isNotCustomItem(item)) {
                player.sendMessage(Component.text("재련할 아이템을 지정된 칸에 올려주세요.", NamedTextColor.RED));
                return;
            }

            // 2. 비용이 충분한지 검사
            int cost = reforgeManager.getReforgeCost();
            if (Cash.getMoney(player) < cost) {
                player.sendMessage(Component.text("재련 비용이 부족합니다. (" + cost + "G)", NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            // 3. 재련 Modifier 가져오기
            ReforgeManager.ReforgeModifier modifier = reforgeManager.getRandomModifier();
            if (modifier == null) {
                player.sendMessage(Component.text("재련 정보가 없습니다. 관리자에게 문의하세요.", NamedTextColor.RED));
                return;
            }

            // 4. 재련 실행
            Cash.removeMoney(player, cost);
            itemManager.reforgeItem(item, modifier);
            statManager.updatePlayerStats(player); // 변경된 스탯 적용

            player.sendMessage(Component.text()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(modifier.getName(), NamedTextColor.YELLOW))
                    .append(Component.text("] 접두사가 부여되었습니다!", NamedTextColor.GRAY)));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 1. 닫힌 인벤토리가 리포지 UI가 맞는지 확인합니다.
        if (!(event.getInventory().getHolder() instanceof Reforge_UI)) {
            return;
        }

        // 2. 아이템 슬롯(22번)에서 아이템을 가져옵니다.
        ItemStack item = event.getInventory().getItem(ITEM_SLOT);

        // 3. 슬롯에 아이템이 존재하는지 확인합니다.
        if (item != null && item.getType() != Material.AIR) {
            Player player = (Player) event.getPlayer();

            // 4. 플레이어의 인벤토리로 아이템을 돌려줍니다.
            //    만약 플레이어 인벤토리가 꽉 찼다면, 아이템은 자동으로 플레이어 발밑에 드롭됩니다.
            player.getInventory().addItem(item);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public void openInventory(final Player p) {
        p.openInventory(inv);
    }
}