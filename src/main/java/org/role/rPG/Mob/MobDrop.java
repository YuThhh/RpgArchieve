package org.role.rPG.Mob;

import org.bukkit.inventory.ItemStack;

public class MobDrop {
    private final ItemStack item;
    private final double chance; // 0.0 (0%) ~ 1.0 (100%)

    public MobDrop(ItemStack item, double chance) {
        this.item = item;
        this.chance = chance;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getChance() {
        return chance;
    }
}
