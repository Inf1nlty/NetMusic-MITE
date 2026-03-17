package com.github.tartaricacid.netmusic.init;

import net.minecraft.Block;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.xiaoyu233.fml.reload.event.RecipeRegistryEvent;

public final class InitRecipes {
    private InitRecipes() {
    }

    public static void registerRecipes(RecipeRegistryEvent event) {
        registerMusicPlayerRecipes(event);
        registerMachineRecipes(event);
        registerMusicCdRecipe(event);
    }

    private static void registerMusicPlayerRecipes(RecipeRegistryEvent event) {
        for (int plankSubtype = 0; plankSubtype <= 3; plankSubtype++) {
            event.registerShapedRecipe(new ItemStack(InitBlocks.MUSIC_PLAYER), false,
                    "ABA", "ACA", "AAA",
                    'A', new ItemStack(Block.planks, 1, plankSubtype),
                    'B', new ItemStack(Item.book),
                    'C', new ItemStack(Item.diamond));
        }
    }

    private static void registerMachineRecipes(RecipeRegistryEvent event) {
        event.registerShapelessRecipe(new ItemStack(InitBlocks.CD_BURNER), false,
                new ItemStack(Block.jukebox),
                new ItemStack(Item.ingotIron),
                new ItemStack(Item.redstone));

        event.registerShapelessRecipe(new ItemStack(InitBlocks.COMPUTER), false,
                new ItemStack(Block.jukebox),
                new ItemStack(Item.ingotGold),
                new ItemStack(Item.redstone));
    }

    private static void registerMusicCdRecipe(RecipeRegistryEvent event) {
        event.registerShapelessRecipe(new ItemStack(InitItems.MUSIC_CD), false,
                new ItemStack(Item.dyePowder, 1, 8),
                new ItemStack(Item.dyePowder, 1, 3),
                new ItemStack(Item.clay),
                new ItemStack(Item.clay));
    }
}
