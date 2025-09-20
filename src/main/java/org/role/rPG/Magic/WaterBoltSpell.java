package org.role.rPG.Magic;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.role.rPG.Player.StatManager;
import org.role.rPG.RPG;

import java.util.UUID;

public class WaterBoltSpell implements Spell {

    private final RPG plugin;
    private final StatManager statManager;

    public WaterBoltSpell(RPG plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    @Override
    public String getName() {
        return "워터 볼트";
    }

    @Override
    public void cast(Player caster) {
        // 물방울 모양 머리 아이템 생성
        ItemStack waterDropHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) waterDropHead.getItemMeta();

        if (meta != null) {
            // ▼▼▼ [수정됨] 특정 플레이어의 스킨을 사용하도록 변경 ▼▼▼
            // "MHF_Water" 부분에 원하는 스킨을 가진 플레이어 닉네임을 넣으세요.
            String textureValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODhhMGY3YmQzZDU4YzU4ZmI5NWU0OGIyYjQ0OTIzZjVlYWEyYzFkNTRkY2Q3MmZhN2NlZmNiYmMxZDRjODFhZCJ9fX0=";

            // Paper API를 사용하여 프로필 생성 및 텍스처 적용
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
            profile.setProperty(new ProfileProperty("textures", textureValue));
            meta.setPlayerProfile(profile);
            waterDropHead.setItemMeta(meta);

            // ▲▲▲ 여기까지 수정 ▲▲▲
        }

        // 플레이어의 눈 위치에서 ItemDisplay 엔티티 생성
        Location spawnLocation = caster.getEyeLocation();
        ItemDisplay projectile = spawnLocation.getWorld().spawn(spawnLocation, ItemDisplay.class, (display) -> {
            display.setItemStack(waterDropHead);
            display.setBillboard(Display.Billboard.FIXED);

            Transformation transformation = display.getTransformation();
            transformation.getScale().set(new Vector3f(0.3f, 0.3f, 0.6f));
            display.setTransformation(transformation);
        });

        new BukkitRunnable() {
            private final Vector direction = caster.getLocation().getDirection();
            private final double speed = 0.8;
            private int lifespan = 0;

            @Override
            public void run() {
                if (lifespan++ > 40) {
                    projectile.remove();
                    this.cancel();
                    return;
                }

                projectile.teleport(projectile.getLocation().add(direction.clone().multiply(speed)));
                projectile.setTeleportDuration(1);

                // ▼▼▼ [수정됨] 2. 올바른 메서드로 주변 엔티티 검색 ▼▼▼
                for (var entity : projectile.getWorld().getNearbyEntities(projectile.getBoundingBox().expand(0.8))) {
                    // 살아있는 엔티티가 아니거나, 시전자 본인이면 건너뛰기
                    if (!(entity instanceof LivingEntity target) || entity.equals(caster)) {
                        continue;
                    }

                    double intelli = statManager.getFinalStat(caster.getUniqueId(), "MAX_MANA");
                    double damage = 10 + (intelli / 5.0);

                    target.damage(damage, caster);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5f, 1.5f);

                    projectile.remove();
                    this.cancel();
                    return; // 하나의 대상만 맞추고 사라지도록 return
                }
                // ▲▲▲ 여기까지 수정 ▲▲▲
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
