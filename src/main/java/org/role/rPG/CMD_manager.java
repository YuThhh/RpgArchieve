package org.role.rPG;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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

import java.util.ArrayList;
import java.util.Objects;
import java.util.List;

public class CMD_manager implements CommandExecutor {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final JavaPlugin plugin;

    public CMD_manager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 명령어 등록
    public void registerCommands() {
        Objects.requireNonNull(plugin.getCommand("메뉴")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setdef")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setcrit")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setcritdmg")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("sethp")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("myinfo")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("sucheck")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("devsucheck")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setpower")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setattackspeed")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setspeed")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("setvital")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

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


}
