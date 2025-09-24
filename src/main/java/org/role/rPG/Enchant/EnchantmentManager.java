package org.role.rPG.Enchant;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import java.lang.reflect.Modifier;
import java.util.Set;

public class EnchantmentManager {

    private final Map<String, CustomEnchantment> registeredEnchantments = new HashMap<>();

    public void registerEnchantment(CustomEnchantment enchantment) {
        registeredEnchantments.put(enchantment.getName().toLowerCase(), enchantment);
    }

    public CustomEnchantment getEnchantment(String name) {
        return registeredEnchantments.get(name.toLowerCase());
    }

    /**
     * 등록된 모든 커스텀 인챈트의 컬렉션을 반환합니다.
     * @return 모든 CustomEnchantment 객체
     */
    public Collection<CustomEnchantment> getAllEnchantments() {
        return registeredEnchantments.values();
    }

    /**
     * 아이템에 커스텀 인챈트를 적용합니다. (최신 API 버전)
     */
    public void applyEnchantment(ItemStack item, CustomEnchantment enchant, int level) {
        if (item == null || !item.hasItemMeta()) return;
        if (level <= 0 || level > enchant.getMaxLevel()) return;

        ItemMeta meta = item.getItemMeta();

        // 1. NBT 데이터 저장 (플러그인 인식용)
        meta.getPersistentDataContainer().set(enchant.getKey(), PersistentDataType.INTEGER, level);

        // 2. Lore 업데이트 (플레이어 시각용, Adventure 컴포넌트 사용)
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // 기존에 같은 인챈트 Lore가 있으면 제거 (NPE 방지)
        // PlainTextComponentSerializer를 사용해 Component를 일반 String으로 변환 후 비교
        lore.removeIf(line -> {
            String plainText = PlainTextComponentSerializer.plainText().serialize(line);
            return plainText.startsWith(enchant.getName());
        });

        // 새로운 인챈트 Lore 추가 (Component 생성)
        // 예: "흡혈의 칼날 I" (회색, 이탤릭체 없음)
        Component enchantLine = Component.text(enchant.getName() + " " + RomanNumerals.toRoman(level))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);

        lore.add(enchantLine);
        meta.lore(lore); // meta.setLore() 대신 lore() 사용

        item.setItemMeta(meta);
    }

    /**
     * 아이템의 특정 커스텀 인챈트 레벨을 확인합니다. (최신 API 버전)
     */
    public int getEnchantmentLevel(ItemStack item, CustomEnchantment enchant) {
        if (item == null || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        // getOrDefault를 사용해 NullPointerException을 원천 방지
        return meta.getPersistentDataContainer().getOrDefault(enchant.getKey(), PersistentDataType.INTEGER, 0);
    }

    /**
     * 지정된 패키지 내의 모든 CustomEnchantment를 자동으로 찾아 등록합니다.
     * @param plugin 메인 플러그인 인스턴스
     * @param packageName 스캔할 패키지 경로 (예: "org.role.rPG.Enchant.enchants")
     */
    public void registerEnchantmentsFromPackage(JavaPlugin plugin, String packageName) {
        Reflections reflections = new Reflections(packageName);
        // CustomEnchantment 클래스를 상속하는 모든 클래스를 찾습니다.
        Set<Class<? extends CustomEnchantment>> enchantClasses = reflections.getSubTypesOf(CustomEnchantment.class);

        for (Class<? extends CustomEnchantment> clazz : enchantClasses) {
            // 추상 클래스나 인터페이스는 객체로 만들 수 없으므로 건너뜁니다.
            if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
                continue;
            }

            try {
                // JavaPlugin을 인자로 받는 생성자를 통해 객체를 생성하고 등록합니다.
                CustomEnchantment enchantment = clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
                registerEnchantment(enchantment);
                plugin.getLogger().info("✅ Successfully registered enchantment: " + enchantment.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("❌ Failed to register enchantment " + clazz.getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static class RomanNumerals {
        private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        public static String toRoman(int number) {
            if (number < 1 || number > 3999) {
                return String.valueOf(number); // 범위를 벗어나면 그냥 숫자로 반환
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < VALUES.length; i++) {
                while (number >= VALUES[i]) {
                    number -= VALUES[i];
                    sb.append(SYMBOLS[i]);
                }
            }
            return sb.toString();
        }
    }
}