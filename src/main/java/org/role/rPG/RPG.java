package org.role.rPG;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RPG extends JavaPlugin implements CommandExecutor {

    private MENU_UI menu;
    public static String last_ui;

    @Override
    public void onEnable() {

        new PER_DATA();
        // 명령어 등록
        Objects.requireNonNull(this.getCommand("메뉴")).setExecutor(this);

        // GUI 인스턴스 생성
        this.menu = new MENU_UI();

        // GUI 클래스가 Listener를 구현하도록 수정했으므로, 이제 등록 가능
        getServer().getPluginManager().registerEvents(this.menu, this);
        getServer().getPluginManager().registerEvents(new STORAGE_UI(null), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (sender instanceof Player player) {
            // 인스턴스 필드에 접근하여 GUI 열기
            this.menu.openInventory(player);
            last_ui = "none";
            return true;
        }
        return false;
    }
}
