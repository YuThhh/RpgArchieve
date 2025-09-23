package org.role.rPG;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Item.ReforgeManager;
import org.role.rPG.Mob.MobManager;
import org.role.rPG.Player.StatManager;
import org.role.rPG.UI.Menu_UI;
import org.role.rPG.UI.Reforge_UI;

import java.util.*;
import java.util.stream.Collectors;

public class CMD_manager implements CommandExecutor, TabCompleter {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final ReforgeManager reforgeManager;
    private final StatManager statManager;
    private final MobManager mobManager;

    // 생성자를 5개의 인자를 받도록 수정
    public CMD_manager(JavaPlugin plugin, ItemManager itemManager, ReforgeManager reforgeManager, StatManager statManager, MobManager mobManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.reforgeManager = reforgeManager;
        this.statManager = statManager;
        this.mobManager = mobManager;
    }

    // 명령어 등록
    public void registerCommands() {
        // plugin.yml에 등록된 명령어들을 가져와 Executor와 TabCompleter를 설정
        List<String> commands = Arrays.asList("메뉴", "리포지", "rpgitem", "mob");
        for (String cmdName : commands) {
            if (plugin.getCommand(cmdName) != null) {
                Objects.requireNonNull(plugin.getCommand(cmdName)).setExecutor(this);
                Objects.requireNonNull(plugin.getCommand(cmdName)).setTabCompleter(this);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }

        String commandName = command.getName().toLowerCase();
        Player viewer = (Player) sender;

        switch (commandName) {
            case "메뉴":
                new Menu_UI(plugin, statManager, viewer).openInventory(player);
                break;
            case "리포지":
                // 필요할 때마다 Reforge_UI를 새로 생성하여 필요한 매니저들을 전달
                new Reforge_UI(itemManager, statManager, reforgeManager).openInventory(player);
                break;
            case "rpgitem":
                handleRpgItemCommand(player, args);
                break;
            case "mob":
                handleMobCommand(player, args);
                break;
            default:
                return false;
        }
        return true;
    }

    private void handleRpgItemCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("/rpgitem give <ID> - 아이템을 받습니다.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/rpgitem reload - Item.yml을 리로드합니다.", NamedTextColor.YELLOW));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length < 2) {
                    player.sendMessage(Component.text("아이템 ID를 입력해주세요.", NamedTextColor.RED));
                    player.sendMessage(Component.text("사용 가능: " + String.join(", ", itemManager.getAllItemIds()), NamedTextColor.GRAY));
                    return;
                }
                ItemStack item = itemManager.getItem(args[1]);
                if (item == null) {
                    player.sendMessage(Component.text("'" + args[1] + "' 아이템을 찾을 수 없습니다.", NamedTextColor.RED));
                    return;
                }
                player.getInventory().addItem(item);
                player.sendMessage(Component.text().append(item.displayName()).append(Component.text(" 아이템을 획득했습니다.", NamedTextColor.GREEN)));
                break;

            case "reload":
                if (!player.hasPermission("rpg.item.reload")) {
                    player.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
                    return;
                }
                itemManager.reloadItems();
                player.sendMessage(Component.text("Item.yml 설정을 리로드했습니다.", NamedTextColor.GREEN));
                break;

            default:
                player.sendMessage(Component.text("알 수 없는 명령어입니다.", NamedTextColor.RED));
                break;
        }
    }

    private void handleMobCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(mm.deserialize("<gold>사용법: /mob <spawn|remove_dummy> [mob_id]"));
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "spawn":
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize("<red>소환할 몹의 ID를 입력해주세요."));
                    player.sendMessage(mm.deserialize("<gray>사용 가능: " + String.join(", ", mobManager.getAllMobIds())));
                    return;
                }
                String mobId = args[1];
                mobManager.spawnMob(mobId, player.getLocation());
                player.sendMessage(mm.deserialize("<green>[알림] <white>" + mobId + " 몹을 소환했습니다."));
                break;

            case "remove_dummy":
                int removedCount = mobManager.removeAllDummies(player.getWorld());
                if (removedCount > 0) {
                    player.sendMessage(mm.deserialize("<green>[알림] <white>더미 몹 " + removedCount + "개를 제거했습니다."));
                } else {
                    player.sendMessage(mm.deserialize("<green>[알림] <white>제거할 더미 몹이 없습니다."));
                }
                break;

            default:
                player.sendMessage(mm.deserialize("<red>알 수 없는 동작입니다. <gray>spawn, remove_dummy만 가능합니다."));
                break;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        final List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("rpgitem")) {
            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "reload"), completions);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                StringUtil.copyPartialMatches(args[1], itemManager.getAllItemIds(), completions);
            }
        } else if (cmdName.equals("mob")) {
            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0], Arrays.asList("spawn", "remove_dummy"), completions);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
                StringUtil.copyPartialMatches(args[1], mobManager.getAllMobIds(), completions);
            }
        }

        // set<Stat> 명령어 자동완성 (만약 사용한다면)
        if (cmdName.startsWith("set")) {
            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0],
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
                        completions);
            }
        }

        return completions;
    }
}