package org.role.rPG;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

public class PlaceHolder extends PlaceholderExpansion {

    private final RPG plugin;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    public PlaceHolder(RPG plugin) {
        this.plugin = plugin;
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
            case "speed" -> decimalFormat.format(data.getPlayerSpeed(player.getUniqueId()));
            case "strength" -> decimalFormat.format(data.getPlayerStrength(player.getUniqueId()));
            case "atkspd" -> decimalFormat.format(data.getPlayerAttactSpeed(player.getUniqueId()));
            case "crit" -> decimalFormat.format(data.getPlayerCrit(player.getUniqueId()));
            case "critdmg" -> decimalFormat.format(data.getPlayerCritDamage(player.getUniqueId()));
            default -> null;
        };
    }
}