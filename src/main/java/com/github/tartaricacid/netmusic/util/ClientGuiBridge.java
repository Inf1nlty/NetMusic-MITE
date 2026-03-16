package com.github.tartaricacid.netmusic.util;

import net.minecraft.EntityPlayer;

import java.lang.reflect.Method;

public final class ClientGuiBridge {
    private static final String DISPATCHER = "com.github.tartaricacid.netmusic.client.gui.NetMusicClientScreens";

    private ClientGuiBridge() {
    }

    public static void openCDBurner(EntityPlayer player) {
        invoke("openCDBurner", player);
    }

    public static void openComputer(EntityPlayer player) {
        invoke("openComputer", player);
    }

    private static void invoke(String methodName, EntityPlayer player) {
        if (player == null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(DISPATCHER);
            Method method = clazz.getMethod(methodName, EntityPlayer.class);
            method.invoke(null, player);
        } catch (Throwable ignored) {
        }
    }
}

