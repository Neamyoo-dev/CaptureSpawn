package cn.oneachina.captureSpawn.item;

import cn.oneachina.captureSpawn.CaptureSpawn;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;
import java.util.Map;

public final class RecipeManager {
    private final CaptureSpawn plugin;
    private final ItemFactory itemFactory;

    public RecipeManager(CaptureSpawn plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public NamespacedKey registerEmptyBallRecipe() {
        if (!plugin.getConfig().getBoolean("recipe.enabled", true)) {
            return null;
        }

        String keyStr = plugin.getConfig().getString("recipe.key", "pokeball");
        NamespacedKey key = new NamespacedKey(plugin, keyStr);
        Bukkit.removeRecipe(key);

        ConfigurationSection shaped = plugin.getConfig().getConfigurationSection("recipe.shaped");
        if (shaped == null) {
            return null;
        }

        List<String> shape = shaped.getStringList("shape");
        if (shape.size() != 3) {
            return null;
        }

        ItemStack result = itemFactory.createEmptyBall();
        int amount = Math.max(1, shaped.getInt("amount", 1));
        result.setAmount(amount);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        ConfigurationSection ingredientsSection = shaped.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            return null;
        }

        for (Map.Entry<String, Object> entry : ingredientsSection.getValues(false).entrySet()) {
            String symbol = entry.getKey();
            if (symbol == null || symbol.length() != 1) {
                continue;
            }
            Object value = entry.getValue();
            if (!(value instanceof String materialName)) {
                continue;
            }
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                continue;
            }
            recipe.setIngredient(symbol.charAt(0), material);
        }

        Bukkit.addRecipe(recipe);
        return key;
    }
}
