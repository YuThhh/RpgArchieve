package org.role.rPG.Item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.format.TextDecoration;
import org.role.rPG.RPG;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                int version = config.getInt("version", 1);
                Material material = Material.matchMaterial(config.getString("material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning(itemId + " 아이템의 material을 찾을 수 없습니다: " + config.getString("material"));
                    continue;
                }

                Component name = MINI_MESSAGE.deserialize(config.getString("name", ""))
                        .decoration(TextDecoration.ITALIC, false);
                List<Component> lore = config.getStringList("lore").stream()
                        .map(MINI_MESSAGE::deserialize)
                        .collect(Collectors.toList());

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    meta.displayName(name);
                    meta.lore(lore);
                    meta.setUnbreakable(config.getBoolean("unbreakable", false));

                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    container.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);
                    container.set(CUSTOM_ITEM_VERSION_KEY, PersistentDataType.INTEGER, version);

                    ConfigurationSection attributesSection = config.getConfigurationSection("attributes");
                    if (attributesSection != null) {
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                        for (String attrKey : attributesSection.getKeys(false)) {
                            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrKey.toLowerCase()));
                            if (attribute == null) {
                                plugin.getLogger().warning(itemId + " 아이템의 attribute를 찾을 수 없습니다: " + attrKey);
                                continue;
                            }
                            double amount = attributesSection.getDouble(attrKey);

                            // ▼▼▼ [수정된 부분] AttributeModifier를 최신 방식으로 생성 ▼▼▼
                            NamespacedKey modifierKey = new NamespacedKey(plugin, itemId + "_" + attrKey.replace('.', '_'));
                            AttributeModifier modifier = new AttributeModifier(
                                    modifierKey,
                                    amount,
                                    AttributeModifier.Operation.ADD_NUMBER,
                                    EquipmentSlot.HAND.getGroup() // 적용될 슬롯 그룹 지정
                            );
                            meta.addAttributeModifier(attribute, modifier);
                        }
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