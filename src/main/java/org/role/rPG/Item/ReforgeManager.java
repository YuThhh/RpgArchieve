package org.role.rPG.Item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.role.rPG.RPG;

import java.io.File;
import java.util.*;

public class ReforgeManager {

    private final RPG plugin;
    private FileConfiguration reforgeConfig;
    private final List<ReforgeModifier> availableModifiers = new ArrayList<>();
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
        availableModifiers.clear();

        ConfigurationSection prefixesSection = reforgeConfig.getConfigurationSection("prefixes.common");
        if (prefixesSection != null) {
            for (String key : prefixesSection.getKeys(false)) {
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
                    availableModifiers.add(new ReforgeModifier(name, stats));
                }
            }
        }
        plugin.getLogger().info(availableModifiers.size() + "개의 리포지 접두사를 로드했습니다.");
    }

    public ReforgeModifier getRandomModifier() {
        if (availableModifiers.isEmpty()) return null;
        return availableModifiers.get(new Random().nextInt(availableModifiers.size()));
    }

    public int getReforgeCost() {
        return reforgeCost;
    }

    // 리포지 정보를 담는 내부 데이터 클래스
    public static class ReforgeModifier {
        private final String name;
        private final Map<String, Double> statModifiers;

        public ReforgeModifier(String name, Map<String, Double> statModifiers) {
            this.name = name;
            this.statModifiers = statModifiers;
        }

        public String getName() {
            return name;
        }

        public Map<String, Double> getStatModifiers() {
            return statModifiers;
        }
    }
}