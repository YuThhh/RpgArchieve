package org.role.rPG.Item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.role.rPG.RPG;

import java.io.File;
import java.util.*;

public class ItemManager {

    private final RPG plugin;
    private final Map<String, ItemStack> customItems = new HashMap<>();
    private FileConfiguration itemConfig;
    private File itemConfigFile;

    public static final NamespacedKey CUSTOM_ITEM_ID_KEY;
    public static final NamespacedKey CUSTOM_ITEM_VERSION_KEY;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    static {
        CUSTOM_ITEM_ID_KEY = new NamespacedKey("rpg", "custom_item_id");
        CUSTOM_ITEM_VERSION_KEY = new NamespacedKey("rpg", "custom_item_version");
    }

    public ItemManager(RPG plugin) {
        this.plugin = plugin;
        createItemConfigFile();
    }

    private void createItemConfigFile() {
        itemConfigFile = new File(plugin.getDataFolder(), "Item.yml");
        if (!itemConfigFile.exists()) {
            plugin.saveResource("Item.yml", false);
        }
        itemConfig = YamlConfiguration.loadConfiguration(itemConfigFile);
    }

    public void reloadItems() {
        itemConfig = YamlConfiguration.loadConfiguration(itemConfigFile);
        customItems.clear();

        ConfigurationSection itemsSection = itemConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Item.yml에서 'items' 섹션을 찾을 수 없습니다.");
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection config = itemsSection.getConfigurationSection(itemId);
            if (config == null) continue;

            try {
                Material material = Material.matchMaterial(config.getString("material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning(itemId + " 아이템의 material을 찾을 수 없습니다: " + config.getString("material"));
                    continue;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    // ...
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    List<String> originalLoreStrings = config.getStringList("lore");
                    for (String loreLine : originalLoreStrings) {
                        String cleanLine = MINI_MESSAGE.stripTags(loreLine);
                        if (cleanLine.startsWith("피해량:")) {
                            try {
                                double value = parseValueFromLore(cleanLine);
                                NamespacedKey key = new NamespacedKey(plugin, itemId + "_attack_damage");
                                AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
                                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
                            } catch (NumberFormatException ignored) {}
                        }
                        // 다른 바닐라 Attribute들도 이런 방식으로 추가 가능
                    }
                    item.setItemMeta(meta);
                }
                customItems.put(itemId, item);
            } catch (Exception e) {
                // 오류 발생 시 더 자세한 정보를 출력하도록 변경
                plugin.getLogger().warning(itemId + " 아이템 로딩 중 오류 발생:");
                e.printStackTrace();
            }
        }
        plugin.getLogger().info(customItems.size() + "개의 커스텀 아이템을 로드했습니다.");
    }

    /**
     * 아이템의 로어를 분석하여 스탯 맵을 반환합니다.
     * @param item 스탯을 추출할 아이템
     * @return 스탯 이름과 값으로 구성된 맵
     */
    public Map<String, Double> getStatsFromItem(ItemStack item) {
        Map<String, Double> stats = new HashMap<>();
        if (item == null || item.getItemMeta() == null || item.getItemMeta().lore() == null) {
            return stats;
        }

        List<Component> lore = item.getItemMeta().lore();
        for (Component lineComponent : Objects.requireNonNull(lore)) {
            String cleanLine = MINI_MESSAGE.stripTags(MINI_MESSAGE.serialize(lineComponent));

            try {
                if (cleanLine.startsWith("피해량:")) {
                    // "피해량"는 Bukkit의 GENERIC_ATTACK_DAMAGE에 해당하므로 별도 처리
                } else if (cleanLine.startsWith("체력:")) {
                    stats.put("MAX_HEALTH", parseValueFromLore(cleanLine));
                } else if (cleanLine.startsWith("방어력:")) {
                    stats.put("DEFENSE", parseValueFromLore(cleanLine));
                } else if (cleanLine.startsWith("힘:")) {
                    stats.put("STRENGTH", parseValueFromLore(cleanLine));
                } else if (cleanLine.startsWith("크리티컬 확률:")) {
                    stats.put("CRIT_CHANCE", parseValueFromLore(cleanLine));
                } else if (cleanLine.startsWith("크리티컬 피해:")) {
                    stats.put("CRIT_DAMAGE", parseValueFromLore(cleanLine));
                }
            } catch (NumberFormatException ignored) {
                // 숫자 파싱 실패 시 무시
            }
        }
        return stats;
    }

    private double parseValueFromLore(String cleanLine) throws NumberFormatException {
        String valueString = cleanLine.substring(cleanLine.indexOf(":") + 1).trim();
        return Double.parseDouble(valueString);
    }

    // 이하 다른 메소드들은 변경 없음
    public boolean updateItemIfNecessary(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING)) return false;

        String itemId = container.get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        int itemVersion = container.getOrDefault(CUSTOM_ITEM_VERSION_KEY, PersistentDataType.INTEGER, 0);

        ItemStack latestItem = customItems.get(itemId);
        if (latestItem == null) return false;

        ItemMeta latestMeta = latestItem.getItemMeta();
        int latestVersion = latestMeta.getPersistentDataContainer().getOrDefault(CUSTOM_ITEM_VERSION_KEY, PersistentDataType.INTEGER, 1);

        if (itemVersion != latestVersion) {
            item.setItemMeta(latestMeta);
            return true;
        }

        return false;
    }

    public ItemStack getItem(String itemId) {
        return customItems.get(itemId) != null ? customItems.get(itemId).clone() : null;
    }

    public Set<String> getAllItemIds() {
        return customItems.keySet();
    }
}