package com.github.tartaricacid.netmusic.client.model;

import java.util.HashMap;
import java.util.Map;

public class ModelMusicPlayer {
    public static final String LAYER = "netmusic_music_player";
    private static final Map<String, float[]> PARTS = new HashMap<String, float[]>();

    public static Object createBodyLayer() {
        if (PARTS.isEmpty()) {
            PARTS.put("base", new float[]{0.0f, 0.0f, 0.0f, 16.0f, 8.0f, 16.0f});
            PARTS.put("arm", new float[]{5.0f, 8.0f, 5.0f, 6.0f, 2.0f, 6.0f});
            PARTS.put("speaker", new float[]{2.0f, 2.0f, 2.0f, 4.0f, 4.0f, 1.0f});
        }
        return PARTS;
    }

    public static Map<String, float[]> getParts() {
        return PARTS;
    }
}