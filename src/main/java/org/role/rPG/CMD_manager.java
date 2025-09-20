package org.role.rPG;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // TabCompleter import
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Mob.MobManager;
import org.role.rPG.Player.Cash;
import org.role.rPG.UI.Reforge_UI;

import java.util.*;
import java.util.stream.Collectors;

// [수정] TabCompleter 인터페이스를 구현(implements)합니다.
public class CMD_manager implements CommandExecutor, TabCompleter {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final Reforge_UI reforgeUI;
    private final MobManager mobManager;

    public CMD_manager(JavaPlugin plugin, ItemManager itemManager, Reforge_UI reforgeUI, MobManager mobManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.reforgeUI = reforgeUI;
        this.mobManager = mobManager;
    }

    // 명령어 등록
    public void registerCommands() {
        List<String> commands = Arrays.asList(
                "메뉴",  "rpgitem", "리포지", "mob"
        );

        for (String cmdName : commands) {
            Objects.requireNonNull(plugin.getCommand(cmdName)).setExecutor(this);
            // [수정] 모든 명령어에 TabCompleter를 설정합니다.
            Objects.requireNonNull(plugin.getCommand(cmdName)).setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // onCommand 메소드 내용은 그대로 유지됩니다.
        // ... (기존 onCommand 코드 생략) ...
        // 1. "메뉴" 명령어 처리

        if (command.getName().equalsIgnoreCase("rpgitem")) {
            if (args.length == 0) {
                sender.sendMessage(Component.text("/rpgitem give <ID> - 아이템을 받습니다.", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("/rpgitem reload - Item.yml을 리로드합니다.", NamedTextColor.YELLOW));
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "give":
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("플레이어만 사용 가능합니다.", NamedTextColor.RED));
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(Component.text("아이템 ID를 입력해주세요.", NamedTextColor.RED));
                        player.sendMessage(Component.text("사용 가능: " + String.join(", ", itemManager.getAllItemIds()), NamedTextColor.GRAY));
                        return true;
                    }
                    ItemStack item = itemManager.getItem(args[1]);
                    if (item == null) {
                        player.sendMessage(Component.text("'" + args[1] + "' 아이템을 찾을 수 없습니다.", NamedTextColor.RED));
                        return true;
                    }
                    player.getInventory().addItem(item);
                    player.sendMessage(Component.text().append(item.displayName()).append(Component.text(" 아이템을 획득했습니다.", NamedTextColor.GREEN)));
                    break;

                case "reload":
                    if (!sender.hasPermission("rpg.item.reload")) {
                        sender.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
                        return true;
                    }
                    itemManager.reloadItems();
                    sender.sendMessage(Component.text("Item.yml 설정을 리로드했습니다.", NamedTextColor.GREEN));
                    break;

                default:
                    sender.sendMessage(Component.text("알 수 없는 명령어입니다.", NamedTextColor.RED));
                    break;
            }
            return true;
        }
        else if (command.getName().equalsIgnoreCase("mob")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(mm.deserialize("<red>플레이어만 사용 가능합니다."));
                return true;
            }
            if (args.length == 0) {
                player.sendMessage(mm.deserialize("<gold>사용법: /mob <spawn|remove_dummy> [mob_id]"));
                return true;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "spawn":
                    if (args.length < 2) {
                        player.sendMessage(mm.deserialize("<red>소환할 몹의 ID를 입력해주세요."));
                        player.sendMessage(mm.deserialize("<gray>사용 가능: " + String.join(", ", mobManager.getAllMobIds())));
                        return true;
                    }
                    String mobId = args[1];
                    mobManager.spawnMob(mobId, player.getLocation());
                    player.sendMessage(mm.deserialize("<green>[알림] <white>" + mobId + " 몹을 소환했습니다."));
                    break;

                case "remove_dummy": // 아직 범용 제거 기능이 없으므로, 더미 제거 기능만 남김
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
            return true;
        }

        if (command.getName().equalsIgnoreCase("리포지")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("플레이어만 사용 가능합니다.", NamedTextColor.RED));
                return true;
            }
            reforgeUI.openInventory(player);
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

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
                    meta.displayName(Component.text(amount, NamedTextColor.YELLOW)
                            .append(Component.text("G", NamedTextColor.YELLOW)));

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("§7우클릭하여 사용하세요. \n", NamedTextColor.GRAY)
                            .append(Component.text("§7쉬프트+우클릭 시 같은 금액의 수표를 모두 사용합니다.", NamedTextColor.GRAY)));
                    meta.lore(lore);
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

    // [추가] onTabComplete 메소드 구현
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        final List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        // 'set'으로 시작하는 스탯 설정 명령어들을 일괄 처리
        if (cmdName.startsWith("set")) {
            // 첫 번째 인자(플레이어 이름) 자동 완성
            if (args.length == 1) {
                // 현재 타이핑 중인 내용을 기반으로 온라인 플레이어 목록을 필터링하여 제안
                StringUtil.copyPartialMatches(args[0],
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
                        completions);
            }
        }
        // 'rpgitem' 명령어 자동 완성
        else if (cmdName.equals("rpgitem")) {
            // 첫 번째 인자 자동 완성 (/rpgitem <...>)
            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "reload"), completions);
            }
            // 'give'의 두 번째 인자(아이템 ID) 자동 완성 (/rpgitem give <...>)
            else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                StringUtil.copyPartialMatches(args[1], itemManager.getAllItemIds(), completions);
            }
        }
        else if (cmdName.equals("mob")) {
            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0], Arrays.asList("spawn", "remove_dummy"), completions);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
                // mobManager에서 가져온 몹 ID 목록
                final Set<String> mobIds = mobManager.getAllMobIds();
                // null 체크 후 자동완성 목록에 추가
                if (mobIds != null) {
                    StringUtil.copyPartialMatches(args[1], mobIds, completions);
                }
            }
        }

        return completions; // 완성된 목록 반환
    }
}