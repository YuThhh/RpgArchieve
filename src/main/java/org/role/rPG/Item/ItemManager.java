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

/**
 * ItemManager 클래스는 플러그인의 커스텀 아이템 설정 파일(Item.yml)을 로드하고,
 * 메모리에서 아이템 템플릿을 관리하며, 아이템 생성, 재련(Reforge), 업데이트 등의
 * 아이템 관련 핵심 로직을 처리합니다.
 */
public class ItemManager {

    private final RPG plugin; // 메인 플러그인 인스턴스
    private final ReforgeManager reforgeManager; // 리포지(재련) 관련 로직을 담당하는 매니저
    private final Map<String, ItemStack> customItems = new HashMap<>(); // 커스텀 아이템 ID와 템플릿 ItemStack을 저장하는 맵
    private FileConfiguration itemConfig; // Item.yml 설정 파일의 내용을 담는 객체
    private File itemConfigFile; // Item.yml 파일 객체

    // === PersistentDataContainer(PDC)에 사용될 고유 키(NamespacedKey) 정의 ===
    // PDC는 아이템에 영구적인 커스텀 데이터를 저장하는 데 사용됩니다.
    public static final NamespacedKey CUSTOM_ITEM_ID_KEY; // 커스텀 아이템의 고유 ID (예: "SWORD_OF_FIRE")
    public static final NamespacedKey CUSTOM_ITEM_TYPE_KEY; // 커스텀 아이템의 타입 (예: "WEAPON", "ARMOR")
    public static final NamespacedKey CUSTOM_ITEM_GRADE_KEY; // **[추가]** 커스텀 아이템의 등급 (예: "LEGENDARY")
    private static final NamespacedKey BASE_STATS_KEY = new NamespacedKey("rpg", "base_stats"); // 아이템의 순수 기본 능력치 맵
    private static final NamespacedKey REFORGE_KEY = new NamespacedKey("rpg", "reforge_id"); // 아이템에 적용된 재련(Reforge) ID
    private static final NamespacedKey CONFIG_HASH_KEY = new NamespacedKey("rpg", "config_hash"); // 설정 변경 여부를 확인하기 위한 해시 값

    // === 정규 표현식 및 MiniMessage 인스턴스 정의 ===
    // 아이템 설명(Lore)에서 능력치 정보(예: "힘: +10.5")를 파싱하기 위한 정규식
    private static final Pattern LORE_STAT_PATTERN = Pattern.compile("([^:]+): ([+\\-]?\\d+(\\.\\d+)?)");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage(); // MiniMessage는 색상 코드를 포함한 텍스트를 Component로 변환

    static {
        // NamespacedKey는 static 블록에서 초기화
        CUSTOM_ITEM_ID_KEY = new NamespacedKey("rpg", "custom_item_id");
        CUSTOM_ITEM_TYPE_KEY = new NamespacedKey("rpg", "custom_item_type");
        CUSTOM_ITEM_GRADE_KEY = new NamespacedKey("rpg", "custom_item_grade"); // **[추가]** 등급 PDC 키 초기화
    }

    /**
     * ItemManager의 생성자입니다.
     * @param plugin 메인 플러그인 인스턴스
     * @param reforgeManager ReforgeManager 인스턴스
     */
    public ItemManager(RPG plugin, ReforgeManager reforgeManager) {
        this.plugin = plugin;
        this.reforgeManager = reforgeManager;
        createItemConfigFile(); // Item.yml 파일 생성 및 로드
    }

    /**
     * Item.yml 설정 파일이 존재하지 않으면 리소스로부터 복사하고, 파일로부터 설정을 로드합니다.
     */
    private void createItemConfigFile() {
        itemConfigFile = new File(plugin.getDataFolder(), "Item.yml");
        if (!itemConfigFile.exists()) {
            // 플러그인 폴더에 파일이 없으면 JAR 내부에 있는 리소스 파일을 복사합니다.
            plugin.saveResource("Item.yml", false);
        }
        // 파일로부터 설정을 로드합니다.
        itemConfig = YamlConfiguration.loadConfiguration(itemConfigFile);
    }

    /**
     * Item.yml 파일을 다시 로드하고 메모리의 커스텀 아이템 데이터를 갱신합니다.
     */
    public void reloadItems() {
        // 1. 설정 파일 다시 불러오기
        itemConfig = YamlConfiguration.loadConfiguration(itemConfigFile);
        // 2. 기존 아이템 데이터 초기화
        customItems.clear();
        // 3. 'items' 섹션 가져오기
        ConfigurationSection itemsSection = itemConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Item.yml에서 'items' 섹션을 찾을 수 없습니다.");
            return;
        }

        // 4. 각 아이템 ID를 순회하며 아이템 생성
        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection config = itemsSection.getConfigurationSection(itemId);
            if (config == null) continue;
            try {
                // Material 정보 파싱
                Material material = Material.matchMaterial(config.getString("material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning(itemId + " 아이템의 material을 찾을 수 없습니다.");
                    continue;
                }
                // ItemStack 생성 및 ItemMeta 설정 로직 호출
                ItemStack item = new ItemStack(material);
                ItemMeta meta = buildMetaFromConfig(itemId, config);
                item.setItemMeta(meta);
                // 메모리에 아이템 템플릿 저장
                customItems.put(itemId, item);
            } catch (Exception e) {
                plugin.getLogger().warning(itemId + " 아이템 로딩 중 오류 발생:");
                e.printStackTrace();
            }
        }
        plugin.getLogger().info(customItems.size() + "개의 커스텀 아이템을 로드했습니다.");
    }

    /**
     * 설정(ConfigurationSection)을 기반으로 아이템의 ItemMeta를 구성합니다.
     * 이 메서드는 아이템 이름, 로어, PDC 데이터, 기본 능력치 파싱 등을 처리합니다.
     * @param itemId 아이템의 고유 ID
     * @param config 아이템의 설정 섹션
     * @return 설정된 ItemMeta 객체
     */
    private ItemMeta buildMetaFromConfig(String itemId, ConfigurationSection config) {
        Material material = Material.matchMaterial(config.getString("material", "STONE"));
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Objects.requireNonNull(material));
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // **[추가]** 1. 아이템 등급(Grade) 파싱 및 PDC 저장
        ItemGrade grade = ItemGrade.COMMON;
        try {
            String gradeString = config.getString("grade", "COMMON").toUpperCase();
            grade = ItemGrade.valueOf(gradeString);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(itemId + " 아이템의 grade가 잘못되었습니다. COMMON으로 설정합니다.");
        }
        container.set(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING, grade.name());

        // 2. 아이템 이름(Display Name) 설정
        // **[수정]** 등급에 맞는 색상을 이름에 적용합니다. (MiniMessage로 변환 후 색상 및 이탤릭체 제거)
        String rawName = config.getString("name", "");
        Component name = MINI_MESSAGE.deserialize(rawName)
                .color(grade.getColor()) // 등급 색상 적용
                .decoration(TextDecoration.ITALIC, false); // 이탤릭체 제거
        meta.displayName(name);

        // **[수정]** 3. 아이템 설명(Lore) 설정 및 등급 정보 추가
        List<String> originalLoreStrings = config.getStringList("lore");
        // 설정된 로어를 Component로 변환하고 이탤릭체를 제거합니다.
        List<Component> loreForDisplay = originalLoreStrings.stream()
                .map(line -> MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());

        // 로어가 비어있지 않다면, 등급 표시 전에 한 줄을 띄웁니다.
        if (!loreForDisplay.isEmpty()) {
            loreForDisplay.add(Component.text("")); // 빈 줄 추가
        }

        // MiniMessage를 사용해 색상과 굵은 글씨가 적용된 등급 문자열을 생성합니다.
        // ItemGrade의 색상(Hex)과 표시 이름(DisplayName)을 사용합니다.
        String gradeLoreString = String.format("<%s><bold>%s</bold>",
                grade.getColor().asHexString(),
                grade.getDisplayName());

        // 최종 로어 리스트에 **등급 표시 컴포넌트**를 추가합니다.
        loreForDisplay.add(MINI_MESSAGE.deserialize(gradeLoreString).decoration(TextDecoration.ITALIC, false));

        meta.lore(loreForDisplay); // 최종 로어 설정

        // 4. 기타 ItemMeta 설정
        meta.setUnbreakable(config.getBoolean("unbreakable", false));
        container.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);

        // 5. 아이템 타입(ItemType) 설정 및 PDC 저장
        try {
            String typeString = config.getString("type", "MISC").toUpperCase();
            ItemType itemType = ItemType.valueOf(typeString);
            container.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, itemType.name());
        } catch (IllegalArgumentException e) {
            container.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, ItemType.MISC.name());
            plugin.getLogger().warning(itemId + " 아이템의 type이 잘못되었습니다. MISC로 설정합니다.");
        }

        // 6. 설정 해시 값(데이터 지문) 저장
        String configString = config.getValues(true).toString();
        int configHash = configString.hashCode();
        container.set(CONFIG_HASH_KEY, PersistentDataType.INTEGER, configHash);

        // 7. 아이템의 기본 능력치(Base Stats) 파싱 및 저장
        Map<String, Double> baseStats = new HashMap<>();
        // 원본 로어 문자열 리스트를 순회하며 능력치 정보를 추출합니다.
        for (String loreLine : originalLoreStrings) {
            String cleanLine = MINI_MESSAGE.stripTags(loreLine); // MiniMessage 태그 제거
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine); // 능력치 패턴 매칭
            if (matcher.find()) {
                String statName = getStatKeyFromName(matcher.group(1)); // 표시 이름 -> 능력치 키 변환
                if (statName != null) {
                    // 능력치 값에서 "+"를 제거하고 Double로 변환하여 맵에 저장
                    baseStats.put(statName, Double.parseDouble(matcher.group(2).replace("+", "")));
                }
            }
        }
        // 파싱된 기본 능력치 맵을 커스텀 데이터 타입으로 PDC에 저장
        container.set(BASE_STATS_KEY, new StatMapDataType(), baseStats);

        // 8. 아이템 속성(Attribute) 적용 및 ItemMeta 반환
        return applyAttributesFromLore(meta, itemId, originalLoreStrings);
    }

    /**
     * 아이템의 로어(Lore)에서 특정 능력치를 파싱하여 Minecraft의 기본 아이템 속성(Attribute)으로 적용합니다.
     * @param meta 현재 ItemMeta
     * @param itemId 아이템 ID
     * @param loreLines 원본 로어 문자열 리스트 (파싱용)
     * @return 속성이 적용된 ItemMeta
     */
    private ItemMeta applyAttributesFromLore(ItemMeta meta, String itemId, List<String> loreLines) {
        // 기본 속성 설명 표시를 숨깁니다.
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // 기존에 적용된 모든 AttributeModifier를 제거합니다.
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers != null && !modifiers.isEmpty()) {
            for (Attribute attribute : new ArrayList<>(modifiers.keySet())) {
                meta.removeAttributeModifier(Objects.requireNonNull(attribute));
            }
        }

        // 로어 라인을 순회하며 속성을 파싱하고 적용합니다. (주로 ATTACK_DAMAGE)
        for (String loreLine : loreLines) {
            String cleanLine = MINI_MESSAGE.stripTags(loreLine);
            if ("ATTACK_DAMAGE".equals(getStatKeyFromName(cleanLine.split(":")[0]))) {
                try {
                    // 로어에서 능력치 값을 파싱합니다.
                    double value = parseValueFromLore(cleanLine);
                    NamespacedKey key = new NamespacedKey(plugin, itemId + "_attack_damage");
                    // 공격 피해 속성 수정자(AttributeModifier)를 생성 및 추가
                    AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return meta;
    }

    /**
     * 아이템의 로어에 변경이 생겼거나 능력치 갱신이 필요할 때,
     * 로어를 기반으로 Minecraft Attribute를 새로고침합니다.
     * @param item 갱신할 ItemStack
     */
    public void refreshStatsFromLore(ItemStack item) {
        if (isNotCustomItem(item)) return;
        ItemMeta meta = item.getItemMeta();
        String itemId = meta.getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        // ItemMeta의 Component 로어를 문자열 리스트로 다시 직렬화합니다.
        List<String> loreStrings = new ArrayList<>();
        if (meta.lore() != null) {
            for (Component component : Objects.requireNonNull(meta.lore())) {
                loreStrings.add(MINI_MESSAGE.serialize(component));
            }
        }
        // 직렬화된 로어 문자열을 사용하여 Attribute를 다시 적용합니다.
        ItemMeta updatedMeta = applyAttributesFromLore(meta, itemId, loreStrings);
        item.setItemMeta(updatedMeta);
    }

    /**
     * 주어진 ItemStack이 커스텀 아이템인지 확인합니다.
     * @param item 확인할 ItemStack
     * @return 커스텀 아이템이 아니면 true, 맞으면 false
     */
    public boolean isNotCustomItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return true;
        return !item.getItemMeta().getPersistentDataContainer().has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * 아이템에 재련(Reforge) 효과를 적용하고 아이템의 이름과 로어를 업데이트합니다.
     * @param item 재련할 ItemStack
     * @param modifier 적용할 재련 수정자 정보
     */
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

        // 1. 이름 업데이트: 재련 접두사를 붙입니다.
        Component originalName = templateItem.getItemMeta().displayName();
        if (originalName == null) originalName = Component.text("이름 없는 아이템");
        Component prefixComponent = Component.text(modifier.getName() + " ").color(NamedTextColor.YELLOW);
        meta.displayName(prefixComponent.append(originalName).decoration(TextDecoration.ITALIC, false));

        // 2. 재련 정보(ID) 저장
        container.set(REFORGE_KEY, PersistentDataType.STRING, modifier.getId());

        // 3. 새로운 로어(Lore) 생성 (재련 스탯 적용)
        Map<String, Double> reforgeModifiers = modifier.getStatModifiers();
        List<Component> newLore = new ArrayList<>();
        // 템플릿의 로어(등급 표시 전까지의 기본 스탯 로어)를 가져옵니다.
        List<Component> originalLoreComponents = Objects.requireNonNull(templateItem.getItemMeta().lore());

        for (Component loreComponent : originalLoreComponents) {
            String loreLineFormat = MINI_MESSAGE.serialize(loreComponent);
            String cleanLine = MINI_MESSAGE.stripTags(loreLineFormat);
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);

            if (matcher.find()) { // 현재 로어 줄이 능력치 라인인 경우
                String statDisplayName = matcher.group(1).trim();
                String statKey = getStatKeyFromName(statDisplayName);

                if (statKey != null) {
                    double baseValue = baseStats.getOrDefault(statKey, 0.0);
                    double reforgeMultiplier = reforgeModifiers.getOrDefault(statKey, 0.0);
                    // 최종 능력치 값 계산: Final = Base * (1 + Multiplier)
                    double finalValue = baseValue * (1 + reforgeMultiplier);
                    double diff = finalValue - baseValue;

                    // 새로운 능력치 로어 라인 포맷팅 (예: "공격력: 10 (+2)")
                    String newLoreLine = String.format("<i:false><gray>%s: </gray><white>%.0f</white>", statDisplayName, finalValue);
                    if (Math.abs(diff) > 0.01) {
                        newLoreLine += String.format(" <gray>(%s%.0f)</gray>", diff > 0 ? "+" : "", diff);
                    }
                    newLore.add(MINI_MESSAGE.deserialize(newLoreLine));
                } else {
                    // 유효한 능력치 키가 없는 경우, 원본 로어만 추가 (등급 표시 등)
                    newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
                }
            } else {
                // 능력치 패턴을 찾지 못한 일반 로어 라인인 경우
                newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
            }
        }

        // 4. ItemMeta 업데이트 및 최종 능력치 새로고침
        meta.lore(newLore);
        item.setItemMeta(meta);
        refreshStatsFromLore(item); // Attribute 등 최종 능력치를 로어 기반으로 갱신
    }

    /**
     * 플레이어가 가진 아이템의 설정이 Item.yml에서 변경되었는지 확인하고,
     * 해시 값이 다르면 아이템의 PDC 데이터(해시, 기본 스탯, 타입, 등급)를 업데이트합니다.
     * @param item 업데이트를 확인할 ItemStack
     * @return 업데이트가 발생했으면 true, 아니면 false
     */
    public boolean updateItemIfNecessary(ItemStack item) {
        if (isNotCustomItem(item)) return false;
        ItemMeta currentMeta = item.getItemMeta();
        String itemId = currentMeta.getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        ItemStack latestItemTemplate = customItems.get(itemId);
        if (latestItemTemplate == null || latestItemTemplate.getItemMeta() == null) return false;
        ItemMeta latestMeta = latestItemTemplate.getItemMeta();

        // 1. 해시 값을 비교하여 설정 변경 여부 확인
        int currentHash = currentMeta.getPersistentDataContainer().getOrDefault(CONFIG_HASH_KEY, PersistentDataType.INTEGER, 0);
        int latestHash = latestMeta.getPersistentDataContainer().getOrDefault(CONFIG_HASH_KEY, PersistentDataType.INTEGER, 1);

        if (currentHash != latestHash) {
            System.out.println("아이템 업데이트 발견 (" + itemId + "): 해시 불일치. " + currentHash + " -> " + latestHash);

            PersistentDataContainer currentContainer = currentMeta.getPersistentDataContainer();
            PersistentDataContainer latestContainer = latestMeta.getPersistentDataContainer();

            // 2. PDC 데이터 업데이트
            currentContainer.set(CONFIG_HASH_KEY, PersistentDataType.INTEGER, latestHash);
            currentContainer.set(BASE_STATS_KEY, new StatMapDataType(), getBaseStats(latestItemTemplate));
            currentContainer.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, Objects.requireNonNull(latestContainer.get(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING)));
            // **[추가]** 등급 정보 업데이트
            currentContainer.set(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING, Objects.requireNonNull(latestContainer.get(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING)));

            item.setItemMeta(currentMeta); // 변경된 모든 PDC 데이터를 저장
            updateLoreAndStats(item); // 최종적으로 로어와 모든 스탯을 새로고침 (재련 정보가 있다면 유지)
            return true;
        }
        return false;
    }

    /**
     * 아이템의 현재 재련 상태를 유지하면서 로어와 최종 능력치(Attributes)를 갱신합니다.
     * (updateItemIfNecessary 또는 기타 이유로 호출됨)
     * @param item 갱신할 ItemStack
     */
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

        // 1. 이름 갱신: 재련이 있으면 접두사를 붙이고, 없으면 기본 이름을 사용합니다.
        if (modifier != null) {
            Component prefixComponent = Component.text(modifier.getName() + " ").color(NamedTextColor.YELLOW);
            // **[수정]** 템플릿의 baseName에 등급 색상이 이미 적용되어 있으므로 그대로 사용
            meta.displayName(prefixComponent.append(Objects.requireNonNull(baseName)).decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(baseName);
        }

        // 2. 로어 갱신
        List<Component> originalLoreComponents = Objects.requireNonNull(template.getItemMeta().lore());
        List<Component> newLore = new ArrayList<>();

        for (Component loreComponent : originalLoreComponents) {
            String loreLineFormat = MINI_MESSAGE.serialize(loreComponent);
            String cleanLine = MINI_MESSAGE.stripTags(loreLineFormat);
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);

            if (matcher.find()) { // 능력치 라인인 경우
                String statDisplayName = matcher.group(1).trim();
                String statKey = getStatKeyFromName(statDisplayName);

                if (statKey != null) {
                    double baseValue = baseStats.getOrDefault(statKey, 0.0);
                    double finalValue = baseValue;
                    double diff = 0;

                    // 재련이 적용된 경우에만 최종 능력치와 차이 값을 계산합니다.
                    if (modifier != null) {
                        double reforgeMultiplier = modifier.getStatModifiers().getOrDefault(statKey, 0.0);
                        finalValue = baseValue * (1 + reforgeMultiplier);
                        diff = finalValue - baseValue;
                    }
                    // 로어 라인 포맷팅 및 추가
                    String newLoreLine = String.format("<i:false><gray>%s: </gray><white>%.0f</white>", statDisplayName, finalValue);
                    if (Math.abs(diff) > 0.01) {
                        newLoreLine += String.format(" <gray>(%s%.0f)</gray>", diff > 0 ? "+" : "", diff);
                    }
                    newLore.add(MINI_MESSAGE.deserialize(newLoreLine));
                } else {
                    newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
                }
            } else { // 일반 로어 라인 (등급 표시 포함)
                newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
            }
        }

        meta.lore(newLore);
        item.setItemMeta(meta);
        refreshStatsFromLore(item); // Minecraft Attribute 갱신
    }

    /**
     * 아이템의 PersistentDataContainer에 저장된 기본 능력치 맵을 가져옵니다.
     * @param item 기본 능력치를 확인할 ItemStack
     * @return 기본 능력치 맵 (없으면 빈 맵)
     */
    public Map<String, Double> getBaseStats(ItemStack item) {
        if (isNotCustomItem(item)) return Collections.emptyMap();
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(BASE_STATS_KEY, new StatMapDataType(), new HashMap<>());
    }

    /**
     * 아이템의 현재 표시되는 로어(Lore)에서 최종 능력치 값을 파싱하여 가져옵니다.
     * (기본 능력치 + 재련/인챈트 등의 보너스가 포함된 최종 값)
     * @param item 능력치를 확인할 ItemStack
     * @return 로어에서 파싱된 최종 능력치 맵 (능력치 키, 값)
     */
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
                        // Group 2는 능력치 값과 부호를 포함한 문자열
                        stats.put(statKey, Double.parseDouble(matcher.group(2)));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return stats;
    }

    /**
     * 로어 문자열에서 실제 능력치 값만 파싱합니다.
     * @param cleanLine 태그가 제거된 로어 문자열 (예: "공격력: 10 (+2)")
     * @return 파싱된 능력치 값 (예: 10.0)
     * @throws NumberFormatException 값이 숫자가 아닐 경우 발생
     */
    public double parseValueFromLore(String cleanLine) throws NumberFormatException {
        // 콜론(:) 뒤의 문자열을 가져와 공백을 제거
        String valueString = cleanLine.substring(cleanLine.indexOf(":") + 1).trim();
        // 첫 번째 공백을 기준으로 문자열을 분리하여 최종 능력치 값만 가져옵니다.
        return Double.parseDouble(valueString.split(" ")[0]);
    }

    /**
     * ItemStack에서 ItemType을 가져오는 메서드입니다.
     * @param item 타입을 확인할 아이템
     * @return 아이템의 타입. 커스텀 아이템이 아니거나 타입 정보가 없으면 UNKNOWN을 반환.
     */
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

    /**
     * **[추가]** ItemStack에서 ItemGrade를 가져오는 메서드입니다.
     * @param item 등급을 확인할 아이템
     * @return 아이템의 등급. 커스텀 아이템이 아니거나 등급 정보가 없으면 COMMON을 반환.
     */
    public ItemGrade getItemGrade(ItemStack item) {
        if (isNotCustomItem(item)) {
            return ItemGrade.COMMON;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        // PDC에서 저장된 ItemGrade 문자열을 가져옵니다.
        String gradeString = container.get(CUSTOM_ITEM_GRADE_KEY, PersistentDataType.STRING);
        if (gradeString == null) {
            return ItemGrade.COMMON;
        }
        try {
            // 문자열을 ItemGrade enum으로 변환합니다.
            return ItemGrade.valueOf(gradeString);
        } catch (IllegalArgumentException e) {
            return ItemGrade.COMMON;
        }
    }

    /**
     * 아이템 로어에 표시되는 한글 능력치 이름을 내부적으로 사용되는 능력치 키(String)로 변환합니다.
     * @param name 로어에 표시된 능력치 이름 (예: "피해량", "힘")
     * @return 내부 능력치 키 (예: "ATTACK_DAMAGE", "STRENGTH"), 매칭되는 것이 없으면 null
     */
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

    /**
     * 아이템 ID를 사용하여 등록된 커스텀 아이템 템플릿의 복사본을 가져옵니다.
     * @param itemId 가져올 커스텀 아이템의 ID
     * @return 커스텀 아이템의 복사본, ID가 없으면 null
     */
    public ItemStack getItem(String itemId) {
        return customItems.get(itemId) != null ? customItems.get(itemId).clone() : null;
    }

    /**
     * 등록된 모든 커스텀 아이템의 ID 목록을 가져옵니다.
     * @return 커스텀 아이템 ID의 Set
     */
    public Set<String> getAllItemIds() {
        return customItems.keySet();
    }
}