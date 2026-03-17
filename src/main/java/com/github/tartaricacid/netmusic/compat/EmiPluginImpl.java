package com.github.tartaricacid.netmusic.compat;

import com.github.tartaricacid.netmusic.init.InitItems;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.Item;
import shims.java.net.minecraft.text.Text;

import java.util.List;

public class EmiPluginImpl implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registerInfos(registry);
    }

    private void registerInfos(EmiRegistry registry) {
        this.info(registry, InitItems.MUSIC_CD, "emi.music_cd.info");
    }

    private void info(EmiRegistry registry, Item item, String info) {
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(item)), List.of(Text.translatable(info)), null));
    }
}
