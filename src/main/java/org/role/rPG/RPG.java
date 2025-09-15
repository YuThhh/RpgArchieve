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
    // TablistManager 변수 삭제

    private static final double NormalHpRegen = 1;
    private static final double NormalMpRegen = 3;

    @Override
    public void onEnable() {
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        new PER_DATA();
        this.indicatorManager = new IndicatorManager(this);

        // 매니저 및 리스너 등록
        new CMD_manager(this).registerCommands();
        new LIS_manager(this).registerListeners();
        Ui.register(this, null); // TablistManager 없으므로 null 전달
        Cash.register(this); // Cash를 리스너로 등록하는 원래 방식으로 복구

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Stat(this), this);
        getServer().getPluginManager().registerEvents(new Indicater(indicatorManager), this);
        getServer().getPluginManager().registerEvents(new Cooked(this), this);

        // 스케줄러 시작
        startStartUpdater();
        Regeneration();

        getLogger().info("RPG Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // 데이터 저장 로직 제거
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("RPG Plugin Disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // TablistManager 관련 코드 없음
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // TablistManager 관련 코드 없음
        Cash.unloadPlayerData(event.getPlayer());
    }

    // startStartUpdater, Regeneration 메서드는 변경 없음
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

                    double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
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