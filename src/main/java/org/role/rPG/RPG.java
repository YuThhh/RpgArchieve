package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.UUID;

public final class RPG extends JavaPlugin implements Listener {

    public static NamespacedKey SUCHECK_VALUE_KEY;
    private IndicatorManager indicatorManager;

    private static final double NormalHpRegen = 1;
    private static final double NormalMpRegen = 3;

    @Override
    public void onEnable() {
        Cash.initializeAndLoad(this);

        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        // 데이터 관리자 초기화
        new PER_DATA();

        this.indicatorManager = new IndicatorManager(this);

        StatDataManager.initialize(this);
        StatDataManager.loadAllStats();

        this.startStartUpdater();

        // 각 기능 클래스의 register 메소드를 호출하여 시스템을 활성화합니다.
        new CMD_manager(this).registerCommands();
        new LIS_manager(this).registerListeners();
        Ui.register(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Stat(this), this);
        getServer().getPluginManager().registerEvents(new Indicater(indicatorManager), this);
        getServer().getPluginManager().registerEvents(new Cooked(this), this);

        startStartUpdater();
        Regeneration();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolder(this).register();
            getLogger().info("RPG's PlaceholderAPI Expansion has been registered.");
        }

        getLogger().info("RPG Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        Cash.saveAllPlayerData();
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("RPG Plugin Disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 데이터는 이미 메모리에 있으므로 별도의 로딩이 필요 없습니다.
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // --- ▼▼▼ 여기가 수정되었습니다 ▼▼▼ ---
        // 플레이어가 나갈 때 데이터를 파일에 저장만 합니다.
        Cash.saveAllPlayerData();
        StatDataManager.saveAllStats();
        // 메모리에서 데이터를 지우는 unloadPlayerData()를 호출하지 않습니다.
        // Cash.unloadPlayerData(event.getPlayer()); // 이 줄을 제거!
    }

    public void startStartUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    PER_DATA data = PER_DATA.getInstance();
                    UUID playerUUID = player.getUniqueId();

                    float speedStat = data.getPlayerSpeed(playerUUID);
                    if (speedStat > 400) speedStat = 400f;
                    else if (speedStat < 0) speedStat = 0;
                    data.setPlayerSpeed(playerUUID, speedStat);
                    float calculatedSpeed = 0.2f * (1 + speedStat * 0.01f);
                    if (Math.abs(player.getWalkSpeed() - calculatedSpeed) > 0.0001f) {
                        player.setWalkSpeed(calculatedSpeed);
                    }
                    double maxHealthStat = data.getplayerMaxHealth(playerUUID);
                    if (maxHealthStat <= 0) maxHealthStat = 1;
                    data.setplayerMaxHealth(playerUUID, maxHealthStat);
                    if (Math.abs(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue() - maxHealthStat) > 0.01) {
                        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(maxHealthStat);
                    }

                    // 여기에 나중에 체력, 방어력 등 다른 스탯도 추가할 수 있습니다.
                    // 예: applyPlayerMaxHealth(player);
                }
            }
        }.runTaskTimer(this, 0L, 10L);
    }
    public void Regeneration() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerUUID = player.getUniqueId();
                    PER_DATA data = PER_DATA.getInstance();

                    // HP 재생 로직
                    double maxHealth = data.getplayerMaxHealth(playerUUID);
                    double currentHealth = player.getHealth();

                    if (currentHealth < maxHealth) {
                        double vital = data.getPlayerHpRegenarationBonus(playerUUID);
                        double hpRegenAmount = 0.5 * (NormalHpRegen + maxHealth * 0.01 * (1 + vital * 0.01));
                        player.setHealth(Math.min(maxHealth, currentHealth + hpRegenAmount));
                    }
                    double maxMp = data.getPlayerMaxMana(playerUUID);
                    double currentMp = data.getPlayerCurrentMana(playerUUID);
                    if (currentMp < maxMp) {
                        double mpRegenAmount = (NormalMpRegen + maxMp * 0.02);
                        data.setPlayerCurrentMana(playerUUID, Math.min(maxMp, currentMp + mpRegenAmount));
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }
}