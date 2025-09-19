// 파일 경로: org/role/rPG/MagicListener.java (예시)
package org.role.rPG.Item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.role.rPG.Player.StatManager;
import org.role.rPG.RPG;
import org.bukkit.Bukkit; // Bukkit 추가

import java.util.UUID;
// PlayerProfile 추가
// PlayerTextures 추가
// URL 추가


public class MagicListener implements Listener {

    private final RPG plugin;
    private final ItemManager itemManager;
    private final StatManager statManager;

    public MagicListener(RPG plugin, ItemManager itemManager, StatManager statManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.statManager = statManager;
    }

    @EventHandler
    public void onPlayerUseMagicBook(PlayerInteractEvent event) {
        // 1. 우클릭 액션인지 확인
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // 2. 손에 든 아이템이 커스텀 아이템인지, 그리고 'test_magicbook'인지 확인
        if (itemManager.isNotCustomItem(itemInHand)) {
            return;
        }

        String itemId = itemInHand.getItemMeta().getPersistentDataContainer().get(ItemManager.CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        if (!"test_magicbook".equals(itemId)) {
            return;
        }

        // TODO: 마나 소모 로직을 여기에 추가할 수 있습니다.
//         double currentMana = statManager.getFinalStat(player.getUniqueId(), "CURRENT_MANA");
//         if (currentMana < 20) {
//             player.sendMessage("마나가 부족합니다!");
//             return;
//         }
//         statManager.updatePlayerCurrentMana(player.getUniqueId(), currentMana - 20);

        // 3. 투사체 발사!
        launchWaterProjectile(player);
    }

    private void launchWaterProjectile(Player caster) {
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
            display.setBillboard(Display.Billboard.CENTER);
            display.setBillboard(Display.Billboard.FIXED);

            Transformation transformation = display.getTransformation();
            transformation.getScale().set(new Vector3f(0.5f, 0.5f, 0.5f));
            display.setTransformation(transformation);


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

                    projectile.remove();
                    this.cancel();
                    return; // 하나의 대상만 맞추고 사라지도록 return
                }
                // ▲▲▲ 여기까지 수정 ▲▲▲
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}