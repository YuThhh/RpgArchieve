package org.role.rPG;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

public class LIS_manager {

    private final JavaPlugin plugin;
    private final PluginManager pm;

    public LIS_manager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }

    public void registerListeners() {
        pm.registerEvents(new Menu_UI(), plugin);
        pm.registerEvents(new Storage_UI(null), plugin);
    }
}
