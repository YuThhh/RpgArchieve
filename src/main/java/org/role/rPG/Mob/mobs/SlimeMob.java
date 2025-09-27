package org.role.rPG.Mob.mobs;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler; // EventHandler import 확인
import org.bukkit.event.entity.SlimeSplitEvent; // SlimeSplitEvent import 추가
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Mob.CustomMob;
import org.role.rPG.Mob.MobDrop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SlimeMob implements CustomMob {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    private final String mobId = "slime";
    private final int mobLevel = 5;
    private final double proficiencyExp = mobLevel * 5;

    public SlimeMob(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @Override
    public String getMobId() {
        return mobId;
    }

    @Override
    public double getProficiencyExp() {
        return proficiencyExp;
    }

    @Override
    public int getMobLevel() {
        return mobLevel;
    }

    @Override
    public void spawn(Location location) {
        Slime slime = (Slime) location.getWorld().spawnEntity(location, EntityType.SLIME);

        Objects.requireNonNull(slime.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(50);
        slime.setHealth(50);
        slime.setSize(2);

        slime.getPersistentDataContainer().set(DummyMob.CUSTOM_MOB_ID_KEY, PersistentDataType.STRING, getMobId());

        slime.customName(Component.text("§a[Lv." + mobLevel + "] §f슬라임"));
        slime.setCustomNameVisible(true);
    }

    @Override
    public void runPattern(LivingEntity entity) {
        // 특별한 패턴이 없으므로 비워둡니다.
    }

    @Override
    public List<MobDrop> getDrops() {
        List<MobDrop> drops = new ArrayList<>();

        // Item.yml에 정의된 'slime_jelly' 아이템을 가져옵니다.
        ItemStack jelly = itemManager.getItem("slime_jelly");
        if (jelly != null) {
            jelly.setAmount((int) (Math.random() * 3) + 1); // 1~3개 수량 조절
            drops.add(new MobDrop(jelly, 1.0)); // 100% 확률
        }

        // Item.yml에 정의된 'rare_slime_core' 아이템을 가져옵니다.
        ItemStack core = itemManager.getItem("rare_slime_core");
        if (core != null) {
            drops.add(new MobDrop(core, 0.05)); // 5% 확률
        }

        return drops;
    }

    /**
     * [추가] 슬라임 분열 방지 이벤트 핸들러
     * 슬라임이 죽거나 특정 조건에 의해 분열하려고 할 때 이 이벤트가 호출됩니다.
     * @param event 슬라임 분열 이벤트
     */
    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent event) {
        // 분열하려는 슬라임을 가져옵니다.
        Slime slime = event.getEntity();
        String id = slime.getPersistentDataContainer().get(DummyMob.CUSTOM_MOB_ID_KEY, PersistentDataType.STRING);

        // 슬라임의 데이터에서 mobId를 확인하고, 우리의 커스텀 슬라임("slime")이 맞는지 확인합니다.
        if (getMobId().equals(id)) {
            // 커스텀 슬라임이 맞다면, 분열 이벤트를 취소합니다.
            event.setCancelled(true);
        }
    }
}