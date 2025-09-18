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
import org.role.rPG.RPG;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemManager {

    private final RPG plugin;
    private final Map<String, ItemStack> customItems = new HashMap<>();
    private FileConfiguration itemConfig;
    private File itemConfigFile;

    public static final NamespacedKey CUSTOM_ITEM_ID_KEY;
    private static final NamespacedKey BASE_STATS_KEY = new NamespacedKey("rpg", "base_stats");
    private static final NamespacedKey PREFIX_KEY = new NamespacedKey("rpg", "prefix_name");
    private static final Pattern LORE_STAT_PATTERN = Pattern.compile("([^:]+): ([+\\-]?\\d+(\\.\\d+)?)");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    static {
        CUSTOM_ITEM_ID_KEY = new NamespacedKey("rpg", "custom_item_id");
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

        Component name = MINI_MESSAGE.deserialize(config.getString("name", "")).decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<String> originalLoreStrings = config.getStringList("lore");
        List<Component> loreForDisplay = originalLoreStrings.stream()
                .map(line -> MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
        meta.lore(loreForDisplay);
        meta.setUnbreakable(config.getBoolean("unbreakable", false));

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);

        Map<String, Double> baseStats = new HashMap<>();
        for (String loreLine : originalLoreStrings) {
            String cleanLine = MINI_MESSAGE.stripTags(loreLine);
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);
            if (matcher.find()) {
                String statName = getStatKeyFromName(matcher.group(1));
                if (statName != null) {
                    baseStats.put(statName, Double.parseDouble(matcher.group(2).replace("+","")));
                }
            }
        }
        container.set(BASE_STATS_KEY, new StatMapDataType(), baseStats);

        return applyAttributesFromLore(meta, itemId, originalLoreStrings);
    }

    private ItemMeta applyAttributesFromLore(ItemMeta meta, String itemId, List<String> loreLines) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        for (String loreLine : loreLines) {
            String cleanLine = MINI_MESSAGE.stripTags(loreLine);
            if (cleanLine.startsWith("피해량:")) {
                try {
                    double value = parseValueFromLore(cleanLine);
                    NamespacedKey key = new NamespacedKey(plugin, itemId + "_attack_damage");
                    AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
                } catch (NumberFormatException ignored) {}
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

        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers != null && !modifiers.isEmpty()) {
            for (Attribute attribute : new ArrayList<>(modifiers.keySet())) {
                meta.removeAttributeModifier(Objects.requireNonNull(attribute));
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
        Map<String, Double> baseStats = getBaseStats(item);
        if (baseStats.isEmpty()) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        // [개선] 기존 접두사가 있다면 이름에서 제거
        if (container.has(PREFIX_KEY, PersistentDataType.STRING)) {
            String oldPrefix = container.get(PREFIX_KEY, PersistentDataType.STRING);
            Component oldDisplayName = meta.displayName();
            if (oldDisplayName != null) {
                String nameString = MINI_MESSAGE.serialize(oldDisplayName);
                // 접두사와 뒤따르는 공백을 제거
                nameString = nameString.replace(oldPrefix + " ", "");
                meta.displayName(MINI_MESSAGE.deserialize(nameString));
            }
        }

        // 새 접두사 적용 및 저장
        String newPrefix = modifier.getName();
        Component originalName = meta.displayName();
        meta.displayName(Component.text(newPrefix + " ").color(NamedTextColor.YELLOW).append(Objects.requireNonNull(originalName)));
        container.set(PREFIX_KEY, PersistentDataType.STRING, newPrefix);

        Map<String, Double> reforgeModifiers = modifier.getStatModifiers();
        List<Component> newLore = new ArrayList<>();

        // [버그 수정] 아이템의 현재 로어를 가져옵니다.
        List<String> originalLoreStrings = new ArrayList<>();
        if (meta.lore() != null) {
            Objects.requireNonNull(meta.lore()).forEach(line -> originalLoreStrings.add(MINI_MESSAGE.serialize(line)));
        }

        // [버그 수정] 원본 로어를 기준으로 한 줄씩 확인하여 새 로어를 만듭니다.
        for (String oldLine : originalLoreStrings) {
            String cleanLine = MINI_MESSAGE.stripTags(oldLine);
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);

            if (matcher.find()) { // 이 줄이 스탯 줄이라면
                String statDisplayName = matcher.group(1).trim();
                String statKey = getStatKeyFromName(statDisplayName);

                if (statKey != null && baseStats.containsKey(statKey)) {
                    double baseValue = baseStats.get(statKey);
                    double reforgeMultiplier = reforgeModifiers.getOrDefault(statKey, 0.0);
                    double finalValue = baseValue * (1 + reforgeMultiplier);
                    double diff = finalValue - baseValue;

                    String newLoreLine = String.format("<i:false><gray>%s: </gray><white>%.0f</white>", statDisplayName, finalValue);
                    if (Math.abs(diff) > 0.01) {
                        newLoreLine += String.format(" <gray>(%s%.0f)</gray>", diff > 0 ? "+" : "", diff);
                    }
                    newLore.add(MINI_MESSAGE.deserialize(newLoreLine));
                } else {
                    newLore.add(MINI_MESSAGE.deserialize(oldLine)); // 인식할 수 없는 스탯이면 원본 유지
                }
            } else { // 스탯 줄이 아니면 (설명, 빈 줄 등)
                newLore.add(MINI_MESSAGE.deserialize(oldLine)); // 원본 로어 그대로 유지
            }
        }


        meta.lore(newLore);
        item.setItemMeta(meta);
        refreshStatsFromLore(item);
    }

    // ▼▼▼ [복구됨] Item.yml 파일 변경 감지를 위한 메소드 ▼▼▼
    public boolean updateItemIfNecessary(ItemStack item) {
        if (isNotCustomItem(item)) return false;

        ItemMeta currentMeta = item.getItemMeta();
        String itemId = currentMeta.getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);

        // 서버에 로드된 최신 버전의 아이템 템플릿을 가져옵니다.
        ItemStack latestItem = customItems.get(itemId);
        if (latestItem == null) return false; // yml에서 아이템이 삭제된 경우

        ItemMeta latestMeta = latestItem.getItemMeta();

        // 현재 아이템이 리포지/강화 등으로 변경되었는지 확인합니다.
        // 만약 접두사가 있다면, 이것은 '원본' 아이템이 아니므로 yml 업데이트 대상에서 제외합니다.
        if (currentMeta.getPersistentDataContainer().has(PREFIX_KEY, PersistentDataType.STRING)) {
            return false;
        }

        // 이름, 로어, 속성 등을 비교합니다.
        // 여기서는 간단하게 '원본' 아이템의 로어만 비교하여 변경 여부를 감지합니다.
        // (리포지된 아이템은 로어가 변경되므로 업데이트되지 않습니다.)
        boolean needsUpdate = !Objects.equals(currentMeta.lore(), latestMeta.lore());

        if (needsUpdate) {
            item.setItemMeta(latestMeta); // 아이템 정보를 최신 템플릿으로 덮어씁니다.
            return true;
        }

        return false;
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
                } catch (NumberFormatException ignored) {}
            }
        }
        return stats;
    }

    public double parseValueFromLore(String cleanLine) throws NumberFormatException {
        String valueString = cleanLine.substring(cleanLine.indexOf(":") + 1).trim();
        return Double.parseDouble(valueString.split(" ")[0]); // "(+3)" 같은 부분을 제외하고 첫 숫자만 파싱
    }

    private String getStatKeyFromName(String name) {
        return switch (name.trim()) {
            case "힘" -> "STRENGTH";
            case "방어력" -> "DEFENSE";
            case "체력", "최대 체력" -> "MAX_HEALTH";
            case "크리티컬 확률" -> "CRIT_CHANCE";
            case "크리티컬 피해" -> "CRIT_DAMAGE";
            case "이동 속도" -> "SPEED";
            case "공격 속도" -> "ATTACK_SPEED";
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