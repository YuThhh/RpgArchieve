package org.role.rPG.Mob;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
// [변경] Listener -> CustomMob 으로 변경
// Listener는 CustomMob이 상속하므로 별도로 쓸 필요가 없습니다.
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

// [변경] CustomMob 인터페이스를 구현(implements)하도록 수정
public class DummyMob implements CustomMob {

    private final JavaPlugin plugin;
    public static final NamespacedKey DUMMY_MOB_KEY;

    static {
        DUMMY_MOB_KEY = new NamespacedKey("rpg", "dummy_mob");
    }

    public DummyMob(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // [추가] CustomMob 인터페이스가 요구하는 getMobId 메소드 구현
    @Override
    public String getMobId() {
        return "dummy"; // 이 몹의 고유 ID는 "dummy" 입니다.
    }

    @Override
    public void spawn(Location location) {
        LivingEntity dummy = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.CREEPER);

        dummy.setAI(false);
        dummy.setGravity(false);
        dummy.setInvulnerable(true);
        dummy.setSilent(true);
        dummy.setCollidable(false);

        Objects.requireNonNull(dummy.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2048);

        dummy.getPersistentDataContainer().set(DUMMY_MOB_KEY, PersistentDataType.BYTE, (byte) 1);
        dummy.customName(Component.text("§7[ §f허수아비 §7]"));
        dummy.setCustomNameVisible(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(DUMMY_MOB_KEY, PersistentDataType.BYTE)) {
            LivingEntity dummy = (LivingEntity) event.getEntity();
            dummy.setHealth(Objects.requireNonNull(dummy.getAttribute(Attribute.MAX_HEALTH)).getValue());
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(DUMMY_MOB_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}