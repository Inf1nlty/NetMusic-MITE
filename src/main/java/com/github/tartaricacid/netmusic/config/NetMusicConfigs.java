package com.github.tartaricacid.netmusic.config;

import fi.dy.masa.malilib.config.SimpleConfigs;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigString;

import java.net.Proxy;
import java.util.List;

public class NetMusicConfigs extends SimpleConfigs {
    private static final NetMusicConfigs INSTANCE;

    public static final ConfigBoolean ENABLE_STEREO = new ConfigBoolean("netmusic.general.enable_stereo", true);
    public static final ConfigString PROXY_TYPE = new ConfigString("netmusic.general.proxy_type", "DIRECT");
    public static final ConfigString PROXY_ADDRESS = new ConfigString("netmusic.general.proxy_address", "");
    public static final ConfigBoolean ENABLE_PLAYER_LYRICS = new ConfigBoolean("netmusic.general.enable_player_lyrics", true);
    public static final ConfigColor ORIGINAL_PLAYER_LYRICS_COLOR = new ConfigColor("netmusic.general.original_player_lyrics_color", "#FFAAAAAA");
    public static final ConfigColor TRANSLATED_PLAYER_LYRICS_COLOR = new ConfigColor("netmusic.general.translated_player_lyrics_color", "#FFFFFFFF");

    public static final List<ConfigBase<?>> VALUES;
    public static final List<ConfigHotkey> HOTKEYS = List.of();

    static {
        VALUES = List.of(
                ENABLE_STEREO,
                PROXY_TYPE,
                PROXY_ADDRESS,
                ENABLE_PLAYER_LYRICS,
                ORIGINAL_PLAYER_LYRICS_COLOR,
                TRANSLATED_PLAYER_LYRICS_COLOR
        );
        INSTANCE = new NetMusicConfigs();
    }

    private NetMusicConfigs() {
        super("NetMusic", HOTKEYS, VALUES);
        bindCallbacks();
    }

    public static NetMusicConfigs getInstance() {
        return INSTANCE;
    }

    public void syncToRuntime() {
        GeneralConfig.ENABLE_STEREO = ENABLE_STEREO.getBooleanValue();
        GeneralConfig.ENABLE_PLAYER_LYRICS = ENABLE_PLAYER_LYRICS.getBooleanValue();
        GeneralConfig.PROXY_TYPE = parseProxyType(PROXY_TYPE.getStringValue());
        GeneralConfig.PROXY_ADDRESS = trim(PROXY_ADDRESS.getStringValue());
        GeneralConfig.ORIGINAL_PLAYER_LYRICS_COLOR = normalizeColor(ORIGINAL_PLAYER_LYRICS_COLOR.getStringValue(), "#FFAAAAAA");
        GeneralConfig.TRANSLATED_PLAYER_LYRICS_COLOR = normalizeColor(TRANSLATED_PLAYER_LYRICS_COLOR.getStringValue(), "#FFFFFFFF");
    }

    private void bindCallbacks() {
        for (ConfigBase<?> value : VALUES) {
            value.setValueChangeCallback(config -> this.syncToRuntime());
        }
    }

    private static Proxy.Type parseProxyType(String value) {
        String type = trim(value).toUpperCase();
        if ("HTTP".equals(type)) {
            return Proxy.Type.HTTP;
        }
        if ("SOCKS".equals(type)) {
            return Proxy.Type.SOCKS;
        }
        return Proxy.Type.DIRECT;
    }

    private static String normalizeColor(String value, String fallback) {
        String color = trim(value).toUpperCase();
        if ((color.length() == 7 || color.length() == 9) && color.startsWith("#")) {
            return color;
        }
        return fallback;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
