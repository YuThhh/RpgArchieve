package org.role.rPG;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class PER_DATA{

    private  static PER_DATA instance;

    private final Map<UUID, String> lastUiMap = new HashMap<>();
    private final Map<UUID,ItemStack[]> p_storage = new HashMap<>();

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

}
