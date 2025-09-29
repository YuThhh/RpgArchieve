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
 * **아이템 타입(ItemType)별로 적용 가능한 접두사를 분리하여 관리**합니다.
 */
public class ReforgeManager {

    private final RPG plugin;
    private FileConfiguration reforgeConfig;

    // 접두사의 ID(예: "broken", "strong")를 키로 사용하여 ReforgeModifier 객체를 저장하는 맵입니다.
    // **각 아이템 타입(ItemType)에 맞는, '공통' 접두사까지 모두 합쳐진 최종 맵**입니다.
    private final Map<ItemType, Map<String, ReforgeModifier>> finalModifierMaps = new EnumMap<>(ItemType.class);

    // ID로 모든 접두사를 빠르게 찾기 위한 통합 맵입니다.
    private final Map<String, ReforgeModifier> allModifiersById = new HashMap<>();
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
     * 이 메서드는 공통 접두사와 타입별 전용 접두사를 로드하여 최종 맵을 구성합니다.
     */
    private void loadReforges() {
        // 1. 설정 파일 로드 및 기본 파일 생성
        File reforgeFile = new File(plugin.getDataFolder(), "reforges.yml");
        if (!reforgeFile.exists()) {
            plugin.saveResource("reforges.yml", false);
        }
        reforgeConfig = YamlConfiguration.loadConfiguration(reforgeFile);

        // 2. 재련 비용 로드
        reforgeCost = reforgeConfig.getInt("reforge_cost", 1000);

        // 3. 모든 맵 초기화
        finalModifierMaps.clear();
        allModifiersById.clear();

        // 4. 공통 접두사를 먼저 로드합니다. (모든 아이템 타입에 적용 가능)
        Map<String, ReforgeModifier> commonModifiers = loadModifiersFromSection("prefixes.common");

        // 5. 각 타입별 전용 접두사를 로드하고, 공통 접두사와 합칩니다.
        // 타입별 전용 맵은 '전용 접두사' + '공통 접두사'로 구성됩니다.

        // 근접 무기(MELEE) 접두사 로드 및 공통 접두사 통합
        Map<String, ReforgeModifier> meleeModifiers = loadModifiersFromSection("prefixes.melee");
        meleeModifiers.putAll(commonModifiers); // ✨ 로드 시점에서 한 번만 합칩니다!
        finalModifierMaps.put(ItemType.MELEE, meleeModifiers);

        // 원거리 무기(RANGE) 접두사 로드 및 공통 접두사 통합
        Map<String, ReforgeModifier> rangeModifiers = loadModifiersFromSection("prefixes.range");
        rangeModifiers.putAll(commonModifiers);
        finalModifierMaps.put(ItemType.RANGE, rangeModifiers);

        // 방어구(ARMOR) 접두사 로드 및 공통 접두사 통합
        Map<String, ReforgeModifier> armorModifiers = loadModifiersFromSection("prefixes.armor");
        armorModifiers.putAll(commonModifiers);
        finalModifierMaps.put(ItemType.ARMOR, armorModifiers);

        // 마법/기타(MAGIC) 접두사 로드 및 공통 접두사 통합
        Map<String, ReforgeModifier> magicModifiers = loadModifiersFromSection("prefixes.magic");
        magicModifiers.putAll(commonModifiers);
        finalModifierMaps.put(ItemType.MAGIC, magicModifiers);

        // 6. ID 검색을 위해 모든 접두사를 하나의 맵에 통합합니다.
        // (타입별 전용 접두사를 나중에 putAll하여 공통 접두사보다 우선순위를 가집니다.
        // 즉, ID가 중복될 경우 전용 접두사가 최종 맵에 남습니다.)
        allModifiersById.putAll(commonModifiers);
        allModifiersById.putAll(magicModifiers);
        allModifiersById.putAll(armorModifiers);
        allModifiersById.putAll(rangeModifiers);
        allModifiersById.putAll(meleeModifiers);

        plugin.getLogger().info(allModifiersById.size() + "개의 리포지 접두사를 로드했습니다.");
    }

    /**
     * 설정 파일의 특정 경로에서 접두사 정보를 읽어 Map으로 반환하는 헬퍼 메소드.
     * @param path 설정 파일 내 경로 (예: "prefixes.common")
     * @return 로드된 ReforgeModifier 맵 (ID -> ReforgeModifier)
     */
    private Map<String, ReforgeModifier> loadModifiersFromSection(String path) {
        Map<String, ReforgeModifier> modifiers = new HashMap<>();
        ConfigurationSection section = reforgeConfig.getConfigurationSection(path);
        if (section == null) {
            return modifiers; // 해당 경로가 없으면 빈 맵 반환
        }

        // 해당 섹션의 모든 키(접두사 ID)를 반복합니다.
        for (String key : section.getKeys(false)) {
            ConfigurationSection modSection = section.getConfigurationSection(key);
            if (modSection != null) {
                // 접두사의 표시 이름(name)을 읽습니다.
                String name = modSection.getString("name", "이름 없음");
                Map<String, Double> stats = new HashMap<>();
                ConfigurationSection statsSection = modSection.getConfigurationSection("modifiers");

                // 능력치 변경 섹션(modifiers)이 있을 경우 처리
                if (statsSection != null) {
                    // 모든 능력치 키(statKey)를 반복하며 값을 읽습니다.
                    for (String statKey : statsSection.getKeys(false)) {
                        // 능력치 키를 대문자로 변환하고 값을 Double로 읽어 저장합니다.
                        stats.put(statKey.toUpperCase(), statsSection.getDouble(statKey));
                    }
                }
                // 접두사 ID, 이름, 능력치 변경 맵을 담은 ReforgeModifier 객체를 생성하고 맵에 저장합니다.
                modifiers.put(key, new ReforgeModifier(key, name, stats));
            }
        }
        return modifiers;
    }

    /**
     * 주어진 아이템 타입에 적용 가능한 무작위 ReforgeModifier를 반환합니다.
     * @param itemType 아이템의 타입 (ItemType.MELEE, ItemType.ARMOR 등)
     * @return 무작위 ReforgeModifier 객체, 해당 타입의 접두사가 없으면 null
     */
    public ReforgeModifier getRandomModifier(ItemType itemType) {
        // 아이템 타입에 해당하는 접두사 맵을 가져옵니다.
        Map<String, ReforgeModifier> applicableModifiers = finalModifierMaps.get(itemType);

        if (applicableModifiers == null || applicableModifiers.isEmpty()) {
            return null; // 해당 타입에 적용 가능한 접두사가 없으면 null 반환
        }

        // 맵의 값들(ReforgeModifier 객체들)을 리스트로 변환합니다.
        List<ReforgeModifier> modifierList = new ArrayList<>(applicableModifiers.values());
        // 리스트에서 무작위 인덱스의 객체를 선택하여 반환합니다.
        return modifierList.get(new Random().nextInt(modifierList.size()));
    }

    /**
     * 주어진 ID에 해당하는 ReforgeModifier를 반환합니다.
     * @param id 찾고자 하는 접두사의 ID
     * @return 해당 ID의 ReforgeModifier 객체, 없으면 null
     */
    public ReforgeModifier getModifierById(String id) {
        // ✨ 이제 통합 맵(allModifiersById)에서 한 번에 찾을 수 있어 매우 효율적입니다.
        return allModifiersById.get(id);
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
        private final String id; // 접두사의 고유 ID
        private final String name; // 접두사의 표시 이름
        private final Map<String, Double> statModifiers; // 능력치 변경 맵 (능력치 키 -> 배율)

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