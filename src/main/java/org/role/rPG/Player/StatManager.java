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
        base.put("SPEED", (double) perData.getPlayerSpeed(uuid));

        // 레벨에 따른 추가 스탯 보너스를 계산하여 합산합니다.
        int mainLevel = perData.getPlayerLevel(uuid);
        int meleeProficiency = perData.getProficiencyLevel(uuid, PER_DATA.MELEE_COMBAT_PROFICIENCY);
        int miningProficiency = perData.getProficiencyLevel(uuid, PER_DATA.MINING_PROFICIENCY);

        // 예시: 1 레벨당 최대 체력 5, 1 전투 숙련도당 힘 0.5, 1 채광 숙련도당 방어력 0.2 증가
        if (mainLevel > 1) {
            // 예시: 1 레벨당 최대 체력 5, 1 전투 숙련도당 힘 0.5, 1 채광 숙련도당 방어력 0.2 증가
            base.merge("MAX_HEALTH", (mainLevel - 1) * 5.0, Double::sum);
        }
        base.merge("STRENGTH", meleeProficiency * 0.5, Double::sum);
        base.merge("DEFENSE", miningProficiency * 0.2, Double::sum);

        return base;
    }

    // [수정됨] 아이템 종류(무기/방어구)를 구분하는 새로운 로직
    private Map<String, Double> calculateEquipmentStats(Player player) {
        Map<String, Double> equipStats = new HashMap<>();

        // 1. 갑옷 슬롯에 '착용'한 아이템들의 스탯을 모두 합산합니다.
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null && !armorPiece.getType().isAir()) {
                Map<String, Double> itemStats = itemManager.getStatsFromItem(armorPiece);
                itemStats.forEach((stat, value) -> equipStats.merge(stat, value, Double::sum));
            }
        }

        // 2. '손에 든' 아이템의 스탯을 합산합니다.
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.getType().isAir()) {
            // 아이템의 종류를 확인합니다.
            String typeName = heldItem.getType().name();
            boolean isArmorPiece = typeName.endsWith("_HELMET") ||
                    typeName.endsWith("_CHESTPLATE") ||
                    typeName.endsWith("_LEGGINGS") ||
                    typeName.endsWith("_BOOTS");

            // [핵심] 손에 든 아이템이 '방어구'가 아닐 경우에만 스탯을 적용합니다.
            if (!isArmorPiece) {
                Map<String, Double> itemStats = itemManager.getStatsFromItem(heldItem);
                itemStats.forEach((stat, value) -> equipStats.merge(stat, value, Double::sum));
            }
        }

        return equipStats;
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

    /**
     * 플레이어의 현재 마나를 업데이트하고 캐시를 갱신합니다.
     * (실시간으로 변하는 스탯을 위한 메서드)
     * @param uuid 플레이어 UUID
     * @param newMana 새로운 마나 값
     */
    public void updatePlayerCurrentMana(UUID uuid, double newMana) {
        // 1. PER_DATA의 실제 데이터 업데이트
        perData.setPlayerCurrentMana(uuid, newMana);

        // 2. 캐시에 플레이어의 스탯 맵이 있는지 확인하고, 있다면 마나 값 갱신
        Map<String, Double> finalStats = finalStatsCache.get(uuid);
        if (finalStats != null) {
            finalStats.put("CURRENT_MANA", newMana);
        }
    }
}