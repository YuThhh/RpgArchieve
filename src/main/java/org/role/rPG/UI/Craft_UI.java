package org.role.rPG.UI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Craft.CraftManager;
import org.role.rPG.Craft.Recipe;

import java.util.HashMap;
import java.util.Map;

public class Craft_UI extends BaseUI {

    private final JavaPlugin plugin;
    private final CraftManager craftManager;

    private static final int[] CRAFTING_SLOTS = { 11, 12, 13, 20, 21, 22, 29, 30, 31 };
    private static final int RESULT_SLOT = 24;

    public Craft_UI(JavaPlugin plugin, CraftManager craftManager) {
        super(54, Component.text("제작대", NamedTextColor.BLUE));
        this.plugin = plugin;
        this.craftManager = craftManager;
    }

    @Override
    protected void initializeItems(Player player) {
        Graypanefiller.fillBackground(inv);
        // 조합 그리드와 결과 슬롯은 비워둡니다.
        for (int slot : CRAFTING_SLOTS) {
            inv.setItem(slot, null);
        }
        inv.setItem(RESULT_SLOT, null);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int clickedSlot = event.getSlot();
        Player player = (Player) event.getWhoClicked();

        // 배경 유리판 클릭 시 이벤트 취소
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true);
            return;
        }

        // 결과 슬롯 클릭 시
        if (clickedSlot == RESULT_SLOT) {
            event.setCancelled(true);
            ItemStack resultItem = inv.getItem(RESULT_SLOT);

            // 결과물이 있는지, 플레이어 인벤토리에 공간이 있는지 확인
            if (resultItem != null && resultItem.getType() != Material.AIR) {
                if(player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§c인벤토리에 공간이 부족합니다.");
                    return;
                }
                consumeIngredients();
                player.getInventory().addItem(resultItem.clone());
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                updateResult();
            }
            return;
        }

        // 조합 그리드나 플레이어 인벤토리 클릭 시, 다음 틱에서 결과 업데이트
        plugin.getServer().getScheduler().runTaskLater(plugin, this::updateResult, 1L);
    }

    /** 조합 그리드의 재료를 레시피만큼 소모합니다. */
    private void consumeIngredients() {
        Recipe recipe = craftManager.getMatchingRecipe(getCurrentCraftingItems());
        if (recipe == null) return;

        for (Map.Entry<Integer, ItemStack> entry : recipe.getIngredients().entrySet()) {
            int slot = entry.getKey();
            int amountToConsume = entry.getValue().getAmount();
            ItemStack itemInSlot = inv.getItem(slot);
            if (itemInSlot != null) {
                itemInSlot.setAmount(itemInSlot.getAmount() - amountToConsume);
            }
        }
    }

    /** 현재 조합 그리드 상태에 따라 결과 슬롯을 업데이트합니다. */
    private void updateResult() {
        Recipe recipe = craftManager.getMatchingRecipe(getCurrentCraftingItems());
        if (recipe != null) {
            inv.setItem(RESULT_SLOT, recipe.getResult());
        } else {
            inv.setItem(RESULT_SLOT, null);
        }
    }

    /** 조합 그리드에 있는 아이템들을 맵 형태로 가져옵니다. (비어있지 않은 슬롯만) */
    private Map<Integer, ItemStack> getCurrentCraftingItems() {
        Map<Integer, ItemStack> currentItems = new HashMap<>();
        for (int slot : CRAFTING_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                currentItems.put(slot, item);
            }
        }
        return currentItems;
    }

    /** GUI를 닫을 때 조합 그리드에 남아있는 아이템을 플레이어에게 돌려줍니다. */
    public void handleClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        for (int slot : CRAFTING_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
    }
}