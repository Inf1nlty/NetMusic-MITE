package com.github.tartaricacid.netmusic.config;

import fi.dy.masa.malilib.config.ConfigManager;
import java.net.Proxy;

public class GeneralConfig {
    private static boolean REGISTERED = false;

    public static boolean ENABLE_STEREO = true;
    public static Proxy.Type PROXY_TYPE = Proxy.Type.DIRECT;
    public static String PROXY_ADDRESS = "";

    public static boolean ENABLE_PLAYER_LYRICS = true;
    public static String ORIGINAL_PLAYER_LYRICS_COLOR = "#FFAAAAAA";
    public static String TRANSLATED_PLAYER_LYRICS_COLOR = "#FFFFFFFF";

    public static void init() {
        if (!REGISTERED) {
            ConfigManager.getInstance().registerConfig(NetMusicConfigs.getInstance());
            REGISTERED = true;
        }
        reload();
    }

    public static void reload() {
        NetMusicConfigs configs = NetMusicConfigs.getInstance();
        configs.load();
        configs.syncToRuntime();
    }
}
