package org.role.rPG.Item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.role.rPG.RPG;

import java.io.File;
import java.util.*;

/**
 * 아이템 재련(Reforge) 시스템을 관리하는 클래스입니다.
 * 설정 파일에서 재련 접두사(Modifier) 목록을 로드하고,
 * 무작위 접두사를 제공하거나 ID로 특정 접두사를 조회하는 기능을 담당합니다.
 */
public class ReforgeManager {

    private final RPG plugin;
    private FileConfiguration reforgeConfig;
    // [수정] List -> Map 으로 변경하여 ID로 접두사를 찾을 수 있도록 함
    // 접두사의 ID(예: "broken", "strong")를 키로 사용하여 ReforgeModifier 객체를 저장하는 맵입니다.
    private final Map<String, ReforgeModifier> modifierMap = new HashMap<>();
    // 재련 시 소모되는 기본 비용입니다.
    private int reforgeCost = 1000;

    /**
     * ReforgeManager의 생성자입니다.
     * @param plugin 메인 플러그인 인스턴스
     */
    public ReforgeManager(RPG plugin) {
        this.plugin = plugin;
        loadReforges(); // 객체 생성 시 재련 정보를 로드합니다.
    }

    /**
     * 'reforges.yml' 파일에서 재련 접두사 설정 및 비용을 로드합니다.
     */
    private void loadReforges() {
        // 1. 설정 파일 로드 및 생성
        File reforgeFile = new File(plugin.getDataFolder(), "reforges.yml");
        if (!reforgeFile.exists()) {
            // 파일이 없으면 플러그인 JAR 내부의 기본 파일을 저장합니다.
            plugin.saveResource("reforges.yml", false);
        }
        reforgeConfig = YamlConfiguration.loadConfiguration(reforgeFile);

        // 2. 재련 비용 로드
        // 설정에서 'reforge_cost' 값을 읽고, 없으면 기본값 1000을 사용합니다.
        reforgeCost = reforgeConfig.getInt("reforge_cost", 1000);

        // 3. 기존 접두사 맵 초기화
        modifierMap.clear();

        // 4. 'common' 접두사 섹션 로드 (일반적인 재련 접두사를 처리)
        ConfigurationSection prefixesSection = reforgeConfig.getConfigurationSection("prefixes.common");
        if (prefixesSection != null) {
            // 섹션 내의 모든 최상위 키(접두사 ID, 예: 'broken')를 반복합니다.
            for (String key : prefixesSection.getKeys(false)) { // key는 'broken', 'strong' 등
                ConfigurationSection modifierSection = prefixesSection.getConfigurationSection(key);
                if (modifierSection != null) {
                    // 접두사의 표시 이름(name)을 읽어옵니다.
                    String name = modifierSection.getString("name");
                    // 해당 접두사가 부여하는 능력치 변경 값(modifier)을 저장할 맵입니다.
                    Map<String, Double> stats = new HashMap<>();
                    ConfigurationSection statsSection = modifierSection.getConfigurationSection("modifiers");

                    // 능력치 변경 섹션이 있을 경우 처리
                    if (statsSection != null) {
                        // 모든 능력치 키(statKey)를 반복하며 값을 읽습니다.
                        for (String statKey : statsSection.getKeys(false)) {
                            // 능력치 키를 대문자로 변환하여 저장하고, 값을 Double로 읽어옵니다.
                            stats.put(statKey.toUpperCase(), statsSection.getDouble(statKey));
                        }
                    }
                    // [수정] key(ID)와 함께 ReforgeModifier 객체를 Map에 저장
                    // 접두사 ID(key), 이름, 능력치 변경 맵을 사용하여 ReforgeModifier 객체를 생성하고 맵에 추가합니다.
                    modifierMap.put(key, new ReforgeModifier(key, name, stats));
                }
            }
        }
        // 로드된 접두사 개수를 로그에 출력합니다.
        plugin.getLogger().info(modifierMap.size() + "개의 리포지 접두사를 로드했습니다.");
    }

    /**
     * 로드된 접두사 목록 중에서 무작위 ReforgeModifier를 반환합니다.
     * @return 무작위 ReforgeModifier 객체, 맵이 비어있으면 null
     */
    public ReforgeModifier getRandomModifier() {
        if (modifierMap.isEmpty()) return null;
        // Map의 values()를 List로 변환하여 인덱스로 접근할 수 있도록 합니다.
        List<ReforgeModifier> modifiers = new ArrayList<>(modifierMap.values());
        // List 크기 내에서 무작위 인덱스를 선택하여 해당 객체를 반환합니다.
        return modifiers.get(new Random().nextInt(modifiers.size()));
    }

    /**
     * 주어진 ID에 해당하는 ReforgeModifier를 반환합니다.
     * @param id 찾고자 하는 접두사의 ID
     * @return 해당 ID의 ReforgeModifier 객체, 없으면 null
     */
    // [추가] ID로 ReforgeModifier를 가져오는 메소드
    public ReforgeModifier getModifierById(String id) {
        return modifierMap.get(id);
    }

    /**
     * 현재 설정된 재련 비용을 반환합니다.
     * @return 재련 비용
     */
    public int getReforgeCost() {
        return reforgeCost;
    }

    /**
     * 재련 접두사(Modifier)의 상세 정보를 담는 내부 클래스입니다.
     */
    public static class ReforgeModifier {
        private final String id; // ID 필드 추가
        private final String name;
        private final Map<String, Double> statModifiers; // 능력치 변경 맵

        /**
         * ReforgeModifier의 생성자입니다.
         * @param id 접두사의 고유 ID
         * @param name 접두사의 표시 이름
         * @param statModifiers 적용될 능력치 변경 맵
         */
        public ReforgeModifier(String id, String name, Map<String, Double> statModifiers) {
            this.id = id;
            this.name = name;
            this.statModifiers = statModifiers;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public Map<String, Double> getStatModifiers() { return statModifiers; }
    }
}