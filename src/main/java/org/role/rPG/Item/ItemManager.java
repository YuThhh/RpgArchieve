package org.role.rPG.Item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.role.rPG.RPG;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

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

    // ▼▼▼ [수정됨] 누락된 로직이 모두 추가된 메소드 ▼▼▼
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
                    // [복구] 아이템 이름 설정
                    Component name = MINI_MESSAGE.deserialize(config.getString("name", ""))
                            .decoration(TextDecoration.ITALIC, false);
                    meta.displayName(name);

                    // [복구] 아이템 로어 설정
                    List<String> originalLoreStrings = config.getStringList("lore");
                    List<Component> loreForDisplay = originalLoreStrings.stream()
                            .map(line -> MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false))
                            .collect(Collectors.toList());
                    meta.lore(loreForDisplay);

                    // [복구] 파괴 불가 설정
                    meta.setUnbreakable(config.getBoolean("unbreakable", false));

                    // [복구] 커스텀 아이템 ID 태그 저장 (자동 업데이트 및 스탯 적용에 필수)
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    container.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);

                    // 로어를 분석하여 Minecraft 기본 속성(AttributeModifier) 적용
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    for (String loreLine : originalLoreStrings) {
                        String cleanLine = MINI_MESSAGE.stripTags(loreLine);
                        // [수정] Attribute.ATTACK_DAMAGE -> Attribute.GENERIC_ATTACK_DAMAGE
                        if (cleanLine.startsWith("공격 피해:")) {
                            try {
                                double value = parseValueFromLore(cleanLine);
                                NamespacedKey key = new NamespacedKey(plugin, itemId + "_attack_damage");
                                AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
                                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
                            } catch (NumberFormatException ignored) {}
                        }
                        // 여기에 '방어구 강도' 등 다른 바닐라 속성 로직 추가 가능
                    }

                    item.setItemMeta(meta);
                }
                customItems.put(itemId, item);
            } catch (Exception e) {
                plugin.getLogger().warning(itemId + " 아이템 로딩 중 오류 발생:");
                e.printStackTrace();
            }
        }
        plugin.getLogger().info(customItems.size() + "개의 커스텀 아이템을 로드했습니다.");
    }
    // ▲▲▲ 메소드 수정 완료 ▲▲▲

    public Map<String, Double> getStatsFromItem(ItemStack item) {
        Map<String, Double> stats = new HashMap<>();
        if (item == null || item.getItemMeta() == null || item.getItemMeta().lore() == null) {
            return stats;
        }

        List<Component> lore = item.getItemMeta().lore();
        for (Component lineComponent : Objects.requireNonNull(lore)) {
            String cleanLine = MINI_MESSAGE.stripTags(MINI_MESSAGE.serialize(lineComponent));

            try {
                if (cleanLine.startsWith("공격 피해:")) {
                    // "공격 피해"는 AttributeModifier로 직접 처리되므로 여기서는 제외합니다.
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
                } else if (cleanLine.startsWith("이동 속도:")) {
                    stats.put("SPEED", parseValueFromLore(cleanLine));
                }else if (cleanLine.startsWith("공격 속도:")) {
                    stats.put("ATTACK_SPEED", parseValueFromLore(cleanLine));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return stats;
    }

    private double parseValueFromLore(String cleanLine) throws NumberFormatException {
        String valueString = cleanLine.substring(cleanLine.indexOf(":") + 1).trim();
        return Double.parseDouble(valueString);
    }

    public boolean updateItemIfNecessary(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        ItemMeta currentMeta = item.getItemMeta();
        PersistentDataContainer container = currentMeta.getPersistentDataContainer();
        if (!container.has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING)) return false;
        String itemId = container.get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        ItemStack latestItem = customItems.get(itemId);
        if (latestItem == null) return false;
        ItemMeta latestMeta = latestItem.getItemMeta();

        boolean needsUpdate = !Objects.equals(currentMeta.displayName(), latestMeta.displayName()) ||
                !Objects.equals(currentMeta.lore(), latestMeta.lore()) ||
                !Objects.equals(currentMeta.getAttributeModifiers(), latestMeta.getAttributeModifiers()) ||
                currentMeta.isUnbreakable() != latestMeta.isUnbreakable();

        if (needsUpdate) {
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