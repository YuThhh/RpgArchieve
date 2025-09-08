package org.role.rPG;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;

public final class RPG extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        // 데이터 관리자 초기화
        new PER_DATA();

        // '담당자'들에게 등록 작업을 지시
        new CMD_MANAGER(this).registerCommands();
        new LIS_MANAGER(this).registerListeners();

        getLogger().info("RPG Plugin Enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
