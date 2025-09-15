package org.role.rPG;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;

public class TablistManager {

    private final RPG plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, BukkitTask> playerTasks = new HashMap<>();
    private final List<PlayerInfoData> fakePlayers = new ArrayList<>();
    private final List<UUID> fakePlayerUUIDs = new ArrayList<>();

    private static final int TAB_LIST_SIZE = 80;

    public TablistManager(RPG plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        initializeFakePlayers();
    }

    private void initializeFakePlayers() {
        for (int i = 0; i < TAB_LIST_SIZE; i++) {
            UUID randomUUID = UUID.randomUUID();
            this.fakePlayerUUIDs.add(randomUUID);
            WrappedGameProfile gameProfile = new WrappedGameProfile(randomUUID, "cell_" + i);
            PlayerInfoData data = new PlayerInfoData(
                    gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText("")
            );
            fakePlayers.add(data);
        }
    }

    // ▼▼▼ [수정됨] 패킷 생성 방식을 변경하여 final 오류를 해결합니다. ▼▼▼
    public void setupPlayer(Player player) {
        // 패킷을 생성할 때부터 필요한 데이터를 모두 담아서 만듭니다.
        PacketContainer addPlayerPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        addPlayerPacket.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
        addPlayerPacket.getPlayerInfoDataLists().write(0, fakePlayers);

        protocolManager.sendServerPacket(player, addPlayerPacket);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                updateTabList(player);
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);

        playerTasks.put(player.getUniqueId(), task);
    }

    public void removePlayer(Player player) {
        BukkitTask task = playerTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        PacketContainer removePlayerPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        removePlayerPacket.getUUIDLists().write(0, fakePlayerUUIDs);
        protocolManager.sendServerPacket(player, removePlayerPacket);
    }

    // ▼▼▼ [수정됨] 패킷 생성 방식을 변경하여 final 오류를 해결합니다. ▼▼▼
    private void updateTabList(Player player) {
        List<PlayerInfoData> updatedData = new ArrayList<>();
        List<Component> content = getTablistContent(player);



        for (int i = 0; i < TAB_LIST_SIZE; i++) {
            UUID entryId = fakePlayerUUIDs.get(i);
            WrappedGameProfile profile = fakePlayers.get(i).getProfile();
            Component displayName = (i < content.size()) ? content.get(i) : Component.empty();

            String legacyText = LegacyComponentSerializer.legacySection().serialize(displayName);
            WrappedChatComponent chatComponent = WrappedChatComponent.fromLegacyText(legacyText);

            // Component를 직접 사용하는 생성자로 변경
            PlayerInfoData newData = new PlayerInfoData(
                    entryId, 0, false, EnumWrappers.NativeGameMode.SURVIVAL, profile, chatComponent
            );
            updatedData.add(newData);
        }

        // 패킷을 생성할 때부터 필요한 데이터를 모두 담아서 만듭니다.
        PacketContainer updatePacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        updatePacket.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME));
        updatePacket.getPlayerInfoDataLists().write(0, updatedData);

        protocolManager.sendServerPacket(player, updatePacket);
    }

    // 이 부분은 그대로 유지됩니다.
    private List<Component> getTablistContent(Player player) {
        List<Component> lines = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일");
        String date = sdf.format(new Date());

        lines.add(Component.text(""));
        lines.add(Component.text("MY-SERVER").color(NamedTextColor.YELLOW).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        lines.add(Component.text(""));
        lines.add(Component.text("플레이어: ").color(NamedTextColor.WHITE).append(Component.text(player.getName()).color(NamedTextColor.GREEN)));
        lines.add(Component.text("위치: ").color(NamedTextColor.WHITE).append(Component.text(player.getWorld().getName()).color(NamedTextColor.GREEN)));
        lines.add(Component.text(""));
        lines.add(Component.text(date).color(NamedTextColor.GRAY));
        lines.add(Component.text(""));

        return lines;
    }
}