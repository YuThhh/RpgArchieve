package org.role.rPG.Skill;

import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.role.rPG.Player.Stat;
import org.role.rPG.Player.StatManager;

public class FireBallSpell implements Spell {

    private final JavaPlugin plugin;
    private final StatManager statManager;

    private final String MAGIC_NAME = "파이어 볼";
    private final Double COOLDOWN = 0.7;

    public FireBallSpell(JavaPlugin plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    @Override
    public String getName() {
        return MAGIC_NAME;
    }

    @Override
    public double getCoolDown() {
        return COOLDOWN;
    }

    @Override
    public void cast(Player caster) {
        // 마나 소모
         double currentMana = statManager.getFinalStat(caster.getUniqueId(), "CURRENT_MANA");
         if (currentMana < 20) {
             caster.sendMessage("마나가 부족합니다!");
             return;
         }
         statManager.updatePlayerCurrentMana(caster.getUniqueId(), currentMana - 20);

        // 물방울 모양 머리 아이템 생성
        ItemStack FireBallDisplay = new ItemStack(Material.FIRE_CHARGE);

        // 플레이어의 눈 위치에서 ItemDisplay 엔티티 생성
        Location spawnLocation = caster.getEyeLocation();
        ItemDisplay projectile = spawnLocation.getWorld().spawn(spawnLocation, ItemDisplay.class, (display) -> {
            display.setItemStack(FireBallDisplay);
            display.setBillboard(Display.Billboard.FIXED);

            Transformation transformation = display.getTransformation();
            transformation.getScale().set(new Vector3f(0.6f, 0.6f, 0.6f));
            display.setTransformation(transformation);

            caster.playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
        });

        new BukkitRunnable() {
            private final Vector direction = caster.getLocation().getDirection();
            private final double speed = 0.5;
            private int lifespan = 0;

            @Override
            public void run() {
                if (lifespan++ > 40) {
                    projectile.remove();
                    this.cancel();
                    projectile.getWorld().spawnParticle(Particle.LAVA, projectile.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
                    projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                    return;
                }

                // 1. 다음 틱에 투사체가 이동할 위치를 미리 계산합니다.
                Location nextLocation = projectile.getLocation().clone().add(direction.clone().multiply(speed));

                // 2. 다음 위치에 있는 블록이 단단한 블록(벽, 바닥 등)인지 확인합니다.
                if (nextLocation.getBlock().getType().isSolid()) {
                    // 3. 만약 벽이라면, 충돌 효과를 발생시키고 투사체를 제거합니다.

                    projectile.getWorld().spawnParticle(Particle.LAVA, projectile.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
                    projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);

                    projectile.remove();
                    this.cancel();
                    return; // 벽에 부딪혔으므로 아래의 엔티티 확인 로직은 실행할 필요가 없습니다.
                }

                projectile.teleport(projectile.getLocation().add(direction.clone().multiply(speed)));
                projectile.setTeleportDuration(1);
                projectile.getWorld().spawnParticle(Particle.FLAME, projectile.getLocation(), 1, 0.1, 0.1, 0.1, 0.05);

                // ▼▼▼ [수정됨] 2. 올바른 메서드로 주변 엔티티 검색 ▼▼▼
                for (var entity : projectile.getWorld().getNearbyEntities(projectile.getBoundingBox().expand(0.3))) {
                    // 살아있는 엔티티가 아니거나, 시전자 본인이면 건너뛰기
                    if (!(entity instanceof LivingEntity target) || entity.equals(caster)) {
                        continue;
                    }

                    double intelli = statManager.getFinalStat(caster.getUniqueId(), "MAX_MANA");
                    double damage = 10 + (intelli / 5.0);

                    // 1. 데미지를 주기 직전, 피격자에게 '마법 데미지' 표식을 붙입니다.
                    target.setMetadata(Stat.MAGIC_DAMAGE_KEY, new FixedMetadataValue(plugin, true));

                    // 2. 데미지를 적용합니다.
                    target.damage(damage, caster);

                    // 3. 다음 틱에 표식을 제거합니다.
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        target.removeMetadata(Stat.MAGIC_DAMAGE_KEY, plugin);
                    }, 1L);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.5f, 1.0f);
                    projectile.getWorld().spawnParticle(Particle.LAVA, projectile.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);

                    projectile.remove();
                    this.cancel();
                    return; // 하나의 대상만 맞추고 사라지도록 return
                }
                // ▲▲▲ 여기까지 수정 ▲▲▲
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
