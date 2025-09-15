package org.role.rPG;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cash implements Listener {

    private static final Map<UUID, Integer> playerMoney = new HashMap<>();
    private static JavaPlugin plugin; // 플러그인 인스턴스를 저장할 변수

    /**
     * Cash 시스템을 초기화하고 리스너를 등록하며, 데이터를 불러옵니다.
     * @param mainPlugin 메인 플러그인 인스턴스 (RPG.java의 this)
     */
    public static void initializeAndLoad(JavaPlugin mainPlugin) {
        plugin = mainPlugin;
        mainPlugin.getServer().getPluginManager().registerEvents(new Cash(), mainPlugin);
        loadAllPlayerData();
    }

    // (onPlayerInteract 메서드는 이전과 동일)
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack itemInHand = event.getItem();
        if ((action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) || itemInHand == null) return;
        if (!itemInHand.hasItemMeta()) return;
        ItemMeta meta = itemInHand.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            Integer amountObj = container.get(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER);
            if (amountObj == null) return;
            int amount = amountObj;
            if (player.isSneaking()) {
                int totalAmount = 0;
                int totalItemsUsed = 0;
                PlayerInventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack currentItem = inventory.getItem(i);
                    if (currentItem != null && currentItem.hasItemMeta()) {
                        PersistentDataContainer currentContainer = currentItem.getItemMeta().getPersistentDataContainer();
                        if (currentContainer.has(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER)) {
                            Integer currentAmountObj = currentContainer.get(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER);
                            if (currentAmountObj != null && currentAmountObj == amount) {
                                totalAmount += currentAmountObj * currentItem.getAmount();
                                totalItemsUsed += currentItem.getAmount();
                                inventory.setItem(i, null);
                            }
                        }
                    }
                }
                if (totalAmount > 0) {
                    addMoney(player, totalAmount);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
                    player.sendMessage("§e수표 " + totalItemsUsed + "장을 모두 사용하여 §6" + String.format("%,d", totalAmount) + "G§f를 획득했습니다.");
                }
            } else {
                addMoney(player, amount);
                itemInHand.setAmount(itemInHand.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage("§e" + String.format("%,d", amount) + "G§f를 획득했습니다.");
            }
        }
    }


    // --- 기본 돈 관리 메서드 (변경 없음) ---
    public static void addMoney(Player player, int amount) {
        playerMoney.put(player.getUniqueId(), getMoney(player) + amount);
    }
    public static void removeMoney(Player player, int amount) {
        playerMoney.put(player.getUniqueId(), getMoney(player) - amount);
    }
    public static int getMoney(Player player) {
        return playerMoney.getOrDefault(player.getUniqueId(), 0);
    }
    public static void unloadPlayerData(Player player) {
        playerMoney.remove(player.getUniqueId());
    }

    // --- ▼▼▼ 데이터 저장 및 불러오기 기능 추가 ▼▼▼ ---

    /**
     * [저장] 모든 돈 데이터를 config.yml 파일에 저장합니다.
     */
    public static void saveAllPlayerData() {
        if (plugin == null) return; // 플러그인이 비활성화된 경우를 대비
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<UUID, Integer> entry : playerMoney.entrySet()) {
            // "player_money.플레이어UUID" 경로에 돈을 저장합니다.
            config.set("player_money." + entry.getKey().toString(), entry.getValue());
        }
        plugin.saveConfig(); // 변경사항을 파일에 최종적으로 기록
        plugin.getLogger().info("플레이어 돈 데이터가 저장되었습니다.");
    }

    /**
     * [불러오기] config.yml 파일에서 모든 돈 데이터를 불러옵니다.
     */
    public static void loadAllPlayerData() {
        if (plugin == null) return; // 플러그인이 비활성화된 경우를 대비
        FileConfiguration config = plugin.getConfig();
        // 'player_money' 항목이 파일에 없으면 아무것도 하지 않음
        ConfigurationSection moneySection = config.getConfigurationSection("player_money");
        if (moneySection == null) {
            return;
        }

        // 파일에 저장된 모든 플레이어의 돈 정보를 하나씩 읽어서 메모리에 올립니다.
        for (String uuidString : moneySection.getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(uuidString);
                int money = config.getInt("player_money." + uuidString);
                playerMoney.put(playerUUID, money);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("config.yml에서 잘못된 UUID 형식을 발견했습니다: " + uuidString);
            }
        }
        plugin.getLogger().info("플레이어 돈 데이터를 불러왔습니다.");
    }
}