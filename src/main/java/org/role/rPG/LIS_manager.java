package org.role.rPG;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.role.rPG.Effect.EffectListener;
import org.role.rPG.Enchant.EnchantmentListener;
import org.role.rPG.Enchant.EnchantmentManager;
import org.role.rPG.Food.FoodListener;
import org.role.rPG.Indicator.IndicatorManager;
import org.role.rPG.Item.EquipmentListener;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Item.ItemUpdateListener;
import org.role.rPG.Level.ExperienceListener;
import org.role.rPG.Level.LevelManager;
import org.role.rPG.Mob.MobDeathListener;
import org.role.rPG.Mob.MobManager;
import org.role.rPG.Player.BuffManager;
import org.role.rPG.Player.Stat;
import org.role.rPG.Player.StatManager;
import org.role.rPG.Skill.SkillListener;
import org.role.rPG.UI.GUI_manager;
import org.role.rPG.UI.Ui;
import org.role.rPG.Effect.EffectManager;
// 필요하다면 이 리스너도 등록


public class LIS_manager {

    private final JavaPlugin plugin;
    private final PluginManager pm;

    public LIS_manager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }

    /**
     * 플러그인의 모든 일반 이벤트 리스너를 등록합니다.
     * onEnable에서 필요한 매니저들을 인자로 받아옵니다.
     */
    public void registerGeneralListeners(StatManager statManager, IndicatorManager indicatorManager, ItemManager itemManager, LevelManager levelManager, MobManager mobManager, EffectManager effectManager, EnchantmentManager enchantmentManager, BuffManager buffManager) {

        // GUI 이벤트 총괄 매니저
        pm.registerEvents(new GUI_manager(), plugin);

        // [이동 및 수정] RPG 클래스에 있던 리스너들을 이곳에서 등록합니다.
        pm.registerEvents(new MobDeathListener(mobManager), plugin);
        pm.registerEvents(new EnchantmentListener(enchantmentManager, effectManager), plugin);

        // 기타 모든 리스너들
        pm.registerEvents(new Stat(plugin, statManager, indicatorManager, effectManager), plugin);
        pm.registerEvents(new FoodListener(plugin, itemManager, statManager, buffManager), plugin);
        pm.registerEvents(new EquipmentListener(plugin, statManager), plugin);
        pm.registerEvents(new Ui(plugin, statManager, levelManager), plugin);
        pm.registerEvents(new ItemUpdateListener(itemManager), plugin);
        pm.registerEvents(new SkillListener(plugin, itemManager, statManager), plugin);
        pm.registerEvents(new ExperienceListener(levelManager, mobManager, effectManager), plugin);
        pm.registerEvents(new EffectListener(plugin, itemManager), plugin);

        // [삭제] 기존의 중복되거나 잘못된 리스너 등록 코드를 제거했습니다.
    }
}