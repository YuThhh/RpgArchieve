package org.role.rPG.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PER_DATA{

    private static PER_DATA instance;
    private static JavaPlugin plugin;

    private final Map<UUID, String> lastUiMap = new HashMap<>(); // GUI 뒤로가기 창
    private final Map<UUID,ItemStack[]> p_storage = new HashMap<>(); // GUI 창고 스토리지

    private final Map<UUID,Double> playerMaxHealth = new HashMap<>(); // 체력 스탯
    private final Map<UUID, Double> playerHpRegenaration = new HashMap<>();
    private final Map<UUID, Double> playerDefense = new HashMap<>(); // 방어력 스탯

    private final Map<UUID, Double> playerCrit = new HashMap<>(); // 크리티컬 스탯
    private final Map<UUID, Double> playerCritDamage = new HashMap<>(); // 크리티컬 대미지 스탯
    private final Map<UUID, Double> playerStrength = new HashMap<>(); //힘 스탯
    private final Map<UUID, Double> playerAttackSpeed = new HashMap<>(); // 공격 속도 스탯

    private final Map<UUID, Float> playerSpeed = new HashMap<>(); // 이동 속도 스탯

    private final Map<UUID, Double> playerMaxMana = new HashMap<>(); // 최대 마나
    private final Map<UUID, Double> playerCurrentMana = new HashMap<>(); // 현재 마나

    private final Map<UUID, Integer> playerLevel = new HashMap<>(); // 메인 레벨
    private final Map<UUID, Double> playerExperience = new HashMap<>(); // 메인 경험치

    private final Map<UUID, Map<String, Integer>> playerProficiencyLevel = new HashMap<>(); // 숙련도 레벨
    private final Map<UUID, Map<String, Double>> playerProficiencyExperience = new HashMap<>(); // 숙련도 경험치
    // 숙련도 종류
    public static final String MELEE_COMBAT_PROFICIENCY = "MELEE_COMBAT";
    public static final String RANGED_COMBAT_PROFICIENCY = "RANGED_COMBAT";
    public static final String MINING_PROFICIENCY = "MINING";
    public static final String MAGIC_PROFICIENCY = "MAGIC";
    public static final String TIMBER_PROFICIENCY = "WOODCUTTING";
    public static final String GATHERING_PROFICIENCY = "GATHERING";

    // 생성자: new PER_DATA()를 할 때 instance에 자기 자신을 저장합니다.
    public PER_DATA() {
        instance = this;
    }

    public static PER_DATA getInstance() {
        return instance;
    }

    public ItemStack[] getPlayerStorage(UUID playerUUID) {
        // playerStorages 맵에서 플레이어의 UUID로 데이터를 찾고,
        // 만약 데이터가 없으면(getOrDefault) 새로 54칸짜리 빈 배열을 만들어서 반환
        return p_storage.getOrDefault(playerUUID, new ItemStack[54]);
    }

    public void savePlayerStorage(UUID playerUUID, ItemStack[] items) {
        p_storage.put(playerUUID, items);
    }

    public void setLastUi(UUID playerUUID, String uiName) {
        lastUiMap.put(playerUUID, uiName);
    }

    public String getLastUi(UUID playerUUID) {
        return lastUiMap.getOrDefault(playerUUID, "none");
    }

    // 메인 레벨
    public int getPlayerLevel(UUID playerUUID) {
        return playerLevel.getOrDefault(playerUUID, 1); // 기본 레벨은 1
    }

    public void setPlayerLevel(UUID playerUUID, int level) {
        playerLevel.put(playerUUID, level);
    }

    // 메인 경험치
    public double getPlayerExperience(UUID playerUUID) {
        return playerExperience.getOrDefault(playerUUID, 0.0);
    }

    public void setPlayerExperience(UUID playerUUID, double experience) {
        playerExperience.put(playerUUID, experience);
    }

    // 숙련도 레벨
    public int getProficiencyLevel(UUID playerUUID, String proficiency) {
        return playerProficiencyLevel.getOrDefault(playerUUID, new HashMap<>()).getOrDefault(proficiency, 0); // 기본 숙련도 레벨은 0
    }

    public void setProficiencyLevel(UUID playerUUID, String proficiency, int level) {
        playerProficiencyLevel.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(proficiency, level);
    }

    // 숙련도 경험치
    public double getProficiencyExperience(UUID playerUUID, String proficiency) {
        return playerProficiencyExperience.getOrDefault(playerUUID, new HashMap<>()).getOrDefault(proficiency, 0.0);
    }

    public void setProficiencyExperience(UUID playerUUID, String proficiency, double experience) {
        playerProficiencyExperience.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(proficiency, experience);
    }

    // (추가) 숙련도 맵 전체를 가져오는 메서드 - 데이터 저장 시 필요
    public Map<String, Integer> getProficiencies(UUID playerUUID) {
        return playerProficiencyLevel.getOrDefault(playerUUID, new HashMap<>());
    }

    public double getplayerMaxHealth(UUID playerUUID) {
        return playerMaxHealth.getOrDefault(playerUUID, 100.0);
    }

    public void setplayerMaxHealth(UUID playerUUID, double health) {
        playerMaxHealth.put(playerUUID, health);
    }

    public double getPlayerHpRegenarationBonus(UUID playerUUID) {
        return playerHpRegenaration.getOrDefault(playerUUID, 0.0);
    }

    public void setPlayerHpRegenarationBonus(UUID playerUUID, double hpRegenaration) {
        playerHpRegenaration.put(playerUUID, hpRegenaration);
    }

    // --- 방어력 데이터 관리 메서드 추가 ---
    public double getPlayerDefense(UUID playerUUID) {
        // 방어력 데이터가 없으면 기본값 0.0 반환
        return playerDefense.getOrDefault(playerUUID, 0.0);
    }

    public double getPlayerMaxMana(UUID playerUUID) {
        return playerMaxMana.getOrDefault(playerUUID, 100.0);
    }

    public void setPlayerMaxMana(UUID playerUUID, double maxMana) {
        playerMaxMana.put(playerUUID, maxMana);
    }

    public double getPlayerCurrentMana(UUID playerUUID) {
        return playerCurrentMana.getOrDefault(playerUUID, 100.0);
    }

    public void setPlayerCurrentMana(UUID playerUUID, double currentMana) {
        playerCurrentMana.put(playerUUID, currentMana);
    }

    public void setPlayerDefense(UUID playerUUID, double defense) {
        playerDefense.put(playerUUID, defense);
    }

    public double getPlayerCrit(UUID playerUUID) {
        return playerCrit.getOrDefault(playerUUID, 0.0);
    }

    public void setPlayerCrit(UUID playerUUID, double cirt) {
        playerCrit.put(playerUUID, cirt);
    }

    public double getPlayerCritDamage(UUID playerUUID) {
        return playerCritDamage.getOrDefault(playerUUID, 50.0);
    }

    public void setPlayerCritDamage(UUID playerUUID, double critDamage) {
        playerCritDamage.put(playerUUID, critDamage);
    }

    public double getPlayerStrength(UUID playerUUID) {
        return playerStrength.getOrDefault(playerUUID, 0.0);
    }

    public void setPlayerStrength(UUID playerUUID, double strength) {
        playerStrength.put(playerUUID, strength);
    }

    public double getPlayerAttactSpeed(UUID playerUUID) {
        return playerAttackSpeed.getOrDefault(playerUUID, 0.0);
    }

    public void setPlayerAttackSpeed(UUID playerUUID, double attackspeed) {
        playerAttackSpeed.put(playerUUID, attackspeed);
    }

    public float getPlayerSpeed(UUID playerUUID) {
        return playerSpeed.getOrDefault(playerUUID, 100f);
    }

    public void setPlayerSpeed(UUID playerUUID, float speed) {
        playerSpeed.put(playerUUID, speed);
    }
}
