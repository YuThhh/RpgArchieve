package org.role.rPG;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.Objects;
import java.util.UUID;

public class Ui implements Listener {

    private static final PER_DATA data = PER_DATA.getInstance();

    // --- [핵심 수정] TablistManager manager 매개변수를 삭제했습니다. ---
    public static void register(JavaPlugin plugin, Object manager) { // Object 타입으로 임시 변경하여 호환성 유지
        plugin.getServer().getPluginManager().registerEvents(new Ui(), plugin);
        startActionBarUpdater(plugin);
        startScoreboardUpdater(plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            setScoreboard(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 탭리스트 관련 코드는 없으므로 기본 헤더/푸터 초기화만 남깁니다.
        event.getPlayer().sendPlayerListHeaderAndFooter(Component.empty(),Component.empty());
    }

    // (이하 다른 코드는 이전과 동일합니다)
    public static void startActionBarUpdater(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID  playerUUID = player.getUniqueId();
                    int currentHealth = (int) player.getHealth();
                    int maxHealth = (int) Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
                    int defense = (int) data.getPlayerDefense(playerUUID);
                    int str = (int) data.getPlayerStrength(playerUUID);
                    int atkspd = (int) data.getPlayerAttactSpeed(playerUUID);

                    Component message = Component.text("♥ "+currentHealth + "/" + maxHealth, NamedTextColor.RED)
                            .append(Component.text("  DEF " + defense, NamedTextColor.GREEN))
                            .append(Component.text("  STR " + str, NamedTextColor.RED))
                            .append(Component.text("  ATKSPD " + atkspd, NamedTextColor.YELLOW));
                    sendActionBar(player, message);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    private static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }

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
                        Component prefix = Component.text(String.format("%,d", currentMoney), NamedTextColor.YELLOW)
                                .append(Component.text("G"));
                        moneyTeam.prefix(prefix);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}