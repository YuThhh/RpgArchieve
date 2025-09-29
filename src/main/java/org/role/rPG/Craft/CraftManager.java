package org.role.rPG.Craft;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.role.rPG.Item.ItemManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 커스텀 조합법(레시피)을 관리하는 클래스입니다.
 * recipes.yml 파일에서 조합법을 불러오고, 인벤토리에 맞는 조합법을 찾는 기능을 제공합니다.
 */
public class CraftManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    // 불러온 모든 커스텀 조합법을 저장하는 리스트
    private final List<Recipe> recipes = new ArrayList<>();

    /**
     * CraftManager의 생성자입니다.
     * @param plugin 메인 플러그인 인스턴스
     * @param itemManager 커스텀 아이템 관리를 위한 ItemManager 인스턴스
     */
    public CraftManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        loadRecipes(); // 플러그인 시작 시 레시피 파일 로드
    }

    /**
     * recipes.yml 파일에서 조합법을 모두 불러와 메모리에 등록합니다.
     */
    public void loadRecipes() {
        recipes.clear(); // 리로드를 위해 기존 레시피 목록 초기화

        File recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        // recipes.yml 파일이 플러그인 폴더에 없으면 jar 내의 기본 파일을 복사
        if (!recipesFile.exists()) {
            plugin.saveResource("recipes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
        // 설정 파일에서 "recipes" 섹션(모든 조합법을 포함하는 최상위 섹션)을 가져옴
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");

        if (recipesSection == null) {
            plugin.getLogger().warning("recipes.yml 파일에서 'recipes' 섹션을 찾을 수 없습니다!");
            return;
        }

        // yml 파일의 모든 조합법 ID를 순회 (예: sharp_iron_sword)
        for (String recipeId : recipesSection.getKeys(false)) {
            ConfigurationSection currentRecipeSection = recipesSection.getConfigurationSection(recipeId);
            if (currentRecipeSection == null) continue; // 해당 조합법 섹션이 없으면 건너뜀

            // 1. 결과물 아이템 불러오기
            String resultItemId = currentRecipeSection.getString("result");
            // ItemManager를 통해 커스텀 아이템 또는 일반 아이템 ItemStack을 가져옴
            ItemStack resultItem = itemManager.getItem(resultItemId);
            if (resultItem == null) {
                plugin.getLogger().warning(recipeId + " 조합법의 결과물 '" + resultItemId + "'를 찾을 수 없습니다.");
                continue; // 결과물이 유효하지 않으면 이 조합법은 무시
            }

            // 2. 재료 아이템 불러오기
            // 조합대 슬롯 인덱스(Integer)와 재료 아이템(ItemStack)을 매핑할 맵
            Map<Integer, ItemStack> ingredients = new HashMap<>();
            // 설정 파일에서 재료 목록 (List<Map>)을 가져옴
            List<Map<?, ?>> ingredientsList = currentRecipeSection.getMapList("ingredients");

            boolean ingredientsValid = true;
            for (Map<?, ?> ingredientMap : ingredientsList) {
                // 재료 정보 추출
                int slot = (int) ingredientMap.get("slot"); // 조합대 내 슬롯 위치 (0-8)
                String itemIdentifier = (String) ingredientMap.get("item"); // 아이템 ID 또는 Material 이름
                int amount = (int) ingredientMap.get("amount"); // 필요 개수

                // 아이템 식별자를 통해 ItemStack을 가져옴
                ItemStack ingredientItem = getItemByIdentifier(itemIdentifier);
                if (ingredientItem == null) {
                    plugin.getLogger().warning(recipeId + " 조합법의 재료 '" + itemIdentifier + "'를 찾을 수 없습니다.");
                    ingredientsValid = false;
                    break; // 유효하지 않은 재료가 발견되면 반복 중단
                }
                // 필요 개수를 ItemStack에 설정
                ingredientItem.setAmount(amount);
                // 슬롯과 아이템을 맵에 추가
                ingredients.put(slot, ingredientItem);
            }

            // 3. 모든 재료가 유효하면 최종적으로 조합법 등록
            if (ingredientsValid) {
                addRecipe(new Recipe(ingredients, resultItem));
            }
        }
        plugin.getLogger().info(recipes.size() + "개의 커스텀 조합법을 불러왔습니다.");
    }

    /**
     * 아이템 식별자(Material 이름 또는 커스텀 아이템 ID)를 기반으로 ItemStack을 반환합니다.
     * @param identifier 아이템 ID 문자열
     * @return 해당 아이템의 ItemStack. 찾지 못하면 null.
     */
    private ItemStack getItemByIdentifier(String identifier) {
        // 먼저 커스텀 아이템인지 ItemManager를 통해 확인
        ItemStack customItem = itemManager.getItem(identifier);
        if (customItem != null) {
            return customItem.clone(); // 원본 수정을 방지하기 위해 복제본 반환
        }

        // 커스텀 아이템이 아니면 기본 마인크래프트 아이템(Material)인지 확인
        try {
            Material material = Material.valueOf(identifier.toUpperCase()); // 대문자로 변환하여 Material enum과 매칭
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return null; // 해당하는 아이템(커스텀 또는 기본)이 전혀 없음
        }
    }

    /**
     * 메모리상의 조합법 리스트에 새로운 조합법을 추가합니다.
     * @param recipe 추가할 Recipe 객체
     */
    public void addRecipe(Recipe recipe) {
        recipes.add(recipe);
    }

    /**
     * 현재 조합대 인벤토리에 있는 아이템들과 일치하는 조합법을 찾습니다.
     * @param currentItems 현재 조합대 3x3 슬롯에 있는 아이템 맵 (슬롯 인덱스 -> ItemStack)
     * @return 일치하는 Recipe 객체. 없으면 null.
     */
    public Recipe getMatchingRecipe(Map<Integer, ItemStack> currentItems) {
        for (Recipe recipe : recipes) {
            // Recipe 클래스의 matches() 메서드를 사용하여 일치 여부 확인
            if (recipe.matches(currentItems)) {
                return recipe;
            }
        }
        return null;
    }
}