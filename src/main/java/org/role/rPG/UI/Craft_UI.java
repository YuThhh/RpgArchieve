package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Item.ReforgeManager;
import org.role.rPG.Player.Cash;

public class Craft_UI extends BaseUI{

    private final JavaPlugin plugin;

    private static final int ITEM_SLOT_1 = 11;
    private static final int ITEM_SLOT_2 = 12;
    private static final int ITEM_SLOT_3 = 13;
    private static final int ITEM_SLOT_4 = 20;
    private static final int ITEM_SLOT_5 = 21;
    private static final int ITEM_SLOT_6 = 22;
    private static final int ITEM_SLOT_7 = 29;
    private static final int ITEM_SLOT_8 = 30;
    private static final int ITEM_SLOT_9 = 31;


    private static final int RESULT_SLOT = 24;

    public Craft_UI(JavaPlugin plugin) {
        super(54, Component.text("제작대", NamedTextColor.BLUE));
        this.plugin = plugin;
    }

    @Override
    protected void initializeItems(Player player) {
        // GUI 아이템 배치
        Graypanefiller.fillBackground(inv);

        ItemStack slot = new ItemStack(Material.AIR);

        inv.setItem(ITEM_SLOT_1, slot);
        inv.setItem(ITEM_SLOT_2, slot);
        inv.setItem(ITEM_SLOT_3, slot);
        inv.setItem(ITEM_SLOT_4, slot);
        inv.setItem(ITEM_SLOT_5, slot);
        inv.setItem(ITEM_SLOT_6, slot);
        inv.setItem(ITEM_SLOT_7, slot);
        inv.setItem(ITEM_SLOT_8, slot);
        inv.setItem(ITEM_SLOT_9, slot);

        inv.setItem(RESULT_SLOT, slot);
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
        if (clickedSlot == RESULT_SLOT) {
            // 버튼 자체를 움직일 수 없도록 이벤트를 취소합니다.


        }
        // 4. 위의 두 조건(유리판, 재련 버튼)에 해당하지 않는 모든 클릭은
        //    event.setCancelled(true)가 호출되지 않으므로 정상적으로 처리됩니다.
        //    (예: 아이템 슬롯, 비어있는 다른 슬롯 등)
    }
}
