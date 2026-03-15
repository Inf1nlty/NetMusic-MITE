package com.github.tartaricacid.netmusic.compat.tlm.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class CompatRegistry {
    public static void initContainer() {
    }

    @Environment(EnvType.CLIENT)
    public static void onRegisterLayers() {
    }

    @Environment(EnvType.CLIENT)
    public static void initContainerScreen() {
    }

    public static void initCreativeModeTab(Object output) {
    }

    @Environment(EnvType.CLIENT)
    public static void registerClientReceiver() {
    }

    public static void registerServerReceiver() {
    }
}
