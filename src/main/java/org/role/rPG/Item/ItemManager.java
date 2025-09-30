package org.role.rPG.Item;

import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
import org.role.rPG.Player.StatMapDataType;
import org.role.rPG.RPG;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ItemManager {

    private final RPG plugin;
    private final ReforgeManager reforgeManager;
    private final Map<String, ItemStack> customItems = new HashMap<>();
    private FileConfiguration itemConfig;
    private File itemConfigFile;

    public static final NamespacedKey CUSTOM_ITEM_ID_KEY;
    public static final NamespacedKey CUSTOM_ITEM_TYPE_KEY;
    public static final NamespacedKey CUSTOM_ITEM_GRADE_KEY;
    private static final NamespacedKey BASE_STATS_KEY = new NamespacedKey("rpg", "base_stats");
    private static final NamespacedKey REFORGE_KEY = new NamespacedKey("rpg", "reforge_id");
    private static final NamespacedKey CONFIG_HASH_KEY = new NamespacedKey("rpg", "config_hash");

    private static final Pattern LORE_STAT_PATTERN = Pattern.compile("([^:]+): ([+\\-]?\\d+(\\.\\d+)?)");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    static {
        CUSTOM_ITEM_ID_KEY = new NamespacedKey("rpg", "custom_item_id");
        CUSTOM_ITEM_TYPE_KEY = new NamespacedKey("rpg", "custom_item_type");
        CUSTOM_ITEM_GRADE_KEY = new NamespacedKey("rpg", "custom_item_grade");
    }

    public ItemManager(RPG plugin, ReforgeManager reforgeManager) {
        this.plugin = plugin;
        this.reforgeManager = reforgeManager;
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
                    plugin.getLogger().warning(itemId + " 아이템의 material을 찾을 수 없습니다.");
                    continue;
                }
                ItemStack item = new ItemStack(material);
                ItemMeta meta = buildMetaFromConfig(itemId, config);
                item.setItemMeta(meta);
                customItems.put(itemId, item);
            } catch (Exception e) {
                plugin.getLogger().warning(itemId + " 아이템 로딩 중 오류 발생:");
                e.printStackTrace();
            }
        }
        plugin.getLogger().info(customItems.size() + "개의 커스텀 아이템을 로드했습니다.");
    }

    private ItemMeta buildMetaFromConfig(String itemId, ConfigurationSection config) {
        Material material = Material.matchMaterial(config.getString("material", "STONE"));
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Objects.requireNonNull(material));
        PersistentDataContainer container = meta.getPersistentDataContainer();

        ItemGrade grade = ItemGrade.COMMON;
        try {
            String gradeString = config.getString("grade", "COMMON").toUpperCase();
            grade = ItemGrade.valueOf(gradeString);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(itemId + " 아이템의 grade가 잘못되었습니다. COMMON으로 설정합니다.");
        }
        container.set(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING, grade.name());

        String rawName = config.getString("name", "");
        Component name = MINI_MESSAGE.deserialize(rawName)
                .color(grade.getColor())
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        // [수정] 로어 설정 및 등급 정보 추가
        List<String> originalLoreStrings = config.getStringList("lore");
        List<Component> loreForDisplay = originalLoreStrings.stream()
                .map(line -> MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());

        // 로어가 비어있지 않다면, 등급 표시 전에 한 줄을 띄웁니다.
        if (!loreForDisplay.isEmpty()) {
            loreForDisplay.add(Component.text("")); // 빈 줄 추가
        }

        // MiniMessage를 사용해 색상과 굵은 글씨가 적용된 등급 문자열을 생성합니다.
        String gradeLoreString = String.format("<%s><bold>%s</bold>",
                grade.getColor().asHexString(),
                grade.getDisplayName());

        // 생성된 등급 문자열을 로어 리스트에 추가합니다.
        loreForDisplay.add(MINI_MESSAGE.deserialize(gradeLoreString).decoration(TextDecoration.ITALIC, false));

        meta.lore(loreForDisplay); // 최종 로어 설정


        meta.setUnbreakable(config.getBoolean("unbreakable", false));
        container.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);

        try {
            String typeString = config.getString("type", "MISC").toUpperCase();
            ItemType itemType = ItemType.valueOf(typeString);
            container.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, itemType.name());
        } catch (IllegalArgumentException e) {
            container.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, ItemType.MISC.name());
            plugin.getLogger().warning(itemId + " 아이템의 type이 잘못되었습니다. MISC로 설정합니다.");
        }

        String configString = config.getValues(true).toString();
        int configHash = configString.hashCode();
        container.set(CONFIG_HASH_KEY, PersistentDataType.INTEGER, configHash);

        Map<String, Double> baseStats = new HashMap<>();
        for (String loreLine : originalLoreStrings) {
            String cleanLine = MINI_MESSAGE.stripTags(loreLine);
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);
            if (matcher.find()) {
                String statName = getStatKeyFromName(matcher.group(1));
                if (statName != null) {
                    baseStats.put(statName, Double.parseDouble(matcher.group(2).replace("+", "")));
                }
            }
        }
        container.set(BASE_STATS_KEY, new StatMapDataType(), baseStats);

        return applyAttributesFromLore(meta, itemId, originalLoreStrings);
    }

    // ... (이하 다른 메서드들은 변경하지 않아도 됩니다)
    private ItemMeta applyAttributesFromLore(ItemMeta meta, String itemId, List<String> loreLines) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers != null && !modifiers.isEmpty()) {
            for (Attribute attribute : new ArrayList<>(modifiers.keySet())) {
                meta.removeAttributeModifier(Objects.requireNonNull(attribute));
            }
        }
        for (String loreLine : loreLines) {
            String cleanLine = MINI_MESSAGE.stripTags(loreLine);
            if ("ATTACK_DAMAGE".equals(getStatKeyFromName(cleanLine.split(":")[0]))) {
                try {
                    double value = parseValueFromLore(cleanLine);
                    NamespacedKey key = new NamespacedKey(plugin, itemId + "_attack_damage");
                    AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return meta;
    }

    public void refreshStatsFromLore(ItemStack item) {
        if (isNotCustomItem(item)) return;
        ItemMeta meta = item.getItemMeta();
        String itemId = meta.getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        List<String> loreStrings = new ArrayList<>();
        if (meta.lore() != null) {
            for (Component component : Objects.requireNonNull(meta.lore())) {
                loreStrings.add(MINI_MESSAGE.serialize(component));
            }
        }
        ItemMeta updatedMeta = applyAttributesFromLore(meta, itemId, loreStrings);
        item.setItemMeta(updatedMeta);
    }

    public boolean isNotCustomItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return true;
        return !item.getItemMeta().getPersistentDataContainer().has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public void reforgeItem(ItemStack item, ReforgeManager.ReforgeModifier modifier) {
        if (isNotCustomItem(item)) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Map<String, Double> baseStats = getBaseStats(item);
        String itemId = container.get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        ItemStack templateItem = customItems.get(itemId);
        if (templateItem == null || templateItem.getItemMeta() == null) {
            plugin.getLogger().warning(itemId + "에 대한 아이템 템플릿을 찾을 수 없어 리포지를 중단합니다.");
            return;
        }
        Component originalName = templateItem.getItemMeta().displayName();
        if (originalName == null) {
            originalName = Component.text("이름 없는 아이템");
        }
        Component prefixComponent = Component.text(modifier.getName() + " ").color(NamedTextColor.YELLOW);
        meta.displayName(prefixComponent.append(originalName).decoration(TextDecoration.ITALIC, false));
        container.set(REFORGE_KEY, PersistentDataType.STRING, modifier.getId());
        Map<String, Double> reforgeModifiers = modifier.getStatModifiers();
        List<Component> newLore = new ArrayList<>();
        List<Component> originalLoreComponents = Objects.requireNonNull(templateItem.getItemMeta().lore());
        for (Component loreComponent : originalLoreComponents) {
            String loreLineFormat = MINI_MESSAGE.serialize(loreComponent);
            String cleanLine = MINI_MESSAGE.stripTags(loreLineFormat);
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);
            if (matcher.find()) {
                String statDisplayName = matcher.group(1).trim();
                String statKey = getStatKeyFromName(statDisplayName);
                if (statKey != null) {
                    double baseValue = baseStats.getOrDefault(statKey, 0.0);
                    double reforgeMultiplier = reforgeModifiers.getOrDefault(statKey, 0.0);
                    double finalValue = baseValue * (1 + reforgeMultiplier);
                    double diff = finalValue - baseValue;
                    String newLoreLine = String.format("<i:false><gray>%s: </gray><white>%.0f</white>", statDisplayName, finalValue);
                    if (Math.abs(diff) > 0.01) {
                        newLoreLine += String.format(" <gray>(%s%.0f)</gray>", diff > 0 ? "+" : "", diff);
                    }
                    newLore.add(MINI_MESSAGE.deserialize(newLoreLine));
                } else {
                    newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
                }
            } else {
                newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.lore(newLore);
        item.setItemMeta(meta);
        refreshStatsFromLore(item);
    }

    public boolean updateItemIfNecessary(ItemStack item) {
        if (isNotCustomItem(item)) return false;
        ItemMeta currentMeta = item.getItemMeta();
        String itemId = currentMeta.getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        ItemStack latestItemTemplate = customItems.get(itemId);
        if (latestItemTemplate == null || latestItemTemplate.getItemMeta() == null) return false;
        ItemMeta latestMeta = latestItemTemplate.getItemMeta();
        int currentHash = currentMeta.getPersistentDataContainer().getOrDefault(CONFIG_HASH_KEY, PersistentDataType.INTEGER, 0);
        int latestHash = latestMeta.getPersistentDataContainer().getOrDefault(CONFIG_HASH_KEY, PersistentDataType.INTEGER, 1);
        if (currentHash != latestHash) {
            System.out.println("아이템 업데이트 발견 (" + itemId + "): 해시 불일치. " + currentHash + " -> " + latestHash);
            PersistentDataContainer currentContainer = currentMeta.getPersistentDataContainer();
            PersistentDataContainer latestContainer = latestMeta.getPersistentDataContainer();
            currentContainer.set(CONFIG_HASH_KEY, PersistentDataType.INTEGER, latestHash);
            currentContainer.set(BASE_STATS_KEY, new StatMapDataType(), getBaseStats(latestItemTemplate));
            currentContainer.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, Objects.requireNonNull(latestContainer.get(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING)));
            currentContainer.set(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING, Objects.requireNonNull(latestContainer.get(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING)));
            item.setItemMeta(currentMeta);
            updateLoreAndStats(item);
            return true;
        }
        return false;
    }

    public void updateLoreAndStats(ItemStack item) {
        if (isNotCustomItem(item)) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String itemId = container.get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        Map<String, Double> baseStats = getBaseStats(item);
        String reforgeId = container.get(REFORGE_KEY, PersistentDataType.STRING);
        ReforgeManager.ReforgeModifier modifier = (reforgeId != null) ? reforgeManager.getModifierById(reforgeId) : null;
        ItemStack template = customItems.get(itemId);
        if (template == null) return;
        Component baseName = Objects.requireNonNull(template.getItemMeta()).displayName();
        if (modifier != null) {
            Component prefixComponent = Component.text(modifier.getName() + " ").color(NamedTextColor.YELLOW);
            meta.displayName(prefixComponent.append(Objects.requireNonNull(baseName)).decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(baseName);
        }
        List<Component> originalLoreComponents = Objects.requireNonNull(template.getItemMeta().lore());
        List<Component> newLore = new ArrayList<>();
        for (Component loreComponent : originalLoreComponents) {
            String loreLineFormat = MINI_MESSAGE.serialize(loreComponent);
            String cleanLine = MINI_MESSAGE.stripTags(loreLineFormat);
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);
            if (matcher.find()) {
                String statDisplayName = matcher.group(1).trim();
                String statKey = getStatKeyFromName(statDisplayName);
                if (statKey != null) {
                    double baseValue = baseStats.getOrDefault(statKey, 0.0);
                    double finalValue = baseValue;
                    double diff = 0;
                    if (modifier != null) {
                        double reforgeMultiplier = modifier.getStatModifiers().getOrDefault(statKey, 0.0);
                        finalValue = baseValue * (1 + reforgeMultiplier);
                        diff = finalValue - baseValue;
                    }
                    String newLoreLine = String.format("<i:false><gray>%s: </gray><white>%.0f</white>", statDisplayName, finalValue);
                    if (Math.abs(diff) > 0.01) {
                        newLoreLine += String.format(" <gray>(%s%.0f)</gray>", diff > 0 ? "+" : "", diff);
                    }
                    newLore.add(MINI_MESSAGE.deserialize(newLoreLine));
                } else {
                    newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
                }
            } else {
                newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.lore(newLore);
        item.setItemMeta(meta);
        refreshStatsFromLore(item);
    }

    public Map<String, Double> getBaseStats(ItemStack item) {
        if (isNotCustomItem(item)) return Collections.emptyMap();
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(BASE_STATS_KEY, new StatMapDataType(), new HashMap<>());
    }

    public Map<String, Double> getStatsFromItem(ItemStack item) {
        Map<String, Double> stats = new HashMap<>();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return stats;
        }
        List<Component> lore = item.getItemMeta().lore();
        for (Component lineComponent : Objects.requireNonNull(lore)) {
            String cleanLine = MINI_MESSAGE.stripTags(MINI_MESSAGE.serialize(lineComponent));
            String statKey = getStatKeyFromName(cleanLine.split(":")[0]);
            if (statKey != null) {
                try {
                    Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);
                    if (matcher.find()) {
                        stats.put(statKey, Double.parseDouble(matcher.group(2)));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return stats;
    }

    public double parseValueFromLore(String cleanLine) throws NumberFormatException {
        String valueString = cleanLine.substring(cleanLine.indexOf(":") + 1).trim();
        return Double.parseDouble(valueString.split(" ")[0]);
    }

    public ItemType getItemType(ItemStack item) {
        if (isNotCustomItem(item)) {
            return ItemType.UNKNOWN;
        }
        String typeString = item.getItemMeta().getPersistentDataContainer().get(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING);
        if (typeString == null) {
            return ItemType.UNKNOWN;
        }
        try {
            return ItemType.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            return ItemType.UNKNOWN;
        }
    }

    public ItemGrade getItemGrade(ItemStack item) {
        if (isNotCustomItem(item)) {
            return ItemGrade.COMMON;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        String gradeString = container.get(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING);
        if (gradeString == null) {
            return ItemGrade.COMMON;
        }
        try {
            return ItemGrade.valueOf(gradeString);
        } catch (IllegalArgumentException e) {
            return ItemGrade.COMMON;
        }
    }

    private String getStatKeyFromName(String name) {
        return switch (name.trim()) {
            case "피해량", "공격 피해" -> "ATTACK_DAMAGE";
            case "힘" -> "STRENGTH";
            case "방어력" -> "DEFENSE";
            case "체력", "최대 체력" -> "MAX_HEALTH";
            case "크리티컬 확률" -> "CRIT_CHANCE";
            case "크리티컬 피해" -> "CRIT_DAMAGE";
            case "이동 속도" -> "SPEED";
            case "공격 속도" -> "ATTACK_SPEED";
            case "마나" -> "MAX_MANA";
            default -> null;
        };
    }

    public ItemStack getItem(String itemId) {
        return customItems.get(itemId) != null ? customItems.get(itemId).clone() : null;
    }

    public Set<String> getAllItemIds() {
        return customItems.keySet();
    }
}