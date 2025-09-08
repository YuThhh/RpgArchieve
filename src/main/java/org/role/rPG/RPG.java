package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;

public final class RPG extends JavaPlugin implements CommandExecutor {

    public static NamespacedKey SUCHECK_VALUE_KEY;

    @Override
    public void onEnable() {
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");

        // 각 기능 클래스의 register 메소드를 호출하여 시스템을 활성화합니다.
        Ui.register(this);
        Cash.register(this);
        CommandManager.register(this);

        getLogger().info("RPG Plugin has been enabled!");
        // 데이터 관리자 초기화
        new PER_DATA();

        // '담당자'들에게 등록 작업을 지시
        new CMD_MANAGER(this).registerCommands();
        new LIS_MANAGER(this).registerListeners();

        getLogger().info("RPG Plugin Enabled.");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("RPG Plugin Disabled.");
    }
}