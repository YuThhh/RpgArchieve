package org.role.rPG;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RPG extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        Objects.requireNonNull(this.getCommand("메뉴")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (sender instanceof Player player) {
            // UI 클래스의 새 인스턴스 생성
            GUI gui = new GUI();
            // GUI 열기
            gui.openInventory(player);
            return true;
        }
        return false;
    }
}
