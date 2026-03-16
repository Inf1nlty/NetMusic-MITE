package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.tileentity.TileEntityCDBurner;
import net.minecraft.EntityPlayer;
import net.minecraft.Minecraft;

public final class NetMusicClientScreens {
    private NetMusicClientScreens() {
    }

    public static void openCDBurner(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null || player != minecraft.thePlayer) {
            return;
        }
        minecraft.displayGuiScreen(new GuiCDBurnerScreen(new CDBurnerMenu(player, new TileEntityCDBurner())));
    }

    public static void openComputer(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null || player != minecraft.thePlayer) {
            return;
        }
        minecraft.displayGuiScreen(new GuiComputerScreen(new ComputerMenu(player)));
    }
}

