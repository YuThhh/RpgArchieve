package org.role.rPG;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class COMMANDS implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            // 메뉴 UI를 새로 생성해서 열어줍니다.
            new MENU_UI().openInventory(player);
            // 이제 안전한 방식으로 last_ui를 설정합니다.
            PER_DATA.getInstance().setLastUi(player.getUniqueId(), "none");
            return true;
        }
        sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
        return false;
    }

}
