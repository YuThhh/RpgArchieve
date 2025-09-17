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
import org.role.rPG.Player.StatManager;

import java.util.Objects;
import java.util.UUID;

public class Ui implements Listener {

    // ▼▼▼ [수정] 필드에 plugin과 statManager 추가 ▼▼▼
    private final JavaPlugin plugin;
    private final StatManager statManager;
    private final PER_DATA data = PER_DATA.getInstance(); // 마나처럼 StatManager가 관리하지 않는 데이터용

    // ▼▼▼ [수정] 생성자를 통해 필요한 인스턴스를 받아옵니다 ▼▼▼
    public Ui(JavaPlugin plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;

        // 생성자에서 직접 스케줄러를 시작합니다.
        startActionBarUpdater();
        startScoreboardUpdater();
    }

    // ▼▼▼ [삭제] register 메소드는 더 이상 필요 없으므로 삭제합니다 ▼▼▼
    // public static void register(...) { ... }

    // ================= 이벤트 핸들러 =================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cash.unloadPlayerData(event.getPlayer()); // 이 부분은 RPG.java에서 관리하므로 중복
        event.getPlayer().sendPlayerListHeaderAndFooter(Component.empty(),Component.empty());
    }

    // ================= 액션바 =================
    // ▼▼▼ [수정] static 제거, 메소드 내부 로직 변경 ▼▼▼
    public void startActionBarUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerUUID = player.getUniqueId();

                    // 체력은 플레이어 객체에서 직접 가져오는 것이 가장 정확합니다.
                    int currentHealth = (int) player.getHealth();
                    int maxHealth = (int) Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();

                    // ▼▼▼ [핵심 수정] PER_DATA 대신 StatManager에서 최종 스탯을 가져옵니다 ▼▼▼
                    int defense = (int) statManager.getFinalStat(playerUUID, "DEFENSE");
                    int str = (int) statManager.getFinalStat(playerUUID, "STRENGTH");
                    // 공격속도는 Attribute를 직접 가져오는 것이 더 정확합니다.
                    double atkSpdValue = statManager.getFinalStat(playerUUID, "ATTACK_SPEED");

                    // 마나는 아직 StatManager에서 관리하지 않으므로 PER_DATA를 유지합니다.
                    int maxMp = (int) data.getPlayerMaxMana(playerUUID);
                    int currentMp = (int) data.getPlayerCurrentMana(playerUUID);

                    Component message = Component.text("♥ " + currentHealth + "/" + maxHealth, NamedTextColor.RED)
                            .append(Component.text("  \uD83D\uDEE1 " + defense, NamedTextColor.GREEN)) // 방패 아이콘 추가
                            .append(Component.text("  \uD83D\uDCA7 " + currentMp + "/" + maxMp, NamedTextColor.AQUA)) // 물방울 아이콘 추가
                            .append(Component.text("  \uD83D\uDCAA " + str, NamedTextColor.RED)) // 근육 아이콘 추가
                            .append(Component.text("  ⚔ " + String.format("%.2f", atkSpdValue), NamedTextColor.YELLOW)); // 칼 아이콘 추가

                    sendActionBar(player, message);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // 비동기(Async) 대신 동기 방식으로 변경하여 안정성 확보
    }

    private void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }


    // ================= 스코어보드 =================
    // ▼▼▼ [수정] static 제거 ▼▼▼
    public void setScoreboard(Player player) {
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

    public void startScoreboardUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Scoreboard board = player.getScoreboard();
                    if (board.getObjective("rpg_info") == null) continue;

                    int currentMoney = Cash.getMoney(player);

                    Team moneyTeam = board.getTeam("rpg_money");
                    if (moneyTeam != null) {
                        Component prefix = Component.text(String.format("%,d", currentMoney),NamedTextColor.YELLOW)
                                .append(Component.text(" G"));

                        moneyTeam.prefix(prefix);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}