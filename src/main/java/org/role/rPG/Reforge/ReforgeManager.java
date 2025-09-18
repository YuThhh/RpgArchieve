package org.role.rPG.Reforge;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.role.rPG.RPG;

import java.io.File;
import java.util.*;

public class ReforgeManager {

    private final RPG plugin;
    private FileConfiguration reforgeConfig;
    // [수정] List -> Map 으로 변경하여 ID로 접두사를 찾을 수 있도록 함
    private final Map<String, ReforgeModifier> modifierMap = new HashMap<>();
    private int reforgeCost = 1000;

    public ReforgeManager(RPG plugin) {
        this.plugin = plugin;
        loadReforges();
    }

    private void loadReforges() {
        File reforgeFile = new File(plugin.getDataFolder(), "reforges.yml");
        if (!reforgeFile.exists()) {
            plugin.saveResource("reforges.yml", false);
        }
        reforgeConfig = YamlConfiguration.loadConfiguration(reforgeFile);

        reforgeCost = reforgeConfig.getInt("reforge_cost", 1000);
        modifierMap.clear();

        ConfigurationSection prefixesSection = reforgeConfig.getConfigurationSection("prefixes.common");
        if (prefixesSection != null) {
            for (String key : prefixesSection.getKeys(false)) { // key는 'broken', 'strong' 등
                ConfigurationSection modifierSection = prefixesSection.getConfigurationSection(key);
                if (modifierSection != null) {
                    String name = modifierSection.getString("name");
                    Map<String, Double> stats = new HashMap<>();
                    ConfigurationSection statsSection = modifierSection.getConfigurationSection("modifiers");
                    if (statsSection != null) {
                        for (String statKey : statsSection.getKeys(false)) {
                            stats.put(statKey.toUpperCase(), statsSection.getDouble(statKey));
                        }
                    }
                    // [수정] key(ID)와 함께 ReforgeModifier 객체를 Map에 저장
                    modifierMap.put(key, new ReforgeModifier(key, name, stats));
                }
            }
        }
        plugin.getLogger().info(modifierMap.size() + "개의 리포지 접두사를 로드했습니다.");
    }

    public ReforgeModifier getRandomModifier() {
        if (modifierMap.isEmpty()) return null;
        List<ReforgeModifier> modifiers = new ArrayList<>(modifierMap.values());
        return modifiers.get(new Random().nextInt(modifiers.size()));
    }

    // [추가] ID로 ReforgeModifier를 가져오는 메소드
    public ReforgeModifier getModifierById(String id) {
        return modifierMap.get(id);
    }

    public int getReforgeCost() {
        return reforgeCost;
    }

    public static class ReforgeModifier {
        private final String id; // ID 필드 추가
        private final String name;
        private final Map<String, Double> statModifiers;

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