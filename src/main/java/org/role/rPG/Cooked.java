package org.role.rPG; // 본인의 패키지 경로에 맞게 설정해주세요.

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cooked implements Listener {

    private final JavaPlugin plugin;
    private final String GUI_TITLE = "§6모닥불 요리";
    private final String MEATBALL_NAME = "§e§l미트볼";
    private final Map<UUID, Long> eatCooldowns = new HashMap<>();
    private final Map<UUID, Integer> activeStrengthBuffs = new HashMap<>();

    public Cooked(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * [기능 1] '미트볼' 섭취 기능 (힘 차감 오류 수정)
     */
    @EventHandler
    public void onEatMeatball(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        UUID playerUUID = player.getUniqueId();

        if (itemInHand.getType() == Material.COCOA_BEANS && itemInHand.hasItemMeta() &&
                itemInHand.getItemMeta().getDisplayName().equals(MEATBALL_NAME)) {

            event.setCancelled(true);

            long now = System.currentTimeMillis();
            if (eatCooldowns.getOrDefault(playerUUID, 0L) > now) {
                return;
            }
            eatCooldowns.put(playerUUID, now + 800);

            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
            itemInHand.setAmount(itemInHand.getAmount() - 1);

            PER_DATA data = PER_DATA.getInstance();

            // --- 버프 중첩 방지 및 갱신 로직 ---
            if (activeStrengthBuffs.containsKey(playerUUID)) {
                // 이미 버프가 걸려있다면, 기존의 버프 제거 작업을 취소합니다.
                int existingTaskId = activeStrengthBuffs.get(playerUUID);
                Bukkit.getScheduler().cancelTask(existingTaskId);
                player.sendMessage("§e미트볼 효과의 지속 시간이 1분으로 초기화되었습니다.");
            } else {
                // 새로 버프가 걸리는 경우에만 힘 스탯을 1000 올립니다.
                double currentStrength = data.getPlayerStrength(playerUUID);
                data.setPlayerStrength(playerUUID, currentStrength + 1000.0);
                player.sendMessage("§c미트볼의 힘이 솟아나 1분간 힘이 강력해집니다! (힘 +1000)");
            }

            // --- 버프 제거 예약 (수정된 부분) ---
            // 새로운 1분짜리 버프 제거 작업을 예약합니다.
            BukkitTask newTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 1분 뒤, 이 작업이 실행될 때 플레이어가 온라인 상태인지 확인합니다.
                if (player.isOnline()) {
                    // 이 작업이 실행되었다는 것은 버프가 만료되었다는 의미이므로,
                    // 현재 힘 스탯에서 1000을 빼서 원래대로 되돌립니다.
                    double strengthAfterBuff = data.getPlayerStrength(playerUUID);
                    data.setPlayerStrength(playerUUID, strengthAfterBuff - 1000.0);
                    player.sendMessage("§7미트볼의 효과가 사라졌습니다.");
                }
                // 작업이 완료되었으므로, 추적 맵에서 플레이어 정보를 제거합니다.
                activeStrengthBuffs.remove(playerUUID);
            }, 1200L); // 1분 = 1200틱

            // 맵에 플레이어와 새로 생성된 작업의 ID를 기록합니다.
            activeStrengthBuffs.put(playerUUID, newTask.getTaskId());
        }
    }

    // (이하 다른 코드들은 이전과 동일합니다)

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
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
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) return;

        if (event.isShiftClick() && clickedInventory.equals(player.getInventory())) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                for (int i = 1; i <= 7; i++) {
                    if (topInventory.getItem(i) == null) {
                        ItemStack singleItem = clickedItem.clone();
                        singleItem.setAmount(1);
                        topInventory.setItem(i, singleItem);
                        clickedItem.setAmount(clickedItem.getAmount() - 1);
                        player.updateInventory();
                        return;
                    }
                }
            }
        } else if (clickedInventory.equals(topInventory)) {
            int slot = event.getSlot();
            if (slot == 0 || slot == 8) {
                event.setCancelled(true);
                if (slot == 8) handleCooking(player, topInventory);
                return;
            }
            ItemStack currentItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            if (currentItem != null && currentItem.getType() != Material.AIR &&
                    cursorItem != null && cursorItem.getType() != Material.AIR) {
                event.setCancelled(true);
            }
            if (event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
            }
        }
    }

    private void openCampfireGui(Player player) {
        Inventory campfireGui = Bukkit.createInventory(player, 9, GUI_TITLE);
        ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName("§c취소");
        cancelItem.setItemMeta(cancelMeta);
        ItemStack cookItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cookMeta = cookItem.getItemMeta();
        cookMeta.setDisplayName("§a조리");
        cookItem.setItemMeta(cookMeta);
        campfireGui.setItem(0, cancelItem);
        campfireGui.setItem(8, cookItem);
        player.openInventory(campfireGui);
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
        meta.setDisplayName(MEATBALL_NAME);
        meta.setLore(Collections.singletonList("§7잘 다져진 고기를 뭉쳐 만들었다."));
        meatball.setItemMeta(meta);
        return meatball;
    }
}