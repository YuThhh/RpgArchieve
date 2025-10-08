package org.role.rPG.Food;

import org.bukkit.NamespacedKey; // NamespacedKey 임포트
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Item.ItemType;
import org.role.rPG.Player.Buff;
import org.role.rPG.Player.BuffManager;
import org.role.rPG.Player.StatManager;

import java.util.Map;
import java.util.Objects;

public class FoodListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final StatManager statManager;
    private final BuffManager buffManager;

    public FoodListener(JavaPlugin plugin, ItemManager itemManager, StatManager statManager, BuffManager buffManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.statManager = statManager;
        this.buffManager = buffManager; // <-- 초기화
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack consumedItem = event.getItem();
        if (itemManager.isNotCustomItem(consumedItem)) return;

        if (itemManager.getItemType(consumedItem) == ItemType.CONSUMABLE_FOOD) {
            applyEffects(event.getPlayer(), consumedItem);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || itemManager.isNotCustomItem(itemInHand)) return;

        if (itemManager.getItemType(itemInHand) == ItemType.CONSUMABLE_INSTANT) {
            event.setCancelled(true);
            applyEffects(event.getPlayer(), itemInHand);
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        }
    }

    @SuppressWarnings("unchecked") // 이 어노테이션으로 'Unchecked cast' 경고를 제거합니다.
    private void applyEffects(Player player, ItemStack item) {
        Map<String, Object> effects = itemManager.getConsumableEffects(item);
        if (effects == null) return;

        // 즉시 체력 회복
        Object rawHealth = effects.get("instant-health");
        if (rawHealth instanceof Number) { // [NPE 방지] 타입 확인
            double amount = ((Number) rawHealth).doubleValue();
            double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + amount));
        }

        // 허기 및 포만감
        Object rawHunger = effects.get("hunger");
        if (rawHunger instanceof Number) {
            int amount = ((Number) rawHunger).intValue();
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + amount));
        }
        Object rawSaturation = effects.get("saturation");
        if (rawSaturation instanceof Number) {
            float amount = ((Number) rawSaturation).floatValue();
            player.setSaturation(Math.min(player.getFoodLevel(), player.getSaturation() + amount));
        }

        // 포션 효과
        Object rawPotionEffects = effects.get("potion-effects");
        if (rawPotionEffects instanceof Map) { // [NPE 방지] 타입 확인
            Map<String, Map<String, Object>> potionEffects = (Map<String, Map<String, Object>>) rawPotionEffects;
            for (Map.Entry<String, Map<String, Object>> entry : potionEffects.entrySet()) {

                PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                if (type == null) continue;

                Map<String, Object> effectData = entry.getValue();
                Object rawDuration = effectData.get("duration");
                Object rawAmplifier = effectData.get("amplifier");

                if (rawDuration instanceof Number && rawAmplifier instanceof Number) { // [NPE 방지]
                    int duration = ((Number) rawDuration).intValue();
                    int amplifier = ((Number) rawAmplifier).intValue();
                    player.addPotionEffect(new PotionEffect(type, duration, amplifier));
                }
            }
        }

        // 스탯 버프
        Object rawStatBuffs = effects.get("stat-buffs");
        if (rawStatBuffs instanceof Map) {
            Map<String, Map<String, Object>> statBuffs = (Map<String, Map<String, Object>>) rawStatBuffs;
            for (Map.Entry<String, Map<String, Object>> entry : statBuffs.entrySet()) {
                String statName = entry.getKey().toUpperCase(); // 스탯 이름을 대문자로 통일
                Map<String, Object> buffData = entry.getValue();

                Object rawValue = buffData.get("value");
                Object rawDuration = buffData.get("duration");

                if (rawValue instanceof Number && rawDuration instanceof Number) {
                    double value = ((Number) rawValue).doubleValue();
                    int durationTicks = ((Number) rawDuration).intValue();

                    // 새로운 Buff 객체를 생성하여 BuffManager에 추가합니다.
                    Buff newBuff = new Buff(statName, value, durationTicks);
                    buffManager.addBuff(player, newBuff);
                }
            }
        }
    }
}