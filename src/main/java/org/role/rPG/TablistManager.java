package org.role.rPG;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TablistManager {

    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, List<UUID>> fakePlayerMap = new HashMap<>(); // <실제 플레이어, 생성된 가짜 플레이어 목록>
    private final Logger logger;

    // [수정] 최신 버전에 필요한 PlayerInfoAction EnumSet을 미리 정의합니다.
    private final EnumSet<EnumWrappers.PlayerInfoAction> ADD_PLAYER_ACTIONS = EnumSet.of(
            EnumWrappers.PlayerInfoAction.ADD_PLAYER,
            EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
            EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
            EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE
    );

    public TablistManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.logger = plugin.getLogger();
    }

    public void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!fakePlayerMap.containsKey(player.getUniqueId())) {
                        createFakePlayers(player);
                    } else { // [개선] else를 사용하여 불필요한 중복 업데이트 방지
                        updateFakePlayers(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다 업데이트
    }

    public void createFakePlayers(Player player) {
        List<UUID> fakePlayerUUIDs = new ArrayList<>();
        List<PlayerInfoData> newPlayersData = new ArrayList<>();

        for (int i = 0; i < 80; i++) { // 탭리스트 슬롯 최대 80개
            UUID fakeUUID = UUID.randomUUID();
            // [수정] 이름은 16자를 넘지 않도록 하고, 고유성을 위해 UUID 일부를 사용
            String fakeName = "§0" + UUID.randomUUID().toString().substring(0, 14);
            WrappedGameProfile gameProfile = new WrappedGameProfile(fakeUUID, fakeName);

            WrappedChatComponent displayName = WrappedChatComponent.fromText("");

            // [수정] 최신 PlayerInfoData 생성자는 추가 인자(RemoteChatSessionData)가 필요할 수 있습니다. null로 처리합니다.
            newPlayersData.add(new PlayerInfoData(gameProfile, 1, EnumWrappers.NativeGameMode.SURVIVAL, displayName, null));
            fakePlayerUUIDs.add(fakeUUID);
        }

        // [수정] 새로운 방식으로 패킷 전송
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().write(0, ADD_PLAYER_ACTIONS);
        packet.getPlayerInfoDataLists().write(1, newPlayersData); // 인덱스가 1로 변경됨
        sendPacket(player, packet);

        fakePlayerMap.put(player.getUniqueId(), fakePlayerUUIDs);
    }

    public void updateFakePlayers(Player player) {
        List<UUID> fakePlayerUUIDs = fakePlayerMap.get(player.getUniqueId());
        if (fakePlayerUUIDs == null) return;

        List<PlayerInfoData> updatedPlayersData = new ArrayList<>();

        String serverName = "§e§lMY AWESOME SERVER";
        String onlinePlayers = "§f온라인: §a" + Bukkit.getOnlinePlayers().size() + "명";
        String serverTime = "§f서버 시간: §a" + new Date().toString(); // 날짜 포맷은 원하는 대로 변경

        updateLine(updatedPlayersData, fakePlayerUUIDs.get(0), serverName);
        updateLine(updatedPlayersData, fakePlayerUUIDs.get(1), onlinePlayers);
        updateLine(updatedPlayersData, fakePlayerUUIDs.get(2), serverTime);
        // ... 나머지 라인은 공백으로 채워 깔끔하게 보이도록 할 수 있습니다.
        for (int i = 3; i < fakePlayerUUIDs.size(); i++) {
            updateLine(updatedPlayersData, fakePlayerUUIDs.get(i), "");
        }


        // [수정] 업데이트 패킷 생성
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME));
        packet.getPlayerInfoDataLists().write(1, updatedPlayersData); // 인덱스가 1로 변경됨
        sendPacket(player, packet);
    }

    private void updateLine(List<PlayerInfoData> list, UUID uuid, String text) {
        WrappedGameProfile gameProfile = new WrappedGameProfile(uuid, "");
        WrappedChatComponent displayName = WrappedChatComponent.fromText(text);
        // [수정] 최신 PlayerInfoData 생성자 사용
        list.add(new PlayerInfoData(gameProfile, 1, EnumWrappers.NativeGameMode.SURVIVAL, displayName, null));
    }

    public void removeFakePlayers(Player player) {
        List<UUID> fakePlayerUUIDs = fakePlayerMap.remove(player.getUniqueId());
        if (fakePlayerUUIDs == null || fakePlayerUUIDs.isEmpty()) {
            return;
        }

        // [수정] 새로운 PLAYER_INFO_REMOVE 패킷을 생성하고 전송합니다. 이 방식이 맞습니다!
        PacketContainer removePacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        removePacket.getUUIDLists().write(0, fakePlayerUUIDs);
        sendPacket(player, removePacket);
    }

    // [수정] 패킷 전송 메소드를 PacketContainer를 직접 받도록 변경
    private void sendPacket(Player player, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "플레이어 정보 패킷 전송 중 오류가 발생했습니다!", e);
        }
    }
}