package com.github.tartaricacid.netmusic.compat;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.github.tartaricacid.netmusic.init.InitItems;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.Block;
import net.minecraft.Item;
import shims.java.net.minecraft.text.Text;

import java.util.List;

public class EmiPluginImpl implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registerInfos(registry);
    }

    private void registerInfos(EmiRegistry registry) {
        this.infoItem(registry, InitItems.MUSIC_CD, "emi.music_cd.info");
        this.infoBlock(registry, InitBlocks.MUSIC_PLAYER, "emi.music_player.info");
        this.infoBlock(registry, InitBlocks.CD_BURNER, "emi.cd_burner.info");
        this.infoBlock(registry, InitBlocks.COMPUTER, "emi.computer.info");
    }

    private void infoItem(EmiRegistry registry, Item item, String info) {
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(item)), List.of(Text.translatable(info)), null));
    }

    private void infoBlock(EmiRegistry registry, Block block, String info) {
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(block)), List.of(Text.translatable(info)), null));
    }
}
