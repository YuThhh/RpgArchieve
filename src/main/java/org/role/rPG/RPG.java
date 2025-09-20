package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.role.rPG.Food.Cooked;
import org.role.rPG.Indicator.IndicatorManager;
import org.role.rPG.Item.*;
import org.role.rPG.Magic.MagicListener;
import org.role.rPG.Player.*;
import org.role.rPG.UI.Reforge_UI;
import org.role.rPG.UI.Ui;

import java.util.UUID;

public final class RPG extends JavaPlugin implements Listener {

    public static NamespacedKey SUCHECK_VALUE_KEY;
    private IndicatorManager indicatorManager;
    private ItemManager itemManager;
    private StatManager statManager;
    private ReforgeManager reforgeManager;
    private Reforge_UI reforgeUi;

    private static final double NormalHpRegen = 1;
    private static final double NormalMpRegen = 3;

    @Override
    public void onEnable() {
        Cash.initializeAndLoad(this);

        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        // 데이터 관리자 초기화
        new PER_DATA();

        this.indicatorManager = new IndicatorManager(this);
        this.reforgeManager = new ReforgeManager(this);
        this.itemManager = new ItemManager(this, this.reforgeManager); // ReforgeManager 전달
        this.statManager = new StatManager(this, this.itemManager);
        this.itemManager.reloadItems();
        this.reforgeUi = new Reforge_UI(itemManager, statManager, reforgeManager);

        StatDataManager.initialize(this);
        StatDataManager.loadAllStats();

        // 각 기능 클래스의 register 메소드를 호출하여 시스템을 활성화합니다.
        new CMD_manager(this, this.itemManager, this.reforgeUi).registerCommands();
        new LIS_manager(this).registerListeners();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Stat(this, this.statManager, this.indicatorManager), this);
        getServer().getPluginManager().registerEvents(new Cooked(this), this);
        getServer().getPluginManager().registerEvents(new EquipmentListener(this, this.statManager), this);
        getServer().getPluginManager().registerEvents(new Ui(this, this.statManager), this);
        getServer().getPluginManager().registerEvents(new ItemUpdateListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(this.reforgeUi, this);
        getServer().getPluginManager().registerEvents(new MagicListener(this, this.itemManager, this.statManager), this);

        Regeneration();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolder(this,this.statManager).register();
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
        Cash.loadAllPlayerData();
        StatDataManager.loadAllStats();
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

    public void Regeneration() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerUUID = player.getUniqueId();

                    // HP 재생 로직
                    double maxHealth = statManager.getFinalStat(playerUUID, "MAX_HEALTH");
                    double currentHealth = player.getHealth();

                    if (currentHealth < maxHealth) {
                        // vital 스탯도 StatManager를 통해 가져와야 하지만, 현재 구조상 PER_DATA에서 가져옵니다.
                        // 추후 vital도 장비 스탯으로 관리하려면 StatManager에 추가해야 합니다.
                        double vital = PER_DATA.getInstance().getPlayerHpRegenarationBonus(playerUUID);
                        double hpRegenAmount = 0.5 * (NormalHpRegen + maxHealth * 0.01 * (1 + vital * 0.01));
                        player.setHealth(Math.min(maxHealth, currentHealth + hpRegenAmount));
                    }

                    double maxMp = statManager.getFinalStat(playerUUID,"MAX_MANA");
                    // 현재 마나는 여전히 statManager를 통해 가져옵니다 (캐시된 값).
                    double currentMp = statManager.getFinalStat(playerUUID,"CURRENT_MANA");

                    if (currentMp < maxMp) {
                        double mpRegenAmount = (NormalMpRegen + maxMp * 0.02);
                        double newCurrentMp = Math.min(maxMp, currentMp + mpRegenAmount);

                        // StatManager에 새로 만든 메서드를 호출하여 데이터를 업데이트합니다.
                        statManager.updatePlayerCurrentMana(playerUUID, newCurrentMp);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }
}