package com.github.tartaricacid.netmusic.api.netease;

import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NeteaseVipMusicApi {
    private static final String BASE_URL = "https://music.163.com/api/song/enhance/player/url/v1?encodeType=mp3&ids=[%s]&level=%s";
    private static final String[] LEVELS = new String[]{"exhigh", "higher", "standard"};
    private static final Pattern MUSIC_ID_PATTERN = Pattern.compile("[?&]id=(\\d+)\\.mp3");

    private NeteaseVipMusicApi() {
    }

    public static String resolveByOuterUrl(String url) {
        return resolveByOuterUrl(url, GeneralConfig.NETEASE_VIP_COOKIE);
    }

    public static String resolveByOuterUrl(String url, String cookie) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        Matcher matcher = MUSIC_ID_PATTERN.matcher(url);
        if (!matcher.find()) {
            return "";
        }
        try {
            long songId = Long.parseLong(matcher.group(1));
            return resolveBySongId(songId, cookie);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public static String resolveBySongId(long songId) {
        return resolveBySongId(songId, GeneralConfig.NETEASE_VIP_COOKIE);
    }

    public static String resolveBySongId(long songId, String cookie) {
        String normalizedCookie = cookie == null ? "" : cookie.trim();
        if (songId <= 0 || StringUtils.isBlank(normalizedCookie)) {
            return "";
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", normalizedCookie);
        headers.put("User-Agent", com.github.tartaricacid.netmusic.api.NetEaseMusic.getUserAgent());
        headers.put("Referer", com.github.tartaricacid.netmusic.api.NetEaseMusic.getReferer());
        headers.put("Origin", com.github.tartaricacid.netmusic.api.NetEaseMusic.getOrigin());

        for (String level : LEVELS) {
            String url = String.format(BASE_URL, songId, level);
            try {
                String raw = NetWorker.get(url, headers);
                String musicUrl = parseSongUrl(raw);
                if (StringUtils.isNotBlank(musicUrl)) {
                    return musicUrl;
                }
            } catch (Exception ignored) {
                // Try lower quality if current level fails.
            }
        }
        return "";
    }

    private static String parseSongUrl(String rawJson) {
        if (StringUtils.isBlank(rawJson)) {
            return "";
        }
        JsonElement element = new JsonParser().parse(rawJson);
        if (element == null || !element.isJsonObject()) {
            return "";
        }
        JsonObject root = element.getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        if (data == null || data.size() <= 0) {
            return "";
        }
        JsonObject first = data.get(0).getAsJsonObject();
        if (first == null || !first.has("url")) {
            return "";
        }
        JsonElement urlElement = first.get("url");
        return urlElement == null || urlElement.isJsonNull() ? "" : urlElement.getAsString();
    }
}
