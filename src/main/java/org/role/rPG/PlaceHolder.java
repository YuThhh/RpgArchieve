package org.role.rPG;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceHolder extends PlaceholderExpansion {

    private final RPG plugin;

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
            case "currentmana" -> String.valueOf(data.getPlayerCurrentMana(player.getUniqueId()));
            case "maxmana" -> String.valueOf(data.getPlayerMaxMana(player.getUniqueId()));
            case "maxhealth" -> String.valueOf(data.getplayerMaxHealth(player.getUniqueId()));
            case "hpregenbonus" -> String.valueOf(data.getPlayerHpRegenarationBonus(player.getUniqueId()));
            case "speed" -> String.valueOf(data.getPlayerSpeed(player.getUniqueId()));
            default -> null;
        };
    }
}