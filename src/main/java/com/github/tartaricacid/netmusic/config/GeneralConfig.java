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
    public static double MUSIC_PLAYER_HEAR_DISTANCE = 64.0D;

    public static boolean ENABLE_PLAYER_LYRICS = true;
    public static boolean ENABLE_DEBUG_MODE = false;

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

    public static void setVipCookies(String neteaseCookie, String qqCookie) {
        NETEASE_VIP_COOKIE = sanitize(neteaseCookie);
        QQ_VIP_COOKIE = sanitize(qqCookie);
    }

    public static void setVipCookie(MusicProviderType provider, String cookie) {
        if (provider == MusicProviderType.QQ) {
            QQ_VIP_COOKIE = sanitize(cookie);
            return;
        }
        NETEASE_VIP_COOKIE = sanitize(cookie);
    }

    public static boolean hasNeteaseVipCookie() {
        return StringUtils.isNotBlank(NETEASE_VIP_COOKIE);
    }

    public static boolean hasQqVipCookie() {
        return StringUtils.isNotBlank(QQ_VIP_COOKIE);
    }

    public static boolean hasVipCookieForUrl(String songUrl) {
        return PlayerVipCookieStore.hasVipCookieForUrl(songUrl, NETEASE_VIP_COOKIE, QQ_VIP_COOKIE);
    }

    private static String sanitize(String cookie) {
        return cookie == null ? StringUtils.EMPTY : cookie.trim();
    }
}
