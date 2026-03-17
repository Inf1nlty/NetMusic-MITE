package com.github.tartaricacid.netmusic.event;

import com.github.tartaricacid.netmusic.config.NetMusicConfigs;
import org.apache.commons.lang3.math.NumberUtils;

public class ConfigEvent {
    // 播放器歌词颜色缓存
    public static int PLAYER_ORIGINAL_COLOR = 0xFF_AAAAAA;
    public static int PLAYER_TRANSLATED_COLOR = 0xFF_FFFFFF;

    public static void onConfigLoading() {
        reloadColors();
    }

    public static void onConfigReloading() {
        reloadColors();
    }

    public static void reloadColors() {
        PLAYER_ORIGINAL_COLOR = parseColor(
                NetMusicConfigs.ORIGINAL_PLAYER_LYRICS_COLOR.getStringValue(),
                0xFF_AAAAAA
        );
        PLAYER_TRANSLATED_COLOR = parseColor(
                NetMusicConfigs.TRANSLATED_PLAYER_LYRICS_COLOR.getStringValue(),
                0xFF_FFFFFF
        );
    }

    public static int parseColor(String colorString, int fallback) {
        if (colorString == null || !colorString.startsWith("#")) {
            return fallback;
        }

        try {
            String hex = colorString.substring(1);

            // 如果颜色是 6 位,自动添加 alpha 通道
            if (hex.length() == 6) {
                hex = "FF" + hex;
            }
            return NumberUtils.createLong("0x" + hex).intValue();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
