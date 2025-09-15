package org.role.rPG;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class PER_DATA{

    private  static PER_DATA instance;

    private final Map<UUID, String> lastUiMap = new HashMap<>(); // GUI 뒤로가기 창
    private final Map<UUID,ItemStack[]> p_storage = new HashMap<>(); // GUI 창고 스토리지
    private final Map<UUID,Double> playerHealth = new HashMap<>(); // 체력 스탯
    private final Map<UUID, Double> playerHpRegenaration = new HashMap<>();
    private final Map<UUID, Double> playerDefense = new HashMap<>(); // 방어력 스탯
    private final Map<UUID, Double> playerCrit = new HashMap<>(); // 크리티컬 스탯
    private final Map<UUID, Double> playerCritDamage = new HashMap<>(); // 크리티컬 대미지 스탯
    private final Map<UUID, Double> playerStrength = new HashMap<>(); //힘 스탯
    private final Map<UUID, Double> playerAttackSpeed = new HashMap<>(); // 공격 속도 스탯
    private final Map<UUID, Float> playerSpeed = new HashMap<>(); // 이동 속도 스탯
    private final Map<UUID, Double> playerMaxMana = new HashMap<>(); // 최대 마나
    private final Map<UUID, Double> playerCurrentMana = new HashMap<>(); // 현재 마나

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

    public double getPlayerHealth(UUID playerUUID) {
        return playerHealth.getOrDefault(playerUUID, 100.0);
    }

    public void setPlayerHealth(UUID playerUUID, double health) {
        playerHealth.put(playerUUID, health);
    }

    public double getPlayerHpRegenaration(UUID playerUUID) {
        return playerHpRegenaration.getOrDefault(playerUUID, 100.0);
    }

    public void setPlayerHpRegenaration(UUID playerUUID, double hpRegenaration) {
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

    public void removePlayerDefense(UUID playerUUID) {
        playerDefense.remove(playerUUID);
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

    public double getPlayerSpeed(UUID playerUUID) {
        return playerSpeed.getOrDefault(playerUUID, 0.2f);
    }

    public void setPlayerSpeed(UUID playerUUID, float speed) {
        playerSpeed.put(playerUUID, speed);
    }
}
