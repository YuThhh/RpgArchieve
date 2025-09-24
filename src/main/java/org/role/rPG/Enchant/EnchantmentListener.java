package org.role.rPG.Enchant; // 패키지 이름은 본인 환경에 맞게 유지

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.role.rPG.Effect.effects.Bleeding;
import org.role.rPG.Effect.Effect;
import org.role.rPG.Effect.EffectManager;

public class EnchantmentListener implements Listener {

    private final EnchantmentManager enchantmentManager;
    private final EffectManager effectManager; // EffectManager를 사용하도록 필드 추가

    // 생성자: 이제 EffectManager도 받습니다.
    public EnchantmentListener(EnchantmentManager enchantmentManager, EffectManager effectManager) {
        this.enchantmentManager = enchantmentManager;
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        // 출혈과 같은 지속 데미지로 인한 무한 루프 방지 (매우 중요)
        if (event.getEntity().hasMetadata(Bleeding.BLEEDING_DAMAGE_KEY.getKey())) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        if (weapon.getType().isAir()) {
            return;
        }

        // 등록된 모든 커스텀 인챈트를 순회하며 무기에 적용되었는지 확인
        for (CustomEnchantment enchant : enchantmentManager.getAllEnchantments()) {
            int level = enchantmentManager.getEnchantmentLevel(weapon, enchant);

            if (level > 0) {
                // 이 인챈트가 Effect를 발동시키는 종류(EffectEnchantment)인지 확인
                if (enchant instanceof EffectEnchantment) {
                    EffectEnchantment effectEnchant = (EffectEnchantment) enchant;
                    String effectName = effectEnchant.getEffectName();

                    // EffectManager를 통해 실제 효과 객체를 가져옴
                    Effect effect = effectManager.getEffect(effectName);

                    if (effect != null) {
                        // 효과 발동!
                        effect.getEffect(victim, attacker);
                        // 참고: 여러 효과를 동시에 발동시키고 싶지 않다면 여기에 break;를 추가하세요.
                    }
                }
                // 여기에 다른 종류의 인챈트(예: 단순 데미지 증가)를 위한 else if 로직을 추가할 수 있습니다.
            }
        }
    }
}