package com.github.tartaricacid.netmusic;

import com.google.common.eventbus.Subscribe;
import com.github.tartaricacid.netmusic.init.InitBlockEntity;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.init.InitRecipes;
import net.xiaoyu233.fml.reload.event.BlockRegistryEvent;
import net.xiaoyu233.fml.reload.event.ItemRegistryEvent;
import net.xiaoyu233.fml.reload.event.RecipeRegistryEvent;
import net.xiaoyu233.fml.reload.event.TileEntityRegisterEvent;

public class NetMusicFMLEvents {

    @Subscribe
    public void onItemRegister(ItemRegistryEvent event) {
        InitItems.registerItems(event);
    }

    @Subscribe
    public void onBlockRegister(BlockRegistryEvent event) {
        InitBlocks.registerBlocks(event);
    }

    @Subscribe
    public void onTileEntityRegister(TileEntityRegisterEvent event) {
        InitBlockEntity.registerTileEntities(event);
    }

    @Subscribe
    public void onRecipeRegister(RecipeRegistryEvent event) {
        InitRecipes.registerRecipes(event);
    }
}
