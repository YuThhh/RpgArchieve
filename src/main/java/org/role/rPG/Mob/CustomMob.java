package org.role.rPG.Mob;

import org.bukkit.Location;
import org.bukkit.event.Listener;

// 모든 몹 클래스가 이 설계도를 따라야 함
public interface CustomMob extends Listener {

    // 몹을 식별할 고유 ID (예: "dummy", "goblin")
    String getMobId();

    // 몹을 소환하는 기능
    void spawn(Location location);
}