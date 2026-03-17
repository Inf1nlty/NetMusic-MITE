package com.github.tartaricacid.netmusic.config;

import com.github.tartaricacid.netmusic.NetMusic;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PlayerVipCookieStore {
    private static final Object LOCK = new Object();
    private static final String PLAYERS_TAG = "players";
    private static final String NETEASE_TAG = "netease_cookie";
    private static final String QQ_TAG = "qq_cookie";
    private static final Map<String, CookiePair> PLAYER_COOKIES = new LinkedHashMap<>();
    private static boolean loaded;

    private PlayerVipCookieStore() {
    }

    public static CookiePair getCookies(UUID playerUuid) {
        return getCookies(playerUuid == null ? null : playerUuid.toString());
    }

    public static CookiePair getCookies(String playerKey) {
        if (StringUtils.isBlank(playerKey)) {
            return CookiePair.EMPTY;
        }
        synchronized (LOCK) {
            ensureLoaded();
            CookiePair pair = PLAYER_COOKIES.get(playerKey.trim());
            return pair == null ? CookiePair.EMPTY : pair.copy();
        }
    }

    public static void setCookies(UUID playerUuid, String neteaseCookie, String qqCookie) {
        setCookies(playerUuid == null ? null : playerUuid.toString(), neteaseCookie, qqCookie);
    }

    public static void setCookies(String playerKey, String neteaseCookie, String qqCookie) {
        if (StringUtils.isBlank(playerKey)) {
            return;
        }
        synchronized (LOCK) {
            ensureLoaded();
            String key = playerKey.trim();
            String netease = sanitizeCookie(neteaseCookie);
            String qq = sanitizeCookie(qqCookie);
            CookiePair old = PLAYER_COOKIES.get(key);
            if (old != null && old.neteaseCookie.equals(netease) && old.qqCookie.equals(qq)) {
                return;
            }
            PLAYER_COOKIES.put(key, new CookiePair(netease, qq));
            saveLocked();
        }
    }

    public static void setProviderCookie(UUID playerUuid, MusicProviderType provider, String cookie) {
        if (playerUuid == null || provider == null) {
            return;
        }
        CookiePair current = getCookies(playerUuid);
        if (provider == MusicProviderType.QQ) {
            setCookies(playerUuid, current.neteaseCookie, cookie);
        } else {
            setCookies(playerUuid, cookie, current.qqCookie);
        }
    }

    public static boolean hasVipCookieForUrl(UUID playerUuid, String songUrl) {
        CookiePair pair = getCookies(playerUuid);
        return hasVipCookieForUrl(songUrl, pair.neteaseCookie, pair.qqCookie);
    }

    public static boolean hasVipCookieForUrl(String songUrl, String neteaseCookie, String qqCookie) {
        boolean hasNetease = StringUtils.isNotBlank(sanitizeCookie(neteaseCookie));
        boolean hasQq = StringUtils.isNotBlank(sanitizeCookie(qqCookie));
        if (StringUtils.isBlank(songUrl)) {
            return hasNetease || hasQq;
        }
        String url = songUrl.toLowerCase(Locale.ROOT);
        if (isQqUrl(url)) {
            return hasQq;
        }
        if (url.contains("music.163.com")) {
            return hasNetease;
        }
        return hasNetease || hasQq;
    }

    private static boolean isQqUrl(String lowerUrl) {
        return lowerUrl.contains("y.qq.com")
                || lowerUrl.contains("qqmusic.qq.com")
                || lowerUrl.contains("aqqmusic.tc.qq.com")
                || lowerUrl.contains("stream.qqmusic.qq.com")
                || lowerUrl.contains("ws.stream.qqmusic.qq.com");
    }

    private static String sanitizeCookie(String cookie) {
        return cookie == null ? "" : cookie.trim();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        loadLocked();
    }

    private static void loadLocked() {
        PLAYER_COOKIES.clear();
        File file = getStoreFile();
        if (!file.isFile()) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject root = element.getAsJsonObject();
            if (!root.has(PLAYERS_TAG) || !root.get(PLAYERS_TAG).isJsonObject()) {
                return;
            }
            JsonObject players = root.getAsJsonObject(PLAYERS_TAG);
            for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject value = entry.getValue().getAsJsonObject();
                String netease = readString(value, NETEASE_TAG);
                String qq = readString(value, QQ_TAG);
                PLAYER_COOKIES.put(entry.getKey(), new CookiePair(netease, qq));
            }
        } catch (Exception e) {
            NetMusic.LOGGER.warn("Failed to load player vip cookie store", e);
        }
    }

    private static void saveLocked() {
        File file = getStoreFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            NetMusic.LOGGER.warn("Failed to create cookie store directory: {}", parent.getAbsolutePath());
            return;
        }

        JsonObject root = new JsonObject();
        JsonObject players = new JsonObject();
        for (Map.Entry<String, CookiePair> entry : PLAYER_COOKIES.entrySet()) {
            CookiePair pair = entry.getValue();
            JsonObject value = new JsonObject();
            value.addProperty(NETEASE_TAG, pair.neteaseCookie);
            value.addProperty(QQ_TAG, pair.qqCookie);
            players.add(entry.getKey(), value);
        }
        root.add(PLAYERS_TAG, players);

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            com.google.gson.GsonBuilder builder = new com.google.gson.GsonBuilder();
            builder.setPrettyPrinting().create().toJson(root, writer);
        } catch (Exception e) {
            NetMusic.LOGGER.warn("Failed to save player vip cookie store", e);
        }
    }

    private static String readString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return sanitizeCookie(element.getAsString());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static File getStoreFile() {
        return new File(new File("config"), "netmusic-player-vip-cookies.json");
    }

    public static final class CookiePair {
        public static final CookiePair EMPTY = new CookiePair("", "");
        public final String neteaseCookie;
        public final String qqCookie;

        public CookiePair(String neteaseCookie, String qqCookie) {
            this.neteaseCookie = sanitizeCookie(neteaseCookie);
            this.qqCookie = sanitizeCookie(qqCookie);
        }

        public CookiePair copy() {
            return new CookiePair(this.neteaseCookie, this.qqCookie);
        }
    }
}
