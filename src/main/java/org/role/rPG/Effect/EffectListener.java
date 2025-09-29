package org.role.rPG.Effect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Effect.effects.Bleeding;
import org.role.rPG.Item.ItemManager;

import java.util.List;
import java.util.Objects;

public class EffectListener implements Listener {

    private final EffectManager effectManager;
    private final ItemManager itemManager;

    public EffectListener(JavaPlugin plugin, ItemManager itemManager) {
        this.effectManager = new EffectManager(plugin);
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 1. 공격자가 플레이어이고, 피격자가 살아있는 개체인지 확인합니다.
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        ItemStack itemInHand = attacker.getInventory().getItemInMainHand();

        // [추가] 출혈 데미지 표식이 있는지 확인하고, 있다면 무한 루프 방지를 위해 즉시 중단합니다.
        if (victim.hasMetadata(Bleeding.BLEEDING_DAMAGE_KEY.getKey())) {
            return;
        }

        // 2. 공격자가 손에 든 아이템이 커스텀 아이템인지, 로어가 있는지 확인합니다.
        if (itemManager.isNotCustomItem(itemInHand) || !itemInHand.hasItemMeta() || !itemInHand.getItemMeta().hasLore()) {
            return;
        }

        // 3. 아이템 로어에서 "효과:"를 찾습니다.
        List<Component> lore = itemInHand.getItemMeta().lore();
        for (Component lineComponent : Objects.requireNonNull(lore)) {
            String plainLine = PlainTextComponentSerializer.plainText().serialize(lineComponent);

            if (plainLine.startsWith("효과:")) {
                String effectName = plainLine.substring(plainLine.indexOf(":") + 1).trim();
                Effect effect = effectManager.getEffect(effectName);

                if (effect != null) {
                    // 4. 피격자(victim)에게 효과를 적용하고, 공격자(attacker)를 시전자로 전달합니다.
                    effect.getEffect(victim, attacker);
                    break; // 효과를 하나만 적용하기 위해 반복을 중단합니다.
                }
            }
        }
    }
}