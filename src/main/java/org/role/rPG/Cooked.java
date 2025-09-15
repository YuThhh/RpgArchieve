package org.role.rPG;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Cooked implements Listener {

    private final JavaPlugin plugin;
    private final Component GUI_TITLE = Component.text("§6모닥불 요리");
    private final Component MEATBALL_NAME = Component.text("§e§l미트볼");
    private final Map<UUID, Long> eatCooldowns = new HashMap<>();
    private final Map<UUID, Integer> activeStrengthBuffs = new HashMap<>();

    public Cooked(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEatMeatball(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        UUID playerUUID = player.getUniqueId();
        if (itemInHand.getType() == Material.COCOA_BEANS && itemInHand.hasItemMeta() &&
                Objects.equals(itemInHand.getItemMeta().displayName(), MEATBALL_NAME)) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            if (eatCooldowns.getOrDefault(playerUUID, 0L) > now) return;
            eatCooldowns.put(playerUUID, now + 800);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
            itemInHand.setAmount(itemInHand.getAmount() - 1);
            PER_DATA data = PER_DATA.getInstance();
            if (activeStrengthBuffs.containsKey(playerUUID)) {
                int existingTaskId = activeStrengthBuffs.get(playerUUID);
                Bukkit.getScheduler().cancelTask(existingTaskId);
                player.sendMessage("§e미트볼 효과의 지속 시간이 1분으로 초기화되었습니다.");
            } else {
                double currentStrength = data.getPlayerStrength(playerUUID);
                data.setPlayerStrength(playerUUID, currentStrength + 1000.0);
                player.sendMessage("§c미트볼의 힘이 솟아나 1분간 힘이 강력해집니다! (힘 +1000)");
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    double strengthAfterBuff = data.getPlayerStrength(playerUUID);
                    data.setPlayerStrength(playerUUID, strengthAfterBuff - 1000.0);
                    player.sendMessage("§7미트볼의 효과가 사라졌습니다.");
                }
                activeStrengthBuffs.remove(playerUUID);
            }, 1200L);
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (!event.getView().title().equals(GUI_TITLE)) return;
        Player player = (Player) event.getPlayer();
        Inventory gui = event.getInventory();
        for (int i = 1; i <= 7; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
    }

    @EventHandler
    public void onCampfireInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null ||
                (event.getClickedBlock().getType() != Material.CAMPFIRE && event.getClickedBlock().getType() != Material.SOUL_CAMPFIRE)) {
            return;
        }
        event.setCancelled(true);
        openCampfireGui(event.getPlayer());
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(GUI_TITLE)) return;
        Player player = (Player) event.getWhoClicked();
        event.getClickedInventory();
        // ... (이하 onGuiClick 로직은 이전과 동일)
    }

    private void openCampfireGui(Player player) {
        // ... (이전과 동일)
    }

    private void handleCooking(Player player, Inventory gui) {
        int porkCount = 0;
        int stickCount = 0;
        int otherItems = 0;
        for (int i = 1; i <= 7; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null) {
                if (item.getType() == Material.PORKCHOP) porkCount += item.getAmount();
                else if (item.getType() == Material.STICK) stickCount += item.getAmount();
                else otherItems++;
            }
        }
        if (porkCount == 3 && stickCount == 2 && otherItems == 0) {
            gui.clear();
            player.closeInventory();
            player.getInventory().addItem(createMeatball());
            player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
            player.sendMessage("§a맛있는 미트볼이 완성되었습니다!");
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 0.5f);
            player.sendMessage("§c레시피가 올바르지 않습니다.");
        }
    }

    private ItemStack createMeatball() {
        ItemStack meatball = new ItemStack(Material.COCOA_BEANS);
        ItemMeta meta = meatball.getItemMeta();
        meta.displayName(MEATBALL_NAME);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7잘 다져진 고기를 뭉쳐 만들었다."));
        meta.lore(lore);
        meatball.setItemMeta(meta);
        return meatball;
    }
}