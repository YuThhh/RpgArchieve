package org.role.rPG.Reforge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Player.Cash;
import org.role.rPG.Player.StatManager;

public class ReforgeCommand implements CommandExecutor {

    private final ItemManager itemManager;
    private final StatManager statManager;
    private final ReforgeManager reforgeManager;

    public ReforgeCommand(ItemManager itemManager, StatManager statManager, ReforgeManager reforgeManager) {
        this.itemManager = itemManager;
        this.statManager = statManager;
        this.reforgeManager = reforgeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용 가능합니다.", NamedTextColor.RED));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (itemManager.isNotCustomItem(item)) {
            player.sendMessage(Component.text("리포지할 수 있는 아이템을 손에 들어주세요.", NamedTextColor.RED));
            return true;
        }

        int cost = reforgeManager.getReforgeCost();
        if (Cash.getMoney(player) < cost) {
            player.sendMessage(Component.text("리포지 비용이 부족합니다. (" + cost + "G)", NamedTextColor.RED));
            return true;
        }

        ReforgeManager.ReforgeModifier modifier = reforgeManager.getRandomModifier();
        if (modifier == null) {
            player.sendMessage(Component.text("리포지 정보가 없습니다. 관리자에게 문의하세요.", NamedTextColor.RED));
            return true;
        }

        Cash.removeMoney(player, cost);
        itemManager.reforgeItem(item, modifier);
        statManager.updatePlayerStats(player);

        player.sendMessage(Component.text()
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(modifier.getName(), NamedTextColor.YELLOW))
                .append(Component.text("] 접두사가 부여되었습니다!", NamedTextColor.GRAY)));

        return true;
    }
}