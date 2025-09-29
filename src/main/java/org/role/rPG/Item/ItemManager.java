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
    private static final NamespacedKey BASE_STATS_KEY = new NamespacedKey("rpg", "base_stats");
    private static final NamespacedKey REFORGE_KEY = new NamespacedKey("rpg", "reforge_id");
    private static final Pattern LORE_STAT_PATTERN = Pattern.compile("([^:]+): ([+\\-]?\\d+(\\.\\d+)?)");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final NamespacedKey CONFIG_HASH_KEY = new NamespacedKey("rpg", "config_hash");

    static {
        CUSTOM_ITEM_ID_KEY = new NamespacedKey("rpg", "custom_item_id");
        CUSTOM_ITEM_TYPE_KEY = new NamespacedKey("rpg", "custom_item_type");
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
        // 1. 설정 파일 다시 불러오기
        itemConfig = YamlConfiguration.loadConfiguration(itemConfigFile);

        // 2. 기존 아이템 데이터 초기화
        customItems.clear();

        // 3. 'items' 섹션 가져오기 및 null 체크
        ConfigurationSection itemsSection = itemConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Item.yml에서 'items' 섹션을 찾을 수 없습니다.");
            return;
        }

        // 4. 각 아이템 ID를 순회하며 아이템 생성
        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection config = itemsSection.getConfigurationSection(itemId);
            if (config == null) continue;

            // 5. 오류 발생에 대비한 try-catch 블록
            try {
                // 5-1. Material 정보 파싱 및 null 체크
                Material material = Material.matchMaterial(config.getString("material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning(itemId + " 아이템의 material을 찾을 수 없습니다.");
                    continue;
                }

                // 5-2. ItemStack 생성 및 ItemMeta 설정
                ItemStack item = new ItemStack(material);
                ItemMeta meta = buildMetaFromConfig(itemId, config); // 핵심 로직 호출
                item.setItemMeta(meta);

                // 5-3. 메모리에 아이템 저장
                customItems.put(itemId, item);

            } catch (Exception e) {
                plugin.getLogger().warning(itemId + " 아이템 로딩 중 오류 발생:");
                e.printStackTrace();
            }
        }

        // 6. 로드 결과 로그 출력
        plugin.getLogger().info(customItems.size() + "개의 커스텀 아이템을 로드했습니다.");
    }

    private ItemMeta buildMetaFromConfig(String itemId, ConfigurationSection config) {
        // 1. 아이템의 기본 Material 설정
        // 설정 파일(config)에서 "material" 값을 읽어오고, 값이 없으면 기본값으로 "STONE"을 사용합니다.
        Material material = Material.matchMaterial(config.getString("material", "STONE"));

        // 2. Material을 기반으로 ItemMeta 객체 생성
        // Material의 ItemMeta 객체를 가져옵니다. Objects.requireNonNull()로 material이 null이 아님을 보장합니다.
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Objects.requireNonNull(material));

        // 3. 아이템 이름(Display Name) 설정
        // 설정 파일에서 "name" 값을 읽어와 MiniMessage로 Component로 변환합니다.
        // 값이 없으면 빈 문자열("")을 사용하고, 이름에 이탤릭체(기울임꼴)는 적용하지 않도록 설정합니다.
        Component name = MINI_MESSAGE.deserialize(config.getString("name", "")).decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        // 4. 아이템 설명(Lore) 설정
        // 설정 파일에서 "lore" 리스트를 읽어옵니다.
        List<String> originalLoreStrings = config.getStringList("lore");
        // 각 설명 줄을 MiniMessage로 Component로 변환하고, 이탤릭체를 적용하지 않도록 처리한 후 리스트로 수집합니다.
        List<Component> loreForDisplay = originalLoreStrings.stream()
                .map(line -> MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
        meta.lore(loreForDisplay);

        // 5. 파괴 불가(Unbreakable) 설정
        // 설정 파일에서 "unbreakable" 값을 읽어와 적용하고, 값이 없으면 기본값으로 false를 사용합니다.
        meta.setUnbreakable(config.getBoolean("unbreakable", false));

        // 6. PersistentDataContainer(영구 데이터 저장소) 준비
        // ItemMeta에 영구적으로 데이터를 저장할 수 있는 컨테이너를 가져옵니다.
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // 7. 커스텀 아이템 ID 저장
        // 아이템의 고유 ID를 PersistentDataContainer에 저장합니다.
        container.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);

        // 8. 아이템 타입(ItemType) 저장
        try {
            // 설정 파일에서 "type" 값을 읽어와 대문자로 변환합니다. 값이 없으면 기본값으로 "MISC"를 사용합니다.
            String typeString = config.getString("type", "MISC").toUpperCase();
            // 문자열을 ItemType enum으로 변환합니다.
            ItemType itemType = ItemType.valueOf(typeString);
            // 변환된 ItemType의 이름을 PersistentDataContainer에 저장합니다.
            container.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, itemType.name());
        } catch (IllegalArgumentException e) {
            // yml에 잘못된 타입이 적혀있을 경우, 기본값(MISC)으로 설정
            container.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, ItemType.MISC.name());
            plugin.getLogger().warning(itemId + " 아이템의 type이 잘못되었습니다. MISC로 설정합니다.");
        }

        // 9. 설정 해시 값(데이터 지문) 저장
        // ▼▼▼ 버전 저장 로직을 아래 해시 저장 로직으로 교체 ▼▼▼
        // 아이템의 모든 설정 값을 문자열로 만든 뒤, hashCode를 계산하여 '데이터 지문'으로 사용합니다.
        // 이 해시 값은 아이템 설정이 변경되었는지 확인하는 용도로 사용될 수 있습니다.
        String configString = config.getValues(true).toString();
        int configHash = configString.hashCode();
        container.set(CONFIG_HASH_KEY, PersistentDataType.INTEGER, configHash);
        // ▲▲▲ 여기까지 ▲▲▲

        // 10. 아이템의 기본 능력치(Base Stats) 파싱 및 저장
        Map<String, Double> baseStats = new HashMap<>();
        // 원래의 Lore 문자열 리스트를 반복합니다.
        for (String loreLine : originalLoreStrings) {
            // MiniMessage 태그를 제거하여 순수 텍스트만 남깁니다.
            String cleanLine = MINI_MESSAGE.stripTags(loreLine);
            // 정의된 정규 표현식 패턴(LORE_STAT_PATTERN)을 사용하여 능력치 정보를 찾습니다.
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);
            if (matcher.find()) {
                // 정규식으로 찾은 능력치 이름(Group 1)을 실제 능력치 키로 변환합니다.
                String statName = getStatKeyFromName(matcher.group(1));
                if (statName != null) {
                    // 정규식으로 찾은 능력치 값(Group 2)에서 "+"를 제거하고 Double로 변환합니다.
                    // 능력치 이름과 값을 baseStats 맵에 저장합니다.
                    baseStats.put(statName, Double.parseDouble(matcher.group(2).replace("+", "")));
                }
            }
        }
        // 파싱된 기본 능력치 맵을 PersistentDataContainer에 저장합니다.
        container.set(BASE_STATS_KEY, new StatMapDataType(), baseStats);

        // 11. 아이템 속성(Attribute) 적용 및 ItemMeta 반환
        // Lore에서 파싱된 정보를 기반으로 아이템 속성(Attributes)을 적용하는 메서드를 호출하고 최종 ItemMeta를 반환합니다.
        return applyAttributesFromLore(meta, itemId, originalLoreStrings);
    }

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
            // [버그 수정] "피해량"을 getStatKeyFromName으로 확인
            if ("ATTACK_DAMAGE".equals(getStatKeyFromName(cleanLine.split(":")[0]))) {
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
        ItemMeta updatedMeta = applyAttributesFromLore(meta, itemId, loreStrings);
        item.setItemMeta(updatedMeta);
    }

    // [수정] 요청대로 isNotCustomItem 메소드 사용
    public boolean isNotCustomItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return true;
        return !item.getItemMeta().getPersistentDataContainer().has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public void reforgeItem(ItemStack item, ReforgeManager.ReforgeModifier modifier) {
        // 1. 커스텀 아이템인지 확인 (필수 전제 조건)
        // 아이템이 플러그인에서 관리하는 커스텀 아이템이 아니면 즉시 종료합니다.
        if (isNotCustomItem(item)) return;

        // 2. ItemMeta와 PersistentDataContainer 준비
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        // 아이템의 PersistentDataContainer에 저장된 기본 능력치 맵을 가져옵니다.
        Map<String, Double> baseStats = getBaseStats(item);

        // 3. 원본 아이템 템플릿 로드 (이름 복원을 위해)
        // [수정] 기존의 불안정한 이름 제거 방식 대신, 원본 아이템의 이름을 가져와 사용합니다.
        String itemId = container.get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        // 아이템 ID를 사용하여 등록된 커스텀 아이템 템플릿(원본)을 가져옵니다.
        ItemStack templateItem = customItems.get(itemId);

        // 템플릿 아이템의 유효성 검사
        if (templateItem == null || templateItem.getItemMeta() == null) {
            // 템플릿 아이템을 찾을 수 없는 경우, 오류를 방지하기 위해 작업을 중단합니다.
            plugin.getLogger().warning(itemId + "에 대한 아이템 템플릿을 찾을 수 없어 리포지를 중단합니다.");
            return;
        }

        // 템플릿 아이템에서 원본 이름(Display Name)을 가져옵니다.
        Component originalName = templateItem.getItemMeta().displayName();
        if (originalName == null) {
            originalName = Component.text("이름 없는 아이템"); // 만약을 위한 대비
        }

        // 4. 아이템 이름(Display Name) 업데이트
        // 새로운 접두사 컴포넌트를 생성하고 색상을 YELLOW로 설정합니다. (예: "[Strong] ")
        Component prefixComponent = Component.text(modifier.getName() + " ").color(NamedTextColor.YELLOW);
        // 접두사를 원본 이름 앞에 붙이고 이탤릭체를 제거하여 새로운 이름으로 설정합니다.
        meta.displayName(prefixComponent.append(originalName).decoration(TextDecoration.ITALIC, false));

        // 5. 재련 정보(ID) 저장
        // [매우 중요] 접두사 이름(getName) 대신, 고유 ID(getId)를 저장합니다.
        // 이는 재련된 상태를 영구적으로 기록하고, 나중에 해당 접두사를 식별하는 데 사용됩니다.
        container.set(REFORGE_KEY, PersistentDataType.STRING, modifier.getId());

        // 6. 새로운 로어(Lore) 생성 로직 준비
        // 적용할 능력치 변경 비율(multiplier) 맵을 가져옵니다.
        Map<String, Double> reforgeModifiers = modifier.getStatModifiers();
        // 새로운 로어 컴포넌트를 저장할 리스트를 초기화합니다.
        List<Component> newLore = new ArrayList<>();

        // 원본 아이템의 로어를 기반으로 새로운 로어를 생성합니다.
        List<Component> originalLoreComponents = Objects.requireNonNull(templateItem.getItemMeta().lore());

        // 7. 로어를 순회하며 능력치 라인 업데이트
        for (Component loreComponent : originalLoreComponents) {
            // Component를 MiniMessage 형식의 문자열로 직렬화합니다.
            String loreLineFormat = MINI_MESSAGE.serialize(loreComponent);
            // 태그를 제거하여 순수한 텍스트만 남깁니다.
            String cleanLine = MINI_MESSAGE.stripTags(loreLineFormat);
            // 능력치 패턴 정규식으로 매칭을 시도합니다.
            Matcher matcher = LORE_STAT_PATTERN.matcher(cleanLine);

            if (matcher.find()) { // 현재 로어 줄이 능력치 라인인 경우
                String statDisplayName = matcher.group(1).trim();
                // 표시 이름을 사용하여 실제 능력치 키(key)를 가져옵니다.
                String statKey = getStatKeyFromName(statDisplayName);

                if (statKey != null) {
                    // 기본 능력치 값과 재련 보너스 배율을 가져옵니다. (없으면 0.0)
                    double baseValue = baseStats.getOrDefault(statKey, 0.0);
                    double reforgeMultiplier = reforgeModifiers.getOrDefault(statKey, 0.0);

                    // 최종 능력치 값 계산: Final = Base * (1 + Multiplier)
                    double finalValue = baseValue * (1 + reforgeMultiplier);
                    // 기본값과의 차이(증가/감소량)를 계산합니다.
                    double diff = finalValue - baseValue;

                    // 새로운 로어 라인 포맷팅 (예: "공격력: 10 (+2)")
                    String newLoreLine = String.format("<i:false><gray>%s: </gray><white>%.0f</white>", statDisplayName, finalValue);
                    // 변경량이 0.01 이상일 경우에만 차이 값을 표시합니다.
                    if (Math.abs(diff) > 0.01) {
                        // 차이 값(diff)이 양수일 경우 "+" 기호를 붙입니다.
                        newLoreLine += String.format(" <gray>(%s%.0f)</gray>", diff > 0 ? "+" : "", diff);
                    }
                    // 포맷팅된 문자열을 Component로 변환하여 새로운 로어 리스트에 추가합니다.
                    newLore.add(MINI_MESSAGE.deserialize(newLoreLine));
                } else {
                    // 능력치 패턴은 찾았으나 유효한 능력치 키가 없는 경우, 원본 로어만 추가합니다.
                    newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
                }
            } else {
                // 능력치 패턴을 찾지 못한 일반 로어 라인인 경우, 원본 로어만 추가합니다.
                newLore.add(loreComponent.decoration(TextDecoration.ITALIC, false));
            }
        }

        // 8. ItemMeta 업데이트 및 최종 능력치 새로고침
        // 새로 생성된 로어 리스트를 ItemMeta에 적용합니다.
        meta.lore(newLore);
        // ItemMeta를 ItemStack에 다시 설정합니다.
        item.setItemMeta(meta);
        // 변경된 로어(능력치)를 기반으로 아이템의 최종 능력치(Attributes 등)를 새로고침합니다.
        refreshStatsFromLore(item);
    }

    public boolean updateItemIfNecessary(ItemStack item) {
        if (isNotCustomItem(item)) return false;

        ItemMeta currentMeta = item.getItemMeta();
        String itemId = currentMeta.getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        ItemStack latestItemTemplate = customItems.get(itemId);

        if (latestItemTemplate == null || latestItemTemplate.getItemMeta() == null) return false;
        ItemMeta latestMeta = latestItemTemplate.getItemMeta();

        // 1. 플레이어 아이템에 저장된 해시와 최신 아이템의 해시를 가져옵니다.
        int currentHash = currentMeta.getPersistentDataContainer().getOrDefault(CONFIG_HASH_KEY, PersistentDataType.INTEGER, 0);
        int latestHash = latestMeta.getPersistentDataContainer().getOrDefault(CONFIG_HASH_KEY, PersistentDataType.INTEGER, 1);

        // 2. 해시 값이 다른 경우에만 업데이트를 진행합니다.
        if (currentHash != latestHash) {
            System.out.println("아이템 업데이트 발견 (" + itemId + "): 해시 불일치. " + currentHash + " -> " + latestHash);

            // [수정 후]
            PersistentDataContainer currentContainer = currentMeta.getPersistentDataContainer();
            PersistentDataContainer latestContainer = latestMeta.getPersistentDataContainer();

            // 해시 업데이트
            currentContainer.set(CONFIG_HASH_KEY, PersistentDataType.INTEGER, latestHash);
            // 기본 스탯 업데이트
            currentContainer.set(BASE_STATS_KEY, new StatMapDataType(), getBaseStats(latestItemTemplate));
            // ▼▼▼ 누락되었던 타입 정보 업데이트 코드 ▼▼▼
            currentContainer.set(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING, Objects.requireNonNull(latestContainer.get(CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING)));
            // ▲▲▲ 여기까지 ▲▲▲

            item.setItemMeta(currentMeta); // 변경된 모든 데이터를 먼저 저장
            updateLoreAndStats(item); // 최종적으로 로어와 모든 스탯을 새로고침
            return true;
        }

        return false;
    }

    public void updateLoreAndStats(ItemStack item) {
        if (isNotCustomItem(item)) return;
        ItemMeta meta = item.getItemMeta();
        String itemId = meta.getPersistentDataContainer().get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        Map<String, Double> baseStats = getBaseStats(item);
        String reforgeId = meta.getPersistentDataContainer().get(REFORGE_KEY, PersistentDataType.STRING);
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
                    newLore.add(loreComponent);
                }
            } else {
                newLore.add(loreComponent);
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

    /**
     * [추가] ItemStack에서 ItemType을 가져오는 메서드
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

    // [수정] "피해량"과 "공격 피해"를 모두 인식하도록 수정
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