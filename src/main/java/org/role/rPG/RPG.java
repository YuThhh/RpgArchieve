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

public final class RPG extends JavaPlugin implements CommandExecutor, Listener {

    public static NamespacedKey SUCHECK_VALUE_KEY;
    private TablistManager tablistManager;

    @Override
    public void onEnable() {
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        // 데이터 관리자 초기화
        new PER_DATA();

        // TabListManager 초기화 및 스케줄러 시작
        this.tablistManager = new TablistManager(this);
        this.tablistManager.startUpdater();

        // 각 기능 클래스의 register 메소드를 호출하여 시스템을 활성화합니다.
        new CMD_manager(this).registerCommands();
        new LIS_manager(this).registerListeners();
        Ui.register(this, tablistManager);
        Cash.register(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Stat(), this);

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
}