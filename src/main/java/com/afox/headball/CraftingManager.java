package com.afox.headball;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

public class CraftingManager {

    private final HeadBall plugin;
    private final NamespacedKey footballKey;
    private final NamespacedKey volleyballKey;

    public CraftingManager(HeadBall plugin) {
        this.plugin = plugin;
        this.footballKey = new NamespacedKey(plugin, "football");
        this.volleyballKey = new NamespacedKey(plugin, "volleyball");
    }

    public void apply() {
        remove();
        if (!plugin.getConfig().getBoolean("crafting.enabled", true)) {
            return;
        }
        registerFootball();
        registerVolleyball();
    }

    public void remove() {
        Bukkit.removeRecipe(footballKey);
        Bukkit.removeRecipe(volleyballKey);
    }

    private void registerFootball() {
        ItemStack result = Ball.createHeldItem(
                plugin,
                plugin.getSettings().randomTexture(),
                "ball",
                plugin.getMessages().plain("football-name")
        );

        ShapedRecipe recipe = new ShapedRecipe(footballKey, result);
        recipe.shape("BDB", "WWW", "BDB");
        recipe.setIngredient('B', Material.BLACK_DYE);
        recipe.setIngredient('D', Material.WHITE_DYE);
        recipe.setIngredient('W', Material.WHITE_WOOL);
        Bukkit.addRecipe(recipe);
    }

    private void registerVolleyball() {
        ItemStack result = Ball.createHeldItem(
                plugin,
                plugin.getVolleyballSettings().randomTexture(),
                "volleyball",
                plugin.getMessages().plain("volleyball-name")
        );

        ShapedRecipe recipe = new ShapedRecipe(volleyballKey, result);
        recipe.shape("BDB", "SWS", "BDB");
        recipe.setIngredient('B', Material.BLACK_DYE);
        recipe.setIngredient('D', Material.ORANGE_DYE);
        recipe.setIngredient('S', Material.STRING);
        recipe.setIngredient('W', new RecipeChoice.MaterialChoice(Tag.WOOL));
        Bukkit.addRecipe(recipe);
    }
}
