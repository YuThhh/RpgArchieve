package org.role.rPG.Mob;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class MobPatternRunnable extends BukkitRunnable {

    private final MobManager mobManager;

    public MobPatternRunnable(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @Override
    public void run() {
        // 서버의 모든 월드를 순회합니다.
        for (World world : Bukkit.getWorlds()) {
            // 각 월드의 살아있는 엔티티(몹, 동물 등)들을 순회합니다.
            for (LivingEntity entity : world.getLivingEntities()) {
                // MobManager를 통해 해당 엔티티가 우리가 만든 커스텀 몹인지 확인합니다.
                CustomMob customMob = mobManager.getCustomMob(entity);

                // 커스텀 몹이 맞다면, 해당 몹의 runPattern 메소드를 실행합니다.
                if (customMob != null) {
                    try {
                        customMob.runPattern(entity);
                    } catch (Exception e) {
                        // 특정 몹 패턴에서 오류가 나도 다른 몹에 영향이 없도록 처리
                        System.err.println(customMob.getMobId() + " 몹의 패턴 실행 중 오류 발생:");
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}