package org.role.rPG.Food;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 모든 요리 레시피를 등록하고 관리하는 클래스
 */
public class RecipeManager {

    private final List<Recipe> recipes = new ArrayList<>();

    public RecipeManager() {
        // 이 곳에서 모든 레시피를 등록합니다.
        registerRecipes();
    }

    private void registerRecipes() {
        // --- 1. 미트볼 레시피 등록 ---
        ItemStack meatballResult = createMeatball();
        Map<Material, Integer> meatballIngredients = new HashMap<>();
        meatballIngredients.put(Material.PORKCHOP, 3);
        meatballIngredients.put(Material.STICK, 2);
        recipes.add(new Recipe(meatballResult, meatballIngredients));

        // --- 2. 여기에 새로운 레시피를 추가하면 됩니다 ---
        // 예시: 황금사과 레시피 (결과물, 재료 설정 후 recipes.add() 호출)
        // ItemStack goldenAppleResult = new ItemStack(Material.GOLDEN_APPLE);
        // Map<Material, Integer> goldenAppleIngredients = new HashMap<>();
        // goldenAppleIngredients.put(Material.GOLD_INGOT, 8);
        // goldenAppleIngredients.put(Material.APPLE, 1);
        // recipes.add(new Recipe(goldenAppleResult, goldenAppleIngredients));
    }

    /**
     * GUI의 내용과 일치하는 레시피를 찾아 반환합니다.
     * @param gui 플레이어의 요리 GUI
     * @return 일치하는 Recipe 객체, 없으면 null
     */
    public Recipe findMatchingRecipe(Inventory gui) {
        // GUI의 재료 칸(1~7)에 있는 아이템들을 분석
        Map<Material, Integer> itemsInGui = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                itemsInGui.put(item.getType(), itemsInGui.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        // 등록된 모든 레시피와 대조
        for (Recipe recipe : recipes) {
            if (recipe.matches(itemsInGui)) {
                return recipe; // 일치하는 레시피를 찾으면 즉시 반환
            }
        }

        return null; // 일치하는 레시피가 없으면 null 반환
    }

    // 미트볼 아이템 생성 로직 (Cooked 클래스에서 가져옴)
    private ItemStack createMeatball() {
        ItemStack meatball = new ItemStack(Material.COCOA_BEANS);
        ItemMeta meta = meatball.getItemMeta();
        meta.displayName(Component.text("§e§l미트볼"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7잘 다져진 고기를 뭉쳐 만들었다."));
        meta.lore(lore);
        meatball.setItemMeta(meta);
        return meatball;
    }
}