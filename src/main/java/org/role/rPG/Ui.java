package org.role.rPG;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public class Ui implements Listener {

    private static final PER_DATA data = PER_DATA.getInstance();

    /**
     * Ui 시스템(이벤트, 스케줄러)을 서버에 등록합니다.
     * @param plugin 메인 클래스 인스턴스
     */
    public static void register(JavaPlugin plugin) {
        // Ui 클래스의 이벤트 핸들러(onPlayerJoin 등)를 등록합니다.
        plugin.getServer().getPluginManager().registerEvents(new Ui(), plugin);

        // 액션바와 스코어보드 업데이트를 시작합니다.
        startActionBarUpdater(plugin);
        startScoreboardUpdater(plugin);
    }

    // --- GUI 메뉴 관련 코드 (그대로 유지) ---
    private final Inventory inv;

    public Ui() {
        this.inv = Bukkit.createInventory(null, 27, "메뉴");
        this.initializeItems();
    }

    public void initializeItems() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName("§b공격 스킬");
        swordMeta.setLore(Collections.singletonList("§7클릭하여 스킬 목록을 봅니다."));
        sword.setItemMeta(swordMeta);

        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta shieldMeta = shield.getItemMeta();
        shieldMeta.setDisplayName("§a방어 스킬");
        shieldMeta.setLore(Collections.singletonList("§7클릭하여 스킬 목록을 봅니다."));
        shield.setItemMeta(shieldMeta);

        inv.setItem(11, sword);
        inv.setItem(15, shield);
    }

    public void open(Player player) {
        player.openInventory(inv);
    }


    // ================= 이벤트 핸들러 =================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Cash.unloadPlayerData(event.getPlayer());
    }


    // --- 정적(전역 UI) 관련 코드 ---

    // ================= 액션바 =================
    public static void startActionBarUpdater(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID  playerUUID = player.getUniqueId();

                    int currentHealth = (int) player.getHealth();
                    int maxHealth = (int) Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
                    int defense = (int) data.getPlayerDefense(playerUUID);
                    int mp = 100;
                    String message = String.format("§c❤ %d/%d  §aDEF %d  §bMP %d",
                            currentHealth, maxHealth, mp, defense);
                    sendActionBar(player, message);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    private static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }


    // ================= 스코어보드 =================
    public static void setScoreboard(Player player) {
        ScoreboardManager bukkitScoreboardManager = Bukkit.getScoreboardManager();

        Scoreboard board = bukkitScoreboardManager.getNewScoreboard();
        Objective objective = board.registerNewObjective("rpg_info", "dummy", "§e§lMY INFO");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore("§a").setScore(5);
        objective.getScore("§f보유 중인 돈").setScore(4);

        Team moneyTeam = board.registerNewTeam("rpg_money");
        moneyTeam.addEntry("§e");
        objective.getScore("§e").setScore(3);

        objective.getScore("§b").setScore(2);

        player.setScoreboard(board);
    }

    public static void startScoreboardUpdater(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Scoreboard board = player.getScoreboard();
                    if (board.getObjective("rpg_info") == null) continue;

                    int currentMoney = Cash.getMoney(player);

                    Team moneyTeam = board.getTeam("rpg_money");
                    if (moneyTeam != null) {
                        moneyTeam.setPrefix("§6" + String.format("%,d", currentMoney) + " G");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}