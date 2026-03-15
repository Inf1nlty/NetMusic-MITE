package com.github.tartaricacid.netmusic.client.init;

import com.github.tartaricacid.netmusic.client.model.ModelMusicPlayer;

public class InitModel {
    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }
        ModelMusicPlayer.createBodyLayer();
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
