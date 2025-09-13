package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.scheduler.BukkitRunnable;

public final class RPG extends JavaPlugin implements CommandExecutor, Listener {

    public static NamespacedKey SUCHECK_VALUE_KEY;
    private TablistManager tablistManager;
    private IndicatorManager indicatorManager;

    @Override
    public void onEnable() {
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        // 데이터 관리자 초기화
        new PER_DATA();

        this.indicatorManager = new IndicatorManager(this);

        // TabListManager 초기화 및 스케줄러 시작
        this.tablistManager = new TablistManager(this);
        this.tablistManager.startUpdater();


        // 각 기능 클래스의 register 메소드를 호출하여 시스템을 활성화합니다.
        new CMD_manager(this).registerCommands();
        new LIS_manager(this).registerListeners();
        Ui.register(this, tablistManager);
        Cash.register(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Stat(this), this);
        getServer().getPluginManager().registerEvents(new Indicater(indicatorManager), this);

        getServer().getPluginManager().registerEvents(new Cooked(this), this);

        getLogger().info("RPG Plugin has been enabled!");

        // 서버 리로드 시 온라인 상태인 플레이어에게도 탭리스트 적용
        for (Player player : Bukkit.getOnlinePlayers()) {
            tablistManager.createFakePlayers(player);
        }

    }

    @Override
    public void onDisable() {
        // 서버 종료 시 모든 플레이어의 탭리스트 제거
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (tablistManager != null) {
                tablistManager.removeFakePlayers(player);
            }
        }
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("RPG Plugin Disabled.");
    }

    // [추가] 플레이어 접속 시 탭리스트 즉시 생성
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 살짝 딜레이를 주어 다른 플러그인과 충돌 방지 및 안정성 확보
        Bukkit.getScheduler().runTaskLater(this, () ->
                tablistManager.createFakePlayers(event.getPlayer()), 20L); // 1초 딜레이
    }

    // [추가] 플레이어 퇴장 시 탭리스트 즉시 제거
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tablistManager.removeFakePlayers(event.getPlayer());
    }

    public void startStartUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    PER_DATA data = PER_DATA.getInstance();

                    // 1. 이동 속도 스탯을 가져옵니다.
                    double speedStat = data.getPlayerSpeed(player.getUniqueId());
                    float targetSpeed = (speedStat > 0) ? (float) speedStat : 0.2f; // 0.2f는 바닐라 기본 속도

                    // 2. 현재 플레이어의 실제 이동 속도와 스탯이 다른지 확인합니다.
                    //    (불필요한 변경을 막기 위해 값이 다를 때만 적용하는 것이 중요합니다)
                    if (Math.abs(player.getWalkSpeed() - targetSpeed) > 0.001f) {
                        player.setWalkSpeed(targetSpeed);
                    }

                    // 여기에 나중에 체력, 방어력 등 다른 스탯도 추가할 수 있습니다.
                    // 예: applyPlayerMaxHealth(player);
                }
            }
        }.runTaskTimer(this, 0L, 10L);
    }
}