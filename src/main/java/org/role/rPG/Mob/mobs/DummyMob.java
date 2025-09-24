package org.role.rPG.Mob.mobs;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
// [변경] Listener -> CustomMob 으로 변경
// Listener는 CustomMob이 상속하므로 별도로 쓸 필요가 없습니다.
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Mob.CustomMob;

import java.util.Comparator;
import java.util.Objects;

// [변경] CustomMob 인터페이스를 구현(implements)하도록 수정
public class DummyMob implements CustomMob {

    private final JavaPlugin plugin;
    public static final NamespacedKey CUSTOM_MOB_ID_KEY= new NamespacedKey("rpg", "custom_mob_id");
    public static final NamespacedKey IS_DUMMY_KEY = new NamespacedKey("rpg", "is_dummy");

    private final int MobLevel = 0;
    private final String MobId = "dummy";
    private final double MobProficiencyExp = MobLevel * 5;

    public DummyMob(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // [추가] CustomMob 인터페이스가 요구하는 getMobId 메소드 구현
    @Override
    public String getMobId() {
        return MobId; // 이 몹의 고유 ID는 "dummy" 입니다.
    }

    @Override
    public double getProficiencyExp() {
        return 0; // 허수아비는 경험치를 주지 않으므로 0을 반환합니다.
    }

    @Override
    public int getMobLevel() {return MobLevel;}

    @Override
    public void spawn(Location location) {
        LivingEntity dummy = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.CREEPER);

        dummy.setAI(false);
        dummy.setGravity(false);
        dummy.setInvulnerable(true);
        dummy.setSilent(true);
        dummy.setCollidable(false);

        Objects.requireNonNull(dummy.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2048);

        dummy.getPersistentDataContainer().set(CUSTOM_MOB_ID_KEY, PersistentDataType.STRING, getMobId());
        dummy.getPersistentDataContainer().set(IS_DUMMY_KEY, PersistentDataType.BYTE, (byte) 1);

        dummy.customName(Component.text("§7[ §f허수아비 §7]"));
        dummy.setCustomNameVisible(true);
    }

    /**
     * [추가] 더미 몹의 패턴을 구현합니다.
     * 10칸 이내의 가장 가까운 플레이어를 바라보게 합니다.
     * @param entity 패턴을 실행할 몹 자신 (더미 몹)
     */
    @Override
    public void runPattern(LivingEntity entity) {
        // 주변 10칸 내의 플레이어들 중 가장 가까운 플레이어를 찾습니다.
        Player closestPlayer = entity.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(entity.getLocation()) <= 100) // 10칸(10*10=100) 이내
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(entity.getLocation())))
                .orElse(null); // 가장 가까운 플레이어가 없으면 null

        // 가장 가까운 플레이어가 있다면 그 방향으로 고개를 돌립니다.
        if (closestPlayer != null) {
            Location headLocation = entity.getEyeLocation();
            Location playerLocation = closestPlayer.getEyeLocation();

            // 플레이어를 바라보도록 방향(yaw, pitch) 설정
            double dx = playerLocation.getX() - headLocation.getX();
            double dy = playerLocation.getY() - headLocation.getY();
            double dz = playerLocation.getZ() - headLocation.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

            entity.setRotation(yaw, pitch);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // [수정] 허수아비 표식 키로 확인합니다.
        if (event.getEntity().getPersistentDataContainer().has(IS_DUMMY_KEY, PersistentDataType.BYTE)) {
            LivingEntity dummy = (LivingEntity) event.getEntity();
            dummy.setHealth(Objects.requireNonNull(dummy.getAttribute(Attribute.MAX_HEALTH)).getValue());
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        // [수정] 허수아비 표식 키로 확인합니다.
        if (event.getEntity().getPersistentDataContainer().has(IS_DUMMY_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}