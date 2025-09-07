package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class RPG extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new Ui(), this);
        pm.registerEvents(new Cash(), this);

        Ui.startActionBarUpdater(this);
        Ui.startScoreboardUpdater(this);

        getLogger().info("RPG Plugin Enabled.");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("RPG Plugin Disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        if (commandName.equals("sucheck") || commandName.equals("devsucheck")) {
            if (args.length != 1) {
                player.sendMessage("§c사용법: /" + label + " <금액>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[0]);

                if (amount <= 0) {
                    player.sendMessage("§c금액은 1 이상의 숫자여야 합니다.");
                    return true;
                }

                if (commandName.equals("sucheck")) {
                    int playerMoney = Cash.getMoney(player);
                    if (playerMoney < amount) {
                        player.sendMessage("§c돈이 부족합니다. (현재 소지금: " + String.format("%,d", playerMoney) + "G)");
                        return true;
                    }
                    Cash.removeMoney(player, amount);
                }

                ItemStack sucheckItem = new ItemStack(Material.IRON_NUGGET, 1);
                ItemMeta meta = sucheckItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6" + String.format("%,d", amount) + "G");

                    // [수정됨] 아이템 설명 문구를 더 명확하게 변경합니다.
                    meta.setLore(Arrays.asList(
                            "§7우클릭하여 사용하세요.",
                            "§7쉬프트+우클릭 시 같은 금액의 수표를 모두 사용합니다."
                    ));

                    sucheckItem.setItemMeta(meta);
                }

                player.getInventory().addItem(sucheckItem);
                player.sendMessage("§e" + String.format("%,d", amount) + "G§f 수표를 발행했습니다!");
                return true;

            } catch (NumberFormatException e) {
                player.sendMessage("§c금액은 숫자로 입력해야 합니다.");
                return true;
            }
        }
        return false;
    }
}