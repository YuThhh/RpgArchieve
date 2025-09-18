package org.role.rPG.Player;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.RPG;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StatManager {

    private final RPG plugin;
    private final ItemManager itemManager;
    private final PER_DATA perData;

    // 플레이어별 최종 스탯을 임시 저장하는 맵 (서버 재시작 시 사라짐)
    private final Map<UUID, Map<String, Double>> finalStatsCache = new HashMap<>();

    public StatManager(RPG plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.perData = PER_DATA.getInstance();
    }

    /**
     * 플레이어의 최종 스탯을 다시 계산하고 적용합니다.
     * @param player 스탯을 업데이트할 플레이어
     */
    public void updatePlayerStats(Player player) {
        UUID uuid = player.getUniqueId();

        // 1. 기본 스탯 가져오기
        Map<String, Double> baseStats = getBaseStats(uuid);

        // 2. 장비 스탯 계산하기
        Map<String, Double> equipStats = calculateEquipmentStats(player);

        // 3. 기본 스탯과 장비 스탯을 합산하여 최종 스탯 계산
        Map<String, Double> finalStats = new HashMap<>(baseStats);
        equipStats.forEach((stat, value) -> finalStats.merge(stat, value, Double::sum));

        // 4. 계산된 최종 스탯을 캐시에 저장
        finalStatsCache.put(uuid, finalStats);

        // 5. 최종 스탯을 실제 게임에 적용
        applyStatsToPlayer(player, finalStats);
    }

    /**
     * 특정 스탯의 최종 계산값을 가져옵니다. (Stat 클래스에서 사용)
     * @param uuid 플레이어 UUID
     * @param statName 스탯 이름 (예: "STRENGTH")
     * @return 계산된 최종 스탯 값
     */
    public double getFinalStat(UUID uuid, String statName) {
        Map<String, Double> finalStats = finalStatsCache.get(uuid);
        // 캐시에 없으면 기본 스탯을 반환 (로그인 직후 등)
        return Objects.requireNonNullElseGet(finalStats, () -> getBaseStats(uuid)).getOrDefault(statName, 0.0);
    }

    // PER_DATA에서 플레이어의 기본 스탯을 Map 형태로 가져옵니다.
    private Map<String, Double> getBaseStats(UUID uuid) {
        Map<String, Double> base = new HashMap<>();
        base.put("MAX_HEALTH", perData.getplayerMaxHealth(uuid));
        base.put("DEFENSE", perData.getPlayerDefense(uuid));
        base.put("STRENGTH", perData.getPlayerStrength(uuid));
        base.put("CRIT_CHANCE", perData.getPlayerCrit(uuid));
        base.put("CRIT_DAMAGE", perData.getPlayerCritDamage(uuid));
        base.put("ATTACK_SPEED", perData.getPlayerAttactSpeed(uuid));
        base.put("MAX_MANA", perData.getPlayerMaxMana(uuid));
        base.put("CURRENT_MANA", perData.getPlayerCurrentMana(uuid));
        // ▼▼▼ [추가] 누락된 스탯 추가 ▼▼▼
        base.put("SPEED", (double) perData.getPlayerSpeed(uuid));
        return base;
    }

    // 플레이어의 장비(손 + 방어구)를 모두 확인하여 장비 스탯 총합을 계산합니다.
    private Map<String, Double> calculateEquipmentStats(Player player) {
        Map<String, Double> equipStats = new HashMap<>();

        // 갑옷 슬롯 아이템 확인
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            addStatsFromItem(equipStats, armor);
        }
        // 메인 핸드 아이템 확인
        addStatsFromItem(equipStats, player.getInventory().getItemInMainHand());

        return equipStats;
    }

    // 아이템 하나에서 스탯을 추출하여 맵에 더합니다.
    private void addStatsFromItem(Map<String, Double> stats, ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        // ItemManager를 통해 아이템의 스탯 맵을 가져옵니다.
        Map<String, Double> itemStats = itemManager.getStatsFromItem(item);
        itemStats.forEach((stat, value) -> stats.merge(stat, value, Double::sum));
    }

    // 계산된 최종 스탯을 플레이어의 Bukkit Attribute에 적용합니다.
    private void applyStatsToPlayer(Player player, Map<String, Double> finalStats) {
        // 최대 체력 적용
        double maxHealth = finalStats.getOrDefault("MAX_HEALTH", 100.0);
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(maxHealth);

        // 이동 속도 적용 (기존 startStartUpdater의 로직을 가져옴)
        double speedStat = finalStats.getOrDefault("SPEED", 100.0); // ItemManager에 SPEED 파싱 추가 필요
        float calculatedSpeed = 0.2f * (float)(speedStat / 100.0); // 기본값 0.2, 스탯 100당 0.1 증가
        player.setWalkSpeed(calculatedSpeed);
    }
}