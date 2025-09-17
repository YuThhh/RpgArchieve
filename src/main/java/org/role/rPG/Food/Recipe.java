package org.role.rPG.Food;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;

/**
 * 하나의 조합법 정보를 담는 클래스
 */
public class Recipe {

    private final ItemStack result; // 결과 아이템
    private final Map<Material, Integer> ingredients; // 재료 (재료 아이템, 필요 개수)

    public Recipe(ItemStack result, Map<Material, Integer> ingredients) {
        this.result = result;
        this.ingredients = ingredients;
    }

    public ItemStack getResult() {
        return result.clone(); // 원본이 수정되지 않도록 복사본을 반환
    }

    /**
     * GUI에 놓인 아이템들과 이 레시피가 일치하는지 확인하는 메서드
     * @param itemsInGui GUI에 있는 아이템들의 정보 (재료, 개수)
     * @return 레시피 일치 여부
     */
    public boolean matches(Map<Material, Integer> itemsInGui) {
        // 재료의 종류 개수가 다르면 바로 false
        if (this.ingredients.size() != itemsInGui.size()) {
            return false;
        }

        // 각 재료의 필요 개수가 일치하는지 확인
        for (Map.Entry<Material, Integer> entry : this.ingredients.entrySet()) {
            Material requiredMaterial = entry.getKey();
            int requiredAmount = entry.getValue();

            // 필요한 재료가 GUI에 없거나, 개수가 다르면 false
            if (!itemsInGui.containsKey(requiredMaterial) || !Objects.equals(itemsInGui.get(requiredMaterial), requiredAmount)) {
                return false;
            }
        }

        return true; // 모든 조건 통과 시 true
    }
}