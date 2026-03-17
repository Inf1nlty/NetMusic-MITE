package com.github.tartaricacid.netmusic.config;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.event.ConfigEvent;
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

    public static final ConfigDouble MUSIC_PLAYER_HEAR_DISTANCE = new ConfigDouble(
            "netmusic.general.music_player_hear_distance", 64.0D, 1.0D, 256.0D, "netmusic.general.music_player_hear_distance"
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

    public static final ConfigBoolean ENABLE_PLAYER_LYRICS = new ConfigBoolean(
            "netmusic.general.enable_player_lyrics", true, "netmusic.general.enable_player_lyrics"
    );

    public static final ConfigBoolean ENABLE_DEBUG_MODE = new ConfigBoolean(
            "netmusic.general.enable_debug_mode", false, "netmusic.general.enable_debug_mode"
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
                MUSIC_PLAYER_HEAR_DISTANCE,
                PROXY_TYPE,
                PROXY_ADDRESS,
                CD_PROVIDER,
                ENABLE_PLAYER_LYRICS,
                ENABLE_DEBUG_MODE,
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
        GeneralConfig.MUSIC_PLAYER_HEAR_DISTANCE = clampHearDistance(MUSIC_PLAYER_HEAR_DISTANCE.getDoubleValue());
        GeneralConfig.ENABLE_PLAYER_LYRICS = ENABLE_PLAYER_LYRICS.getBooleanValue();
        GeneralConfig.ENABLE_DEBUG_MODE = ENABLE_DEBUG_MODE.getBooleanValue();
        GeneralConfig.PROXY_TYPE = parseProxyType(PROXY_TYPE.getStringValue());
        GeneralConfig.PROXY_ADDRESS = trim(PROXY_ADDRESS.getStringValue());
        GeneralConfig.CD_PROVIDER = CD_PROVIDER.getEnumValue();
        ConfigEvent.reloadColors();
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

    private static double clampHearDistance(double value) {
        if (value < 1.0D) {
            return 1.0D;
        }
        if (value > 256.0D) {
            return 256.0D;
        }
        return value;
    }
}
