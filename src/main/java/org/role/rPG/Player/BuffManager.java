package org.role.rPG.Player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.role.rPG.RPG;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuffManager {

    private final RPG plugin;
    private final StatManager statManager;
    // <플레이어 UUID, <버프 고유 ID, 버프 객체>>
    private final Map<UUID, Map<String, Buff>> activeBuffs = new ConcurrentHashMap<>();

    public BuffManager(RPG plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
        startBuffTicker();
    }

    // 1초마다 모든 플레이어의 버프를 확인하고 만료시키는 타이머
    private void startBuffTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerUUID : activeBuffs.keySet()) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player == null || !player.isOnline()) {
                        activeBuffs.remove(playerUUID); // 오프라인 플레이어 버프 제거
                        continue;
                    }

                    boolean needsUpdate = false;
                    Map<String, Buff> playerBuffs = activeBuffs.get(playerUUID);

                    // 만료된 버프를 찾아서 제거
                    Iterator<Map.Entry<String, Buff>> iterator = playerBuffs.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Buff buff = iterator.next().getValue();
                        buff.tickDown();
                        if (buff.getDurationTicks() <= 0) {
                            iterator.remove();
                            needsUpdate = true;
                        }
                    }

                    // 버프가 하나라도 제거되었다면 스탯을 다시 계산
                    if (needsUpdate) {
                        statManager.updatePlayerStats(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // 1틱마다 실행
    }

    public void addBuff(Player player, Buff newBuff) {
        UUID uuid = player.getUniqueId();
        activeBuffs.putIfAbsent(uuid, new ConcurrentHashMap<>());

        // 이미 같은 종류의 버프가 있다면 지속시간만 갱신
        activeBuffs.get(uuid).put(newBuff.getStat(), newBuff);

        // 버프가 추가되었으므로 즉시 스탯 업데이트
        statManager.updatePlayerStats(player);
    }

    // StatManager가 호출할 메서드
    public Map<String, Double> getStatBuffsForPlayer(UUID uuid) {
        Map<String, Double> statModifiers = new HashMap<>();
        if (activeBuffs.containsKey(uuid)) {
            for (Buff buff : activeBuffs.get(uuid).values()) {
                statModifiers.merge(buff.getStat(), buff.getValue(), Double::sum);
            }
        }
        return statModifiers;
    }
}