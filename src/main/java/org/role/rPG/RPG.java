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
import org.role.rPG.Craft.CraftManager;
import org.role.rPG.Effect.EffectManager;
import org.role.rPG.Enchant.EnchantmentManager;
import org.role.rPG.Indicator.IndicatorManager;
import org.role.rPG.Item.*;
import org.role.rPG.Level.LevelManager;
import org.role.rPG.Mob.MobManager;
import org.role.rPG.Mob.MobPatternRunnable;
import org.role.rPG.Player.*;
import org.role.rPG.UI.Reforge_UI;

import java.util.UUID;

// test workflow asdfjkl;asdfㅁㄴㅇㄹ
public final class RPG extends JavaPlugin implements Listener { // 메인 클래스

    // 필요한 클래스 선언
    public static NamespacedKey SUCHECK_VALUE_KEY;
    private IndicatorManager indicatorManager;
    private ItemManager itemManager;
    private StatManager statManager;
    private ReforgeManager reforgeManager;
    private Reforge_UI reforgeUi;
    private MobManager mobManager;
    private LevelManager levelManager;
    private EffectManager effectManager;
    private EnchantmentManager enchantmentManager;
    private CraftManager craftManager;
    private AccessoryManager accessoryManager;
    private BuffManager buffManager;

    // 매직 넘버 선언
    private static final double NormalHpRegen = 1; // 체력 재생 (고정값)
    private static final double NormalMpRegen = 3; // 마나 재생 (고정값)

    @Override
    public void onEnable() {
        getLogger().info("RPG Plugin is enabling...");
        // --- 1. 데이터 및 기본 설정 초기화 ---
        Cash.initializeAndLoad(this);
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");
        new PER_DATA();
        StatDataManager.initialize(this);
        StatDataManager.loadAllStats();

        // --- 2. 핵심 매니저(부품) 생성 [수정된 부분] ---
        this.indicatorManager = new IndicatorManager(this);
        this.reforgeManager = new ReforgeManager(this);
        this.itemManager = new ItemManager(this, this.reforgeManager);
        this.itemManager.reloadItems();

        // 1. StatManager와 EffectManager를 먼저 생성합니다.
        this.effectManager = new EffectManager(this);
        this.statManager = new StatManager(this, this.itemManager);
        this.buffManager = new BuffManager(this, this.statManager);
        this.statManager.setBuffManager(this.buffManager);

        // 2. AccessoryManager를 생성하면서 StatManager와 effectManager를 주입합니다.
        this.accessoryManager = new AccessoryManager(this.itemManager, this.statManager, this.effectManager);
        this.statManager = new StatManager(this, this.itemManager);
        this.statManager.setBuffManager(this.buffManager); // <-- StatManager에 BuffManager 연결

        // 3. Setter를 사용해 StatManager에 AccessoryManager 의존성을 주입합니다.
        this.statManager.setAccessoryManager(this.accessoryManager);

        // 4. 나머지 매니저들을 생성합니다.
        this.craftManager = new CraftManager(this, this.itemManager);
        this.mobManager = new MobManager(this, this.itemManager);
        this.levelManager = new LevelManager(this, this.statManager);
        this.enchantmentManager = new EnchantmentManager();

        // --- 3. 콘텐츠 로드/등록 ---
        this.mobManager.registerMobsFromPackage("org.role.rPG.Mob.mobs");
        this.enchantmentManager.registerEnchantmentsFromPackage(this, "org.role.rPG.Enchant.enchants");

        // --- 4. 명령어 관리자(CMD_manager) 등록 ---
        CMD_manager cmdManager = new CMD_manager(this, this.itemManager, this.reforgeManager, this.statManager, this.mobManager, this.levelManager, this.craftManager, this.accessoryManager);
        cmdManager.registerCommands();

        // --- 5. 이벤트 리스너(Listener) 등록 ---
        getServer().getPluginManager().registerEvents(this, this);
        LIS_manager lisManager = new LIS_manager(this);
        lisManager.registerGeneralListeners(statManager, indicatorManager, itemManager, levelManager, mobManager, effectManager, enchantmentManager);

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
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        // 데이터는 이미 메모리에 있으므로 별도의 로딩이 필요 없습니다.
        PER_DATA.getInstance().addPlayer(playerUUID);
        Cash.loadAllPlayerData();
        accessoryManager.loadAccessories(player);
        StatDataManager.loadAllStats();

        statManager.updatePlayerStats(player);
        accessoryManager.updatePassiveEffects(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // --- ▼▼▼ 여기가 수정되었습니다 ▼▼▼ ---
        // 플레이어가 나갈 때 데이터를 파일에 저장만 합니다.
        accessoryManager.saveAccessories(player);
        Cash.saveAllPlayerData();
        StatDataManager.saveAllStats();
        // 메모리에서 데이터를 지우는 unloadPlayerData()를 호출하지 않습니다.
        // Cash.unloadPlayerData(event.getPlayer()); // 이 줄을 제거!
    }

    /**
     * [신규] 플레이어가 부활할 때 호출되는 이벤트 핸들러입니다.
     * 부활 직후 스탯을 다시 적용하여 체력을 정상화합니다.
     */
    @EventHandler
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Bukkit이 플레이어를 부활시킨 다음 틱에 스탯을 적용하여 충돌을 방지합니다.
        new BukkitRunnable() {
            @Override
            public void run() {
                // 1. 플레이어의 모든 스탯(최대 체력 포함)을 다시 계산하고 적용합니다.
                statManager.updatePlayerStats(player);
                // 2. 부활 후 체력을 최대 체력으로 채워줍니다.
                double maxHealth = statManager.getFinalStat(player.getUniqueId(), "MAX_HEALTH");
                player.setHealth(maxHealth);
            }
        }.runTaskLater(this, 1L); // 1틱(약 0.05초) 뒤에 실행
    }

    public void Regeneration() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // [추가] 플레이어가 죽은 상태라면, 이 플레이어에 대한 모든 로직을 건너뜁니다.
                    if (player.isDead()) {
                        continue;
                    }

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
