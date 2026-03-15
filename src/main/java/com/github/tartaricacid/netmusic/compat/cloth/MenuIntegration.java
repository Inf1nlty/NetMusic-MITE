package com.github.tartaricacid.netmusic.compat.cloth;

import com.github.tartaricacid.netmusic.config.NetMusicConfigs;
import net.minecraft.GuiScreen;
import net.minecraft.Minecraft;

public class MenuIntegration {

    private MenuIntegration() {
    }

    public static void openConfigScreen(GuiScreen parent) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            minecraft.displayGuiScreen(NetMusicConfigs.getInstance().getConfigScreen(parent));
        }
    }
}
