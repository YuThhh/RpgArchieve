package org.role.rPG;

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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class Ui implements Listener {

    private static final PER_DATA data = PER_DATA.getInstance();
    private static TablistManager tabListManager;

    /**
     * Ui 시스템(이벤트, 스케줄러)을 서버에 등록합니다.
     * @param plugin 메인 클래스 인스턴스
     */
    public static void register(JavaPlugin plugin, TablistManager manager) {
        // Ui 클래스의 이벤트 핸들러(onPlayerJoin 등)를 등록합니다.
        plugin.getServer().getPluginManager().registerEvents(new Ui(), plugin);
        tabListManager = manager; // [추가]


        // 액션바와 스코어보드 업데이트를 시작합니다.
        startActionBarUpdater(plugin);
        startScoreboardUpdater(plugin);
//        startTabListUpdater(plugin);
    }

    // --- GUI 메뉴 관련 코드 (그대로 유지) ---
    private final Inventory inv;

    public Ui() {
        this.inv = Bukkit.createInventory(null, 27, Component.text("메뉴"));
        this.initializeItems();
    }

    public void initializeItems() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        List<Component> swordlore = new ArrayList<>();
        swordlore.add(Component.text("§7클릭하여 스킬 목록을 봅니다."));

        swordMeta.displayName(Component.text("§b공격 스킬"));
        swordMeta.lore(swordlore);
        sword.setItemMeta(swordMeta);

        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta shieldMeta = shield.getItemMeta();
        List<Component> shieldlore = new ArrayList<>();
        swordlore.add(Component.text("§7클릭하여 스킬 목록을 봅니다."));

        shieldMeta.displayName(Component.text("§a방어 스킬"));
        shieldMeta.lore(shieldlore);
        shieldMeta.lore();
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
        event.getPlayer().sendPlayerListHeaderAndFooter(Component.empty(),Component.empty());

        // [추가] 플레이어가 나갈 때 생성된 가짜 플레이어를 제거합니다.
        if (tabListManager != null) {
            tabListManager.removeFakePlayers(event.getPlayer());
        }
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
                    Component message = Component.text("♥"+currentHealth + "/" + maxHealth, NamedTextColor.RED)
                            .append(Component.text("  DEF" + " " + defense, NamedTextColor.GREEN))
                            .append(Component.text("  MP" + " " + mp, NamedTextColor.AQUA));
                    sendActionBar(player, message);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    private static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }


    // ================= 스코어보드 =================
    public static void setScoreboard(Player player) {
        ScoreboardManager bukkitScoreboardManager = Bukkit.getScoreboardManager();

        Scoreboard board = bukkitScoreboardManager.getNewScoreboard();
        Objective objective = board.registerNewObjective("rpg_info", Criteria.DUMMY, Component.text("§e§lMY INFO"));
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
                        Component prefix = Component.text(currentMoney,NamedTextColor.YELLOW)
                                .append(Component.text("G"));

                        moneyTeam.prefix(prefix);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ================= 탭 리스트 =================
//    public static void startTabListUpdater(JavaPlugin plugin) {
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                for (Player player : Bukkit.getOnlinePlayers()) {
//                    // 탭 리스트에 넣을 변수들
//                    UUID playerUUID = player.getUniqueId();
//
//                    double crit = PER_DATA.getInstance().getPlayerCrit(playerUUID);
//                    double critdmg = PER_DATA.getInstance().getPlayerCritDamage(playerUUID);
//
//                    Component header = Component.text("\n 서버 탭리스트 만드는 중 \n",NamedTextColor.AQUA);
//                    Component footer = Component.text("크리티컬 " + crit + "%", NamedTextColor.BLUE)
//                            .append(Component.text("크리티컬 피해 " + critdmg + "%", NamedTextColor.BLUE));
//
//                    player.sendPlayerListHeaderAndFooter(header, footer);
//
//                    // TabListManager 초기화 및 스케줄러 시작
//
//                }
//            }
//        }.runTaskTimer(plugin, 0L, 10L);
//    }
}