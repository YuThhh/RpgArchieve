package org.role.rPG;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StatDataManager {

    private static JavaPlugin plugin;

    /**
     * StatDataManager를 초기화합니다. 메인 클래스의 onEnable에서 호출해야 합니다.
     * @param mainPlugin 메인 플러그인 인스턴스
     */
    public static void initialize(JavaPlugin mainPlugin) {
        plugin = mainPlugin;
    }

    /**
     * [저장] PER_DATA에 있는 모든 플레이어의 스탯 데이터를 config.yml에 저장합니다.
     */
    public static void saveAllStats() {
        if (plugin == null || PER_DATA.getInstance() == null) return;

        PER_DATA perData = PER_DATA.getInstance();
        FileConfiguration config = plugin.getConfig();

        // PER_DATA의 모든 맵에서 UUID를 수집하여 모든 플레이어 목록을 만듭니다.
        Set<UUID> allPlayerUUIDs = new HashSet<>();
        // 각 스탯 맵의 키셋(UUID 목록)을 allPlayerUUIDs에 추가합니다.
        // PER_DATA 클래스에서 실제 사용하는 맵들의 키셋을 추가해야 합니다.
        // 예를 들어, PER_DATA의 playerMaxHealth 필드가 public이거나 getter가 있다면
        // perData.getPlayerMaxHealthMap().keySet() 과 같은 방식으로 접근해야 합니다.
        // 여기서는 get-메서드를 통해 개별 데이터를 가져오는 방식을 사용합니다.
        // 따라서 모든 플레이어 UUID를 얻기 위해선 PER_DATA에 모든 UUID를 반환하는 메서드가 필요하지만,
        // 여기서는 간단하게 각 스탯을 저장하는 방식으로 구현합니다.

        // 'player_stats' 섹션을 새로 시작하기 위해 기존 데이터를 삭제합니다.
        config.set("player_stats", null);

        // 모든 플레이어의 스탯을 저장하기 위해선 접속했던 모든 플레이어 UUID 목록이 필요합니다.
        // Cash 클래스의 playerMoney 맵을 활용하여 UUID 목록을 가져옵니다.
        // (더 나은 방법은 PER_DATA가 모든 유저 UUID를 관리하는 것입니다)
        ConfigurationSection moneySection = config.getConfigurationSection("player_money");
        if (moneySection != null) {
            for (String uuidString : moneySection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String basePath = "player_stats." + uuidString;

                    // 각 스탯을 yml 파일에 저장합니다.
                    config.set(basePath + ".last_ui", perData.getLastUi(uuid));
                    config.set(basePath + ".max_health", perData.getplayerMaxHealth(uuid));
                    config.set(basePath + ".hp_regeneration", perData.getPlayerHpRegenarationBonus(uuid));
                    config.set(basePath + ".defense", perData.getPlayerDefense(uuid));
                    config.set(basePath + ".crit_chance", perData.getPlayerCrit(uuid));
                    config.set(basePath + ".crit_damage", perData.getPlayerCritDamage(uuid));
                    config.set(basePath + ".strength", perData.getPlayerStrength(uuid));
                    config.set(basePath + ".attack_speed", perData.getPlayerAttactSpeed(uuid));
                    config.set(basePath + ".speed", perData.getPlayerSpeed(uuid));
                    config.set(basePath + ".max_mana", perData.getPlayerMaxMana(uuid));
                    config.set(basePath + ".current_mana", perData.getPlayerCurrentMana(uuid));

                    // 참고: 창고(ItemStack[])는 기본적으로 저장할 수 없어 별도의 변환(직렬화) 과정이 필요합니다.
                    // 이 예제에서는 편의상 제외했습니다.

                } catch (IllegalArgumentException e) {
                    // 잘못된 UUID는 무시
                }
            }
        }

        plugin.saveConfig();
        plugin.getLogger().info("플레이어 스탯 데이터가 저장되었습니다.");
    }

    /**
     * [불러오기] config.yml에서 모든 플레이어의 스탯 데이터를 불러와 PER_DATA에 적용합니다.
     */
    public static void loadAllStats() {
        if (plugin == null || PER_DATA.getInstance() == null) return;

        PER_DATA perData = PER_DATA.getInstance();
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection statsSection = config.getConfigurationSection("player_stats");
        if (statsSection == null) {
            plugin.getLogger().info("불러올 스탯 데이터가 없습니다.");
            return;
        }

        for (String uuidString : statsSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String basePath = "player_stats." + uuidString;

                // config.yml에서 스탯 값을 읽어 PER_DATA에 설정합니다.
                perData.setLastUi(uuid, config.getString(basePath + ".last_ui", "none"));
                perData.setplayerMaxHealth(uuid, config.getDouble(basePath + ".max_health", 100.0));
                perData.setPlayerHpRegenarationBonus(uuid, config.getDouble(basePath + ".hp_regeneration", 0.0));
                perData.setPlayerDefense(uuid, config.getDouble(basePath + ".defense", 0.0));
                perData.setPlayerCrit(uuid, config.getDouble(basePath + ".crit_chance", 0.0));
                perData.setPlayerCritDamage(uuid, config.getDouble(basePath + ".crit_damage", 50.0));
                perData.setPlayerStrength(uuid, config.getDouble(basePath + ".strength", 0.0));
                perData.setPlayerAttackSpeed(uuid, config.getDouble(basePath + ".attack_speed", 0.0));
                perData.setPlayerSpeed(uuid, (float) config.getDouble(basePath + ".speed", 100.0));
                perData.setPlayerMaxMana(uuid, config.getDouble(basePath + ".max_mana", 100.0));
                perData.setPlayerCurrentMana(uuid, config.getDouble(basePath + ".current_mana", 100.0));

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("config.yml에서 잘못된 스탯 UUID 형식을 발견했습니다: " + uuidString);
            }
        }
        plugin.getLogger().info("플레이어 스탯 데이터를 불러왔습니다.");
    }
}