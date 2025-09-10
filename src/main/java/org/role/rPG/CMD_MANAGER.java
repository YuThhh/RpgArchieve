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

public class CMD_MANAGER implements CommandExecutor {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final JavaPlugin plugin;

    public CMD_MANAGER(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    // 명령어 등록
    public void registerCommands() {
        Objects.requireNonNull(plugin.getCommand("메뉴")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setdef")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setcrit")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setcritdmg")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("sethp")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. "메뉴" 명령어 처리
        if (command.getName().equalsIgnoreCase("메뉴")) {
            // 이 명령어는 플레이어만 사용해야 하므로, 먼저 플레이어인지 확인합니다.
            if (!(sender instanceof Player player)) {
                sender.sendMessage(mm.deserialize("<red>이 명령어는 플레이어만 사용할 수 있습니다."));
                return true; // 명령어 실행은 성공적으로 끝났으므로 true 반환
            }

            new Menu_UI().openInventory(player);
            PER_DATA.getInstance().setLastUi(player.getUniqueId(), "none");
            return true;
        }

        // 2. "setdef" 명령어 처리
        else if (command.getName().equalsIgnoreCase("setdef")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /setdef <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double defense = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerDefense(target.getUniqueId(), defense);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 방어력을 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), defense);
                String targetMessage = String.format("<aqua>당신의 방어력이 <white>%.1f</white>로 설정되었습니다.</aqua>", defense);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>방어력은 숫자여야 합니다."));
            }
            return true;

        } else if (command.getName().equalsIgnoreCase("setcrit")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /setdef <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double crit = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerCrit(target.getUniqueId(), crit);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 크리티컬을 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), crit);
                String targetMessage = String.format("<aqua>당신의 크리티컬이 <white>%.1f</white>로 설정되었습니다.</aqua>", crit);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>크리티컬은 숫자여야 합니다."));
            }
            return true;

        } else if (command.getName().equalsIgnoreCase("setcritdmg")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /setdef <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double critdmg = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerCritDamage(target.getUniqueId(), critdmg);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 크리티컬 피해를 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), critdmg);
                String targetMessage = String.format("<aqua>당신의 크리티컬 피해가 <white>%.1f</white>로 설정되었습니다.</aqua>", critdmg);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>크리티컬 피해는 숫자여야 합니다."));
            }
            return true;

        } else if (command.getName().equalsIgnoreCase("sethp")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /sethp <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double hp = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerHealth(target.getUniqueId(), hp);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 크리티컬 피해를 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), hp);
                String targetMessage = String.format("<aqua>당신의 크리티컬 피해가 <white>%.1f</white>로 설정되었습니다.</aqua>", hp);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>체력은 숫자여야 합니다."));
            }
            return true;

        }

        return false;
    }
}
