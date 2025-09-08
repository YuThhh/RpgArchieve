package org.role.rPG;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CommandManager implements CommandExecutor {

    // [추가됨] /myinfo 명령어를 처리하기 위한 Ui 클래스의 인스턴스
    private final Ui myInfoGUI;

    /**
     * 생성자: CommandManager가 생성될 때, GUI를 열기 위한 Ui 인스턴스를 함께 생성합니다.
     */
    public CommandManager() {
        this.myInfoGUI = new Ui();
    }

    /**
     * CommandManager를 서버에 명령어 처리 담당자로 등록합니다.
     */
    public static void register(JavaPlugin plugin) {
        CommandManager executor = new CommandManager();
        plugin.getCommand("myinfo").setExecutor(executor);
        plugin.getCommand("sucheck").setExecutor(executor);
        plugin.getCommand("devsucheck").setExecutor(executor);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        // [추가됨] /myinfo 명령어 처리 로직
        if (commandName.equals("myinfo")) {
            myInfoGUI.open(player);
            return true;
        }

        if (commandName.equals("sucheck") || commandName.equals("devsucheck")) {
            if (args.length == 0 || args.length > 2) {
                player.sendMessage("§c사용법: /" + label + " <금액> [개수]");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[0]);
                int count = (args.length == 2) ? Integer.parseInt(args[1]) : 1;

                if (amount <= 0 || count <= 0 || count > 64) {
                    player.sendMessage("§c금액과 개수는 올바른 범위의 양수여야 합니다.");
                    return true;
                }

                if (commandName.equals("sucheck")) {
                    long totalCost = (long) amount * count;
                    if (totalCost > Integer.MAX_VALUE) {
                        player.sendMessage("§c총액이 너무 커서 처리할 수 없습니다.");
                        return true;
                    }
                    int playerMoney = Cash.getMoney(player);
                    if (playerMoney < totalCost) {
                        player.sendMessage("§c돈이 부족합니다. (필요 금액: " + String.format("%,d", totalCost) + "G)");
                        return true;
                    }
                    Cash.removeMoney(player, (int) totalCost);
                }

                ItemStack sucheckItem = new ItemStack(Material.IRON_NUGGET, count);
                ItemMeta meta = sucheckItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6" + String.format("%,d", amount) + "G");
                    meta.setLore(Arrays.asList(
                            "§7우클릭하여 사용하세요.",
                            "§7쉬프트+우클릭 시 같은 금액의 수표를 모두 사용합니다."
                    ));
                    meta.getPersistentDataContainer().set(RPG.SUCHECK_VALUE_KEY, PersistentDataType.INTEGER, amount);
                    sucheckItem.setItemMeta(meta);
                }
                player.getInventory().addItem(sucheckItem);
                player.sendMessage("§e" + String.format("%,d", amount) + "G§f 수표 §a" + count + "장§f을 발행했습니다!");
                return true;

            } catch (NumberFormatException e) {
                player.sendMessage("§c금액과 개수는 숫자로 입력해야 합니다.");
                return true;
            }
        }
        return false;
    }
}