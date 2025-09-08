package org.role.rPG;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public class CMD_MANAGER {

    private final JavaPlugin plugin;

    public CMD_MANAGER(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        // "/메뉴" 명령어의 담당자를 MenuCommand 클래스로 지정합니다.
        Objects.requireNonNull(plugin.getCommand("메뉴")).setExecutor(new COMMANDS());
    }
}
