package com.github.tartaricacid.netmusic.config;

import fi.dy.masa.malilib.config.ConfigManager;
import org.apache.commons.lang3.StringUtils;
import java.net.Proxy;

public class GeneralConfig {
    private static boolean REGISTERED = false;

    public static boolean ENABLE_STEREO = true;
    public static Proxy.Type PROXY_TYPE = Proxy.Type.DIRECT;
    public static String PROXY_ADDRESS = "";
    public static MusicProviderType CD_PROVIDER = MusicProviderType.NETEASE;
    public static String NETEASE_VIP_COOKIE = "";
    public static String QQ_VIP_COOKIE = "";
    public static double MUSIC_PLAYER_VOLUME = 1.0D;

    public static boolean ENABLE_PLAYER_LYRICS = true;

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

    public static boolean hasNeteaseVipCookie() {
        return StringUtils.isNotBlank(NETEASE_VIP_COOKIE);
    }

    public static boolean hasQqVipCookie() {
        return StringUtils.isNotBlank(QQ_VIP_COOKIE);
    }

    public static boolean hasVipCookieForUrl(String songUrl) {
        if (StringUtils.isBlank(songUrl)) {
            return hasNeteaseVipCookie() || hasQqVipCookie();
        }
        String url = songUrl.toLowerCase();
        if (url.contains("qqmusic.qq.com") || url.contains("y.qq.com")) {
            return hasQqVipCookie();
        }
        if (url.contains("music.163.com")) {
            return hasNeteaseVipCookie();
        }
        return hasNeteaseVipCookie() || hasQqVipCookie();
    }
}
