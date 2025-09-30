package org.role.rPG.Craft;

import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class Recipe {
    // <슬롯 번호, 아이템> 형태로 재료를 저장합니다.
    private final Map<Integer, ItemStack> ingredients;
    private final ItemStack result;

    public Recipe(Map<Integer, ItemStack> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
    }

    public Map<Integer, ItemStack> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    /**
     * GUI의 현재 아이템 상태가 이 레시피와 일치하는지 확인합니다.
     * @param currentItems GUI의 조합 그리드에 있는 아이템 맵 (<슬롯, 아이템>)
     * @return 일치하면 true
     */
    public boolean matches(Map<Integer, ItemStack> currentItems) {
        // 재료 개수가 다르면 바로 false
        if (currentItems.size() != ingredients.size()) {
            return false;
        }

        for (Map.Entry<Integer, ItemStack> entry : ingredients.entrySet()) {
            int slot = entry.getKey();
            ItemStack requiredItem = entry.getValue();
            ItemStack providedItem = currentItems.get(slot);

            // 해당 슬롯에 아이템이 없거나, 요구하는 아이템과 다르면 false
            // isSimilar()는 개수를 무시하고 아이템 종류와 메타데이터(이름, NBT 등)를 비교합니다.
            if (providedItem == null || !providedItem.isSimilar(requiredItem)) {
                return false;
            }
            // 재료의 개수보다 적게 넣었으면 false
            if (providedItem.getAmount() < requiredItem.getAmount()) {
                return false;
            }
        }

        return true;
    }
}
