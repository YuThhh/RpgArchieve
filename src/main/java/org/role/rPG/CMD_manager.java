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
import org.role.rPG.Player.Cash;
import org.role.rPG.Player.PER_DATA;
import org.role.rPG.UI.Menu_UI;
import org.role.rPG.UI.Reforge_UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// [수정] TabCompleter 인터페이스를 구현(implements)합니다.
public class CMD_manager implements CommandExecutor, TabCompleter {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final Reforge_UI reforgeUI;

    public CMD_manager(JavaPlugin plugin, ItemManager itemManager, Reforge_UI reforgeUI) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.reforgeUI = reforgeUI;
    }

    // 명령어 등록
    public void registerCommands() {
        List<String> commands = Arrays.asList(
                "메뉴", "setdef", "setcrit", "setcritdmg", "sethp", "myinfo",
                "sucheck", "devsucheck", "setpower", "setattackspeed",
                "setspeed", "setvital", "rpgitem", "리포지"
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

        }
        else if (command.getName().equalsIgnoreCase("setcritdmg")) {
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

        }
        else if (command.getName().equalsIgnoreCase("sethp")) {
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
                PER_DATA.getInstance().setplayerMaxHealth(target.getUniqueId(), hp);

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
        else if (command.getName().equalsIgnoreCase("setpower")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /setpower <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double str = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerStrength(target.getUniqueId(), str);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 힘을 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), str);
                String targetMessage = String.format("<aqua>당신의 힘이 <white>%.1f</white>로 설정되었습니다.</aqua>", str);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>힘은 숫자여야 합니다."));
            }
            return true;

        }
        else if (command.getName().equalsIgnoreCase("setattackspeed")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /setpower <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double atkspd = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerAttackSpeed(target.getUniqueId(), atkspd);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 공격 속도를 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), atkspd);
                String targetMessage = String.format("<aqua>당신의 공격 속도가 <white>%.1f</white>로 설정되었습니다.</aqua>", atkspd);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>공격 속도는 숫자여야 합니다."));
            }
            return true;

        } else if (command.getName().equalsIgnoreCase("setspeed")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /setspeed <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double spd = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerSpeed(target.getUniqueId(), (float) spd);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 이동 속도를 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), spd);
                String targetMessage = String.format("<aqua>당신의 이동 속도가 <white>%.1f</white>로 설정되었습니다.</aqua>", spd);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>이동 속도는 숫자여야 합니다."));
            }
            return true;

        }
        else if (command.getName().equalsIgnoreCase("setvital")) {
            // [개선 1] 권한 확인: OP가 아니거나 "rpg.command.setdef" 권한이 없으면 거부
            if (!sender.isOp() && !sender.hasPermission("rpg.command.setdef")) {
                sender.sendMessage(mm.deserialize("<red>이 명령어를 사용할 권한이 없습니다."));
                return true;
            }

            // [개선 2] 인자 개수 확인: 명령어 형식이 잘못되었으면 사용법 안내
            if (args.length != 2) {
                sender.sendMessage(mm.deserialize("<red>사용법: /setspeed <플레이어> <수치>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>플레이어를 찾을 수 없습니다."));
                return true;
            }

            try {
                double vital = Double.parseDouble(args[1]);
                PER_DATA.getInstance().setPlayerHpRegenarationBonus(target.getUniqueId(), (float) vital);

                // [개선 3] 메시지 형식을 MiniMessage로 통일
                String senderMessage = String.format("<green>%s님의 체력 재생을 <white>%.1f</white>로 설정했습니다.</green>", target.getName(), vital);
                String targetMessage = String.format("<aqua>당신의 체력 재생이 <white>%.1f</white>로 설정되었습니다.</aqua>", vital);

                sender.sendMessage(mm.deserialize(senderMessage));
                target.sendMessage(mm.deserialize(targetMessage));

            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>체력 재생은 숫자여야 합니다."));
            }
            return true;

        }
        else if (command.getName().equalsIgnoreCase("rpgitem")) {
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
        else if (command.getName().equalsIgnoreCase("리포지")) {
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

        return completions; // 완성된 목록 반환
    }
}