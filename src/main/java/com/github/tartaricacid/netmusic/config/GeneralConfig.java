package com.github.tartaricacid.netmusic.config;

import java.net.Proxy;

public class GeneralConfig {
    public static boolean ENABLE_STEREO = true;
    public static Proxy.Type PROXY_TYPE = Proxy.Type.DIRECT;
    public static String PROXY_ADDRESS = "";

    public static boolean ENABLE_PLAYER_LYRICS = true;
    public static String ORIGINAL_PLAYER_LYRICS_COLOR = "#FFAAAAAA";
    public static String TRANSLATED_PLAYER_LYRICS_COLOR = "#FFFFFFFF";


    public static void init() {
    }
}
