package org.role.rPG.Mob;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public class MobDeathListener implements Listener {

    private final MobManager mobManager;

    public MobDeathListener(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        // MobManager를 통해 죽은 엔티티가 커스텀 몹인지 확인합니다.
        CustomMob customMob = mobManager.getCustomMob(entity);

        // 커스텀 몹이 아니라면, 아무 작업도 하지 않습니다.
        if (customMob == null) {
            return;
        }

        // 기본 드랍템 (예: 슬라임의 슬라임 볼)을 모두 제거합니다.
        event.getDrops().clear();

        // 커스텀 몹의 드랍 테이블을 가져옵니다.
        List<MobDrop> customDrops = customMob.getDrops();
        Location loc = entity.getLocation();

        // 각 드랍 아이템에 대해 확률 계산을 수행합니다.
        for (MobDrop drop : customDrops) {
            // Math.random()은 0.0 이상 1.0 미만의 값을 반환합니다.
            if (Math.random() <= drop.getChance()) {
                // 확률에 당첨되면 아이템을 드랍합니다.
                loc.getWorld().dropItemNaturally(loc, drop.getItem());
            }
        }
    }
}