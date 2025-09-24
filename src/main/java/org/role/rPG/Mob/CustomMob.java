package org.role.rPG.Mob;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;

// 모든 몹 클래스가 이 설계도를 따라야 함
public interface CustomMob extends Listener {

    // 몹을 식별할 고유 ID (예: "dummy", "goblin")
    String getMobId();

    // 몹이 드롭할 경험치
    double getProficiencyExp();

    int getMobLevel();

    // 몹을 소환하는 기능
    void spawn(Location location);

    /**
     * [추가] 몹의 고유 패턴을 실행합니다.
     * 이 메소드는 일정 시간마다 MobPatternRunnable에 의해 호출됩니다.
     * @param entity 패턴을 실행할 몹 자신 (LivingEntity)
     */
    void runPattern(LivingEntity entity);
}