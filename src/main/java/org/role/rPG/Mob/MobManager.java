package org.role.rPG.Mob;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MobManager {

    private final JavaPlugin plugin;
    // 모든 몹을 ID와 함께 저장하는 '등록소' 역할을 하는 Map
    private final Map<String, CustomMob> mobRegistry = new HashMap<>();

    public MobManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 새로운 몹을 MobManager에 등록합니다.
     * @param mob 등록할 CustomMob 객체
     */
    public void registerMob(CustomMob mob) {
        String mobId = mob.getMobId().toLowerCase();
        mobRegistry.put(mobId, mob);
        // 해당 몹이 가진 이벤트 리스너를 서버에 자동으로 등록
        plugin.getServer().getPluginManager().registerEvents(mob, plugin);
        plugin.getLogger().info("'" + mobId + "' 몹이 등록되었습니다.");
    }

    /**
     * mobId를 사용하여 등록된 몹을 소환합니다.
     * @param mobId 소환할 몹의 ID
     * @param location 소환할 위치
     */
    public void spawnMob(String mobId, Location location) {
        CustomMob mob = mobRegistry.get(mobId.toLowerCase());
        if (mob != null) {
            mob.spawn(location);
        } else {
            // 이 메시지는 플레이어에게 보내는 것이 더 좋습니다. 예시용 콘솔 출력.
            plugin.getLogger().warning(mobId + " ID를 가진 몹을 찾을 수 없습니다.");
        }
    }

    /**
     * 등록된 모든 몹의 ID 목록을 반환합니다. (TabCompleter를 위해)
     * @return 몹 ID 목록
     */
    public Set<String> getAllMobIds() {
        return mobRegistry.keySet();
    }

    /**
     * 월드에 있는 Entity가 등록된 CustomMob인지 확인하고, 맞다면 해당 CustomMob 객체를 반환합니다.
     * @param entity 확인할 Entity
     * @return CustomMob 객체. 커스텀 몹이 아니면 null을 반환합니다.
     */
    public CustomMob getCustomMob(Entity entity) {
        // 엔티티의 데이터 컨테이너에서 커스텀 몹 ID를 가져옵니다.
        String mobId = entity.getPersistentDataContainer().get(DummyMob.CUSTOM_MOB_ID_KEY, PersistentDataType.STRING);

        if (mobId != null) {
            // mobId가 존재하면, 등록소(mobRegistry)에서 해당 ID를 가진 CustomMob 객체를 찾아 반환합니다.
            return mobRegistry.get(mobId.toLowerCase());
        }
        // 커스텀 몹이 아니면 null을 반환합니다.
        return null;
    }

    // 참고: 범용 제거 기능은 모든 몹이 동일한 식별 태그를 가져야 하므로,
    // 추후 더 복잡한 시스템(예: 모든 몹에 고유 태그 부여)을 구현할 때 추가하는 것이 좋습니다.
    // 지금은 /더미제거 기능만 남겨두겠습니다.
    public int removeAllDummies(World world) {
        int removedCount = 0;
        for (Entity entity : world.getEntities()) {
            if (entity.getPersistentDataContainer().has(DummyMob.IS_DUMMY_KEY, PersistentDataType.BYTE)) {
                entity.remove();
                removedCount++;
            }
        }
        return removedCount;
    }
}