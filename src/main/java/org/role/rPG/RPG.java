package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class RPG extends JavaPlugin {

    public static NamespacedKey SUCHECK_VALUE_KEY;

    @Override
    public void onEnable() {
        SUCHECK_VALUE_KEY = new NamespacedKey(this, "sucheck_value");

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
            // [수정됨] 인자 개수 확인 (1개 또는 2개만 허용)
            if (args.length == 0 || args.length > 2) {
                player.sendMessage("§c사용법: /" + label + " <금액> [개수]");
                return true;
            }

            try {
                // 첫 번째 인자: 금액
                int amount = Integer.parseInt(args[0]);

                // [추가됨] 두 번째 인자: 개수 (없으면 기본값 1)
                int count = 1;
                if (args.length == 2) {
                    count = Integer.parseInt(args[1]);
                }

                // 금액 및 개수 유효성 검사
                if (amount <= 0) {
                    player.sendMessage("§c금액은 1 이상의 숫자여야 합니다.");
                    return true;
                }
                if (count <= 0 || count > 64) {
                    player.sendMessage("§c개수는 1개부터 64개까지 지정할 수 있습니다.");
                    return true;
                }

                // 일반 수표일 경우 돈 차감 로직 실행
                if (commandName.equals("sucheck")) {
                    // [수정됨] 총 필요 금액 계산 (금액 * 개수)
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

                // [수정됨] 지정된 '개수'만큼 아이템 스택 생성
                ItemStack sucheckItem = new ItemStack(Material.IRON_NUGGET, count);
                ItemMeta meta = sucheckItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6" + String.format("%,d", amount) + "G");
                    meta.setLore(Arrays.asList(
                            "§7우클릭하여 사용하세요.",
                            "§7쉬프트+우클릭 시 같은 금액의 수표를 모두 사용합니다."
                    ));
                    meta.getPersistentDataContainer().set(SUCHECK_VALUE_KEY, PersistentDataType.INTEGER, amount);
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