package org.role.rPG;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.role.rPG.Player.PER_DATA;
import org.role.rPG.Player.StatManager;

import java.text.DecimalFormat;

public class PlaceHolder extends PlaceholderExpansion {

    private final RPG plugin;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private StatManager statManager;

    public PlaceHolder(RPG plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager =  statManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rpg";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName"; // 본인 이름으로 변경하세요
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

            if (player == null || !player.isOnline()) {
            return null;
        }

        PER_DATA data = PER_DATA.getInstance();
        if (data == null) {
            return null;
        }

        return switch (params) {
            case "currentmana" -> decimalFormat.format(data.getPlayerCurrentMana(player.getUniqueId()));
            case "maxmana" -> decimalFormat.format(data.getPlayerMaxMana(player.getUniqueId()));
            case "maxhealth" -> decimalFormat.format(data.getplayerMaxHealth(player.getUniqueId()));
            case "vital" -> decimalFormat.format(data.getPlayerHpRegenarationBonus(player.getUniqueId()));
            case "speed" -> decimalFormat.format(statManager.getFinalStat(player.getUniqueId(),"SPEED"));
            case "strength" -> decimalFormat.format(statManager.getFinalStat(player.getUniqueId(),"STRENGTH"));
            case "atkspd" -> decimalFormat.format(statManager.getFinalStat(player.getUniqueId(),"ATTACK_SPEED"));
            case "crit" -> decimalFormat.format(statManager.getFinalStat(player.getUniqueId(),"CRIT_CHANCE"));
            case "critdmg" -> decimalFormat.format(statManager.getFinalStat(player.getUniqueId(),"CRIT_DAMAGE"));
            default -> null;
        };
    }
}