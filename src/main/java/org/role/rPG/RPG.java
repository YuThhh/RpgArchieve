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
import org.role.rPG.Effect.EffectManager;
import org.role.rPG.Indicator.IndicatorManager;
import org.role.rPG.Item.*;
import org.role.rPG.Level.LevelManager;
import org.role.rPG.Mob.DummyMob;
import org.role.rPG.Mob.MobManager;
import org.role.rPG.Mob.MobPatternRunnable;
import org.role.rPG.Player.*;
import org.role.rPG.UI.Reforge_UI;

import java.util.UUID;

public final class RPG extends JavaPlugin implements Listener {

    public static NamespacedKey SUCHECK_VALUE_KEY;
    private IndicatorManager indicatorManager;
    private ItemManager itemManager;
    private StatManager statManager;
    private ReforgeManager reforgeManager;
    private Reforge_UI reforgeUi;
    private MobManager mobManager;
    private LevelManager levelManager;
    private EffectManager effectManager;

    private static final double NormalHpRegen = 1;
    private static final double NormalMpRegen = 3;

    @Override
    public void onEnable() {
        getLogger().info("RPG Plugin is enabling...");
        // --- 1. 데이터 및 기본 설정 초기화 ---
        Cash.initializeAndLoad(this);
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        new PER_DATA();
        StatDataManager.initialize(this);
        StatDataManager.loadAllStats();

        // --- 2. 핵심 매니저(부품) 생성 ---
        this.indicatorManager = new IndicatorManager(this);
        this.reforgeManager = new ReforgeManager(this);
        this.mobManager = new MobManager(this);
        this.itemManager = new ItemManager(this, this.reforgeManager);
        this.statManager = new StatManager(this, this.itemManager);
        this.levelManager = new LevelManager(this, this.statManager);
        this.effectManager = new EffectManager(this);

        // 2-1. UI 인스턴스 생성 코드를 제거합니다.
        // this.reforgeUi = new Reforge_UI(itemManager, statManager, reforgeManager);

        // --- 3. 콘텐츠 로드/등록 ---
        this.itemManager.reloadItems();
        this.mobManager.registerMob(new DummyMob(this));

        // --- 4. 명령어 관리자(CMD_manager) 등록 ---
        // CMD_manager에 Reforge_UI를 넘기는 대신, 필요한 매니저들을 넘겨주도록 수정해야 합니다.
        // 또는 CMD_manager 내부에서 new Reforge_UI()를 호출하도록 구조를 변경합니다. (후자를 추천)
        CMD_manager cmdManager = new CMD_manager(this, this.itemManager, this.reforgeManager, this.statManager, this.mobManager);
        cmdManager.registerCommands();

        // --- 5. 이벤트 리스너(Listener) 등록 ---
        // 5-1. 메인 클래스(this)를 리스너로 직접 등록합니다. (PlayerJoin/Quit 이벤트 처리용)
        getServer().getPluginManager().registerEvents(this, this);

        // 5-2. LIS_manager를 통해 나머지 리스너들을 등록합니다.
        LIS_manager lisManager = new LIS_manager(this);
        lisManager.registerGeneralListeners(statManager, indicatorManager, itemManager, levelManager, mobManager, effectManager);


        // --- 6. PlaceholderAPI 등록 및 후속 작업 ---
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolder(this, this.statManager).register();
            getLogger().info("PlaceholderAPI expansion for RPG has been registered.");
        }

        // --- 7. 스케줄러 시작 ---
        Regeneration();
        new MobPatternRunnable(this.mobManager).runTaskTimer(this, 0L, 20L);

        getLogger().info("RPG Plugin has been enabled successfully!");
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
                    } else {
                        statManager.updatePlayerCurrentMana(playerUUID, maxMp);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }
}