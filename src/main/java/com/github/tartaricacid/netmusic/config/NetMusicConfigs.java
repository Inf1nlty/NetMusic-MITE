package com.github.tartaricacid.netmusic.config;

import com.github.tartaricacid.netmusic.NetMusic;
import fi.dy.masa.malilib.config.SimpleConfigs;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigEnum;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigString;

import java.net.Proxy;
import java.util.List;

public class NetMusicConfigs extends SimpleConfigs {
    private static final NetMusicConfigs INSTANCE;

    public static final ConfigBoolean ENABLE_STEREO = new ConfigBoolean(
            "netmusic.general.enable_stereo", true, "netmusic.general.enable_stereo"
    );

    public static final ConfigDouble MUSIC_PLAYER_VOLUME = new ConfigDouble(
            "netmusic.general.music_player_volume", 0.5D, 0.0D, 2.0D, "netmusic.general.music_player_volume"
    );

    public static final ConfigString PROXY_TYPE = new ConfigString(
            "netmusic.general.proxy_type", "DIRECT", "netmusic.general.proxy_type"
    );

    public static final ConfigString PROXY_ADDRESS = new ConfigString(
            "netmusic.general.proxy_address", "", "netmusic.general.proxy_address"
    );

    public static final ConfigEnum<MusicProviderType> CD_PROVIDER = new ConfigEnum<>(
            "netmusic.general.cd_provider", MusicProviderType.NETEASE, "netmusic.general.cd_provider"
    );

    public static final ConfigString NETEASE_VIP_COOKIE = new ConfigString(
            "netmusic.general.netease_vip_cookie", "", "netmusic.general.netease_vip_cookie"
    );

    public static final ConfigString QQ_VIP_COOKIE = new ConfigString(
            "netmusic.general.qq_vip_cookie", "", "netmusic.general.qq_vip_cookie"
    );

    public static final ConfigBoolean ENABLE_PLAYER_LYRICS = new ConfigBoolean(
            "netmusic.general.enable_player_lyrics", true, "netmusic.general.enable_player_lyrics"
    );

    public static final ConfigColor ORIGINAL_PLAYER_LYRICS_COLOR = new ConfigColor(
            "netmusic.general.original_player_lyrics_color", "#FFAAAAAA", "netmusic.general.original_player_lyrics_color"
    );

    public static final ConfigColor TRANSLATED_PLAYER_LYRICS_COLOR = new ConfigColor(
            "netmusic.general.translated_player_lyrics_color", "#FFFFFFFF", "netmusic.general.translated_player_lyrics_color"
    );

    public static final List<ConfigBase<?>> VALUES;
    public static final List<ConfigHotkey> HOTKEYS = List.of();

    static {
        VALUES = List.of(
                ENABLE_STEREO,
                MUSIC_PLAYER_VOLUME,
                PROXY_TYPE,
                PROXY_ADDRESS,
                CD_PROVIDER,
                NETEASE_VIP_COOKIE,
                QQ_VIP_COOKIE,
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
        GeneralConfig.MUSIC_PLAYER_VOLUME = clampVolume(MUSIC_PLAYER_VOLUME.getDoubleValue());
        GeneralConfig.ENABLE_PLAYER_LYRICS = ENABLE_PLAYER_LYRICS.getBooleanValue();
        GeneralConfig.PROXY_TYPE = parseProxyType(PROXY_TYPE.getStringValue());
        GeneralConfig.PROXY_ADDRESS = trim(PROXY_ADDRESS.getStringValue());
        GeneralConfig.CD_PROVIDER = CD_PROVIDER.getEnumValue();
        GeneralConfig.NETEASE_VIP_COOKIE = trim(NETEASE_VIP_COOKIE.getStringValue());
        GeneralConfig.QQ_VIP_COOKIE = trim(QQ_VIP_COOKIE.getStringValue());
        GeneralConfig.ORIGINAL_PLAYER_LYRICS_COLOR = normalizeColor(ORIGINAL_PLAYER_LYRICS_COLOR.getStringValue(), "#FFAAAAAA");
        GeneralConfig.TRANSLATED_PLAYER_LYRICS_COLOR = normalizeColor(TRANSLATED_PLAYER_LYRICS_COLOR.getStringValue(), "#FFFFFFFF");
        NetMusic.refreshNetEaseApi();
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

    private static double clampVolume(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 2.0D) {
            return 2.0D;
        }
        return value;
    }
}
