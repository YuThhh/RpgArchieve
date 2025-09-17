package org.role.rPG.UI;

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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.role.rPG.Player.Cash;
import org.role.rPG.Player.PER_DATA;

import java.util.*;

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

    // ================= 이벤트 핸들러 =================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Cash.unloadPlayerData(event.getPlayer());
        event.getPlayer().sendPlayerListHeaderAndFooter(Component.empty(),Component.empty());


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
                    int maxMp = (int) data.getPlayerMaxMana(playerUUID);
                    int currentMp = (int) data.getPlayerCurrentMana(playerUUID);
                    int str = (int) data.getPlayerStrength(playerUUID);
                    int atkspd = (int) data.getPlayerAttactSpeed(playerUUID);

                    Component message = Component.text("♥ "+currentHealth + "/" + maxHealth, NamedTextColor.RED)
                            .append(Component.text("  DEF " + defense, NamedTextColor.GREEN))
                            .append(Component.text("  MP " + currentMp + "/" + maxMp, NamedTextColor.AQUA))
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


}