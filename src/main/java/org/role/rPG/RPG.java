package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class RPG extends JavaPlugin {

    public static NamespacedKey SUCHECK_VALUE_KEY;

    @Override
    public void onEnable() {
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");

        // 각 기능 클래스의 register 메소드를 호출하여 시스템을 활성화합니다.
        Ui.register(this);
        Cash.register(this);
        CommandManager.register(this);

        getLogger().info("RPG Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("RPG Plugin Disabled.");
    }
}