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
import org.role.rPG.Level.LevelManager;
import org.role.rPG.Skill.SkillListener;
import org.role.rPG.Mob.DummyMob;
import org.role.rPG.Mob.MobManager;
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
    private MobManager mobManager;
    private LevelManager levelManager;

    @Override
    public void onEnable() {
        getLogger().info("RPG Plugin is enabling...");
        Cash.initializeAndLoad(this);
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        new PER_DATA(); // 데이터 관리자 초기화
        StatDataManager.initialize(this);
        StatDataManager.loadAllStats();

        // --- 2. 핵심 매니저(부품) 생성 (의존성 순서에 맞게) ---
        // 의존성이 없는 기본 매니저들을 먼저 생성합니다.
        this.indicatorManager = new IndicatorManager(this);
        this.reforgeManager = new ReforgeManager(this); // ReforgeManager는 plugin 참조가 필요 없어 보입니다. 필요하다면 new ReforgeManager(this)로 수정
        this.mobManager = new MobManager(this);

        // 다른 매니저를 필요로 하는 매니저들을 생성합니다.
        this.itemManager = new ItemManager(this, this.reforgeManager); // ItemManager는 ReforgeManager가 필요
        this.statManager = new StatManager(this, this.itemManager);     // StatManager는 ItemManager가 필요
        this.levelManager = new LevelManager(this, this.statManager);

        // UI 클래스를 생성합니다.
        this.reforgeUi = new Reforge_UI(itemManager, statManager, reforgeManager);

        // --- 3. 설정 및 콘텐츠 로드/등록 ---
        this.itemManager.reloadItems(); // 아이템 로드
        this.mobManager.registerMob(new DummyMob(this)); // 커스텀 몹 등록

        // --- 4. 명령어 관리자(CMD_manager) 생성 및 등록 ---
        // 모든 부품이 준비된 후, 마지막으로 CMD_manager를 단 한 번 생성하고 등록합니다.
        CMD_manager cmdManager = new CMD_manager(this, this.itemManager, this.reforgeUi, this.mobManager);
        cmdManager.registerCommands();

        // --- 5. 이벤트 리스너(Listener) 등록 ---
        // 여러 클래스에 흩어져 있는 이벤트 핸들러들을 서버에 등록합니다.
        getServer().getPluginManager().registerEvents(this, this); // onPlayerJoin/Quit 등
        getServer().getPluginManager().registerEvents(new LIS_manager(this), this); // LIS_manager 등록 (기존 코드에 있었음)
        getServer().getPluginManager().registerEvents(new Stat(this, this.statManager, this.indicatorManager), this);
        getServer().getPluginManager().registerEvents(new Cooked(this), this);
        getServer().getPluginManager().registerEvents(new EquipmentListener(this, this.statManager), this);
        getServer().getPluginManager().registerEvents(new Ui(this, this.statManager, this.levelManager), this);
        getServer().getPluginManager().registerEvents(new ItemUpdateListener(this.itemManager), this);
        getServer().getPluginManager().registerEvents(this.reforgeUi, this); // Reforge_UI 리스너 등록
        getServer().getPluginManager().registerEvents(new SkillListener(this, this.itemManager, this.statManager), this);
        getServer().getPluginManager().registerEvents(new org.role.rPG.Level.ExperienceListener(this.levelManager, this.mobManager), this);


        // --- 6. PlaceholderAPI 등록 및 후속 작업 ---
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolder(this, this.statManager).register();
            getLogger().info("PlaceholderAPI expansion for RPG has been registered.");
        }

        // 체력/마나 재생 스케줄러를 시작합니다.
        Regeneration();

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