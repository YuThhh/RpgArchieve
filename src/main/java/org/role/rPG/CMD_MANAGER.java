package org.role.rPG;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// CMD_MANAGER가 CommandExecutor의 역할도 겸하도록 implements를 추가합니다.
public class CMD_MANAGER implements CommandExecutor {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final JavaPlugin plugin;

    public CMD_MANAGER(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 메인 클래스에서 호출할 명령어 등록 메서드입니다.
     */
    public void registerCommands() {
        // "/메뉴" 명령어의 담당자를 다른 클래스가 아닌, '자기 자신(this)'으로 지정합니다.
        Objects.requireNonNull(plugin.getCommand("메뉴")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setdef")).setExecutor(this);
    }

    /**
     * 명령어가 실행될 때 호출되는 메서드입니다.
     * (기존 COMMANDS 클래스의 내용을 그대로 가져왔습니다)
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 이 클래스는 '/메뉴' 명령어에 대한 처리만 담당합니다.
        if (command.getName().equalsIgnoreCase("메뉴")) {
            if (sender instanceof Player player) {
                // 메뉴 UI를 새로 생성해서 열어줍니다.
                new MENU_UI().openInventory(player);
                // 안전한 방식으로 last_ui를 설정합니다.
                PER_DATA.getInstance().setLastUi(player.getUniqueId(), "none");
                return true;
            }
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return false;
        } else if (command.getName().equalsIgnoreCase("setdef")) {
            if (sender instanceof Player player) {

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                    return true;

                }

                try {
                    double defense = Double.parseDouble(args[1]);
                    // PER_DATA의 싱글턴 인스턴스에 직접 접근하여 데이터 수정
                    PER_DATA.getInstance().setPlayerDefense(target.getUniqueId(), defense);
                    sender.sendMessage(target.getName() + "님의 방어력을 " + defense + "로 설정했습니다.");
                    target.sendMessage("당신의 방어력이 " + defense + "로 설정되었습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("방어력은 숫자여야 합니다.");
                }

                return true;
            }
        }
        return false;
    }
}
