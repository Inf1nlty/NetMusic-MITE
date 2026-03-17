package com.github.tartaricacid.netmusic.api.qq;

import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QqMusicApi {
    private static final Pattern URL_SONG_DETAIL = Pattern.compile("^https?://y\\.qq\\.com/n/ryqq/songDetail/([A-Za-z0-9]+).*$");
    private static final Pattern URL_PLAY_SONG = Pattern.compile("^https?://i\\.y\\.qq\\.com/v8/playsong\\.html\\?songmid=([A-Za-z0-9]+).*$");
    private static final Pattern URL_QUERY_MID = Pattern.compile("^https?://.*[?&](?:songmid|mid)=([A-Za-z0-9]+).*$");
    private static final Pattern MID_REG = Pattern.compile("^[A-Za-z0-9]{4,}$");
    private static final String DEFAULT_SIP = "http://ws.stream.qqmusic.qq.com/";

    private static final FileCandidate[] QUALITY_CANDIDATES = new FileCandidate[]{
            new FileCandidate("AI00", "flac"),
            new FileCandidate("Q000", "flac"),
            new FileCandidate("Q001", "flac"),
            new FileCandidate("F000", "flac"),
            new FileCandidate("M800", "mp3"),
            new FileCandidate("M500", "mp3"),
            new FileCandidate("RS02", "mp3"),
            new FileCandidate("C600", "m4a"),
            new FileCandidate("C400", "m4a"),
            new FileCandidate("C200", "m4a"),
            new FileCandidate("C100", "m4a")
    };

    private QqMusicApi() {
    }

    public static String normalizeInput(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String trimmed = text.trim();
        Matcher matcher = URL_SONG_DETAIL.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = URL_PLAY_SONG.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = URL_QUERY_MID.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return trimmed;
    }

    public static boolean isValidMid(String input) {
        return MID_REG.matcher(input).matches();
    }

    public static ItemMusicCD.SongInfo resolveSong(String input) throws Exception {
        String mid = normalizeInput(input);
        if (!isValidMid(mid)) {
            return null;
        }

        String cookie = sanitizeCookie(GeneralConfig.QQ_VIP_COOKIE);
        TrackInfo trackInfo = getTrackInfoByMid(mid, cookie);
        if (trackInfo == null || StringUtils.isBlank(trackInfo.songName) || trackInfo.interval <= 0) {
            return null;
        }

        String mediaMid = StringUtils.isBlank(trackInfo.mediaMid) ? mid : trackInfo.mediaMid;
        JsonObject vkeyData = requestVkeyData(mid, mediaMid, buildRequestHeaders(cookie));
        String baseUrl = resolveBaseUrl(vkeyData);
        String purl = selectBestPurl(vkeyData == null ? null : vkeyData.getAsJsonArray("midurlinfo"));
        if (StringUtils.isBlank(purl)) {
            return null;
        }

        ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo();
        info.songUrl = baseUrl + purl;
        info.songName = trackInfo.songName;
        info.songTime = trackInfo.interval;
        info.vip = trackInfo.vip;
        if (trackInfo.artists != null) {
            info.artists.addAll(trackInfo.artists);
        }
        return info;
    }

    private static TrackInfo getTrackInfoByMid(String mid, String cookie) throws Exception {
        String payload = "{\"req_1\":{\"module\":\"music.pf_song_detail_svr\",\"method\":\"get_song_detail\",\"param\":{\"song_mid\":\""
                + mid
                + "\",\"song_id\":0},\"loginUin\":\"0\",\"comm\":{\"uin\":\"0\",\"format\":\"json\",\"ct\":24,\"cv\":0}}}";
        JsonObject tree = parseJsonObject(postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", payload, buildRequestHeaders(cookie)));
        JsonObject req1 = getObject(tree, "req_1");
        JsonObject data = getObject(req1, "data");
        JsonObject trackInfo = getObject(data, "track_info");
        if (trackInfo == null) {
            return null;
        }

        String songName = getStringOrEmpty(trackInfo, "name");
        int interval = getIntOrDefault(trackInfo, "interval", 0);
        boolean vip = false;
        JsonObject pay = getObject(trackInfo, "pay");
        if (pay != null) {
            vip = getIntOrDefault(pay, "pay_play", 0) == 1;
        }

        String mediaMid = "";
        JsonObject file = getObject(trackInfo, "file");
        if (file != null) {
            mediaMid = getStringOrEmpty(file, "media_mid");
        }

        ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo();
        JsonArray singerArray = trackInfo.getAsJsonArray("singer");
        if (singerArray != null) {
            for (JsonElement singerElement : singerArray) {
                if (singerElement == null || !singerElement.isJsonObject()) {
                    continue;
                }
                String artistName = getStringOrEmpty(singerElement.getAsJsonObject(), "name");
                if (StringUtils.isNotBlank(artistName)) {
                    info.artists.add(artistName);
                }
            }
        }
        return new TrackInfo(songName, interval, mediaMid, vip, info.artists);
    }

    private static JsonObject requestVkeyData(String songMid, String mediaMid, Map<String, String> requestHeaders) throws Exception {
        JsonArray filenameList = new JsonArray();
        JsonArray songMidList = new JsonArray();
        JsonArray songTypeList = new JsonArray();
        for (FileCandidate candidate : QUALITY_CANDIDATES) {
            filenameList.add(candidate.buildFilename(mediaMid));
            songMidList.add(songMid);
            songTypeList.add(0);
        }

        JsonObject param = new JsonObject();
        param.add("filename", filenameList);
        param.addProperty("guid", "10000");
        param.add("songmid", songMidList);
        param.add("songtype", songTypeList);
        param.addProperty("uin", "0");
        param.addProperty("loginflag", 1);
        param.addProperty("platform", "20");

        JsonObject req = new JsonObject();
        req.addProperty("module", "vkey.GetVkeyServer");
        req.addProperty("method", "CgiGetVkey");
        req.add("param", param);

        JsonObject comm = new JsonObject();
        comm.addProperty("uin", "0");
        comm.addProperty("format", "json");
        comm.addProperty("ct", 24);
        comm.addProperty("cv", 0);

        JsonObject body = new JsonObject();
        body.add("req_1", req);
        body.addProperty("loginUin", "0");
        body.add("comm", comm);

        JsonObject tree = parseJsonObject(postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", body.toString(), requestHeaders));
        JsonObject req1 = getObject(tree, "req_1");
        return getObject(req1, "data");
    }

    private static String resolveBaseUrl(JsonObject data) {
        if (data != null && data.has("sip")) {
            JsonArray sip = data.getAsJsonArray("sip");
            if (sip != null && sip.size() > 0) {
                String value = sip.get(0).getAsString();
                if (StringUtils.isNotBlank(value)) {
                    return value.endsWith("/") ? value : value + "/";
                }
            }
        }
        return DEFAULT_SIP;
    }

    private static String selectBestPurl(JsonArray midurlinfo) {
        if (midurlinfo == null) {
            return "";
        }
        int limit = Math.min(midurlinfo.size(), QUALITY_CANDIDATES.length);
        for (int i = 0; i < limit; i++) {
            JsonObject info = midurlinfo.get(i).getAsJsonObject();
            if (info != null && info.has("purl")) {
                String purl = info.get("purl").getAsString();
                if (StringUtils.isNotBlank(purl)) {
                    return purl;
                }
            }
        }
        return "";
    }

    private static String postJson(String url, String body, Map<String, String> requestHeaders) throws IOException {
        URLConnection connection = new URL(url).openConnection(NetWorker.getProxyFromConfig());
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        if (!requestHeaders.containsKey("Content-Type")) {
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        }
        connection.setConnectTimeout(12000);
        connection.setDoOutput(true);
        connection.setDoInput(true);

        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private static Map<String, String> buildRequestHeaders(String cookie) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Content-Type", "application/json;charset=utf-8");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("Referer", "https://y.qq.com/");
        if (StringUtils.isNotBlank(cookie)) {
            headers.put("Cookie", cookie);
        }
        return headers;
    }

    private static JsonObject parseJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        JsonElement element = JsonParser.parseString(json);
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        return element.getAsJsonObject();
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(key);
    }

    private static String getStringOrEmpty(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static int getIntOrDefault(JsonObject object, String key, int defaultValue) {
        if (object == null || !object.has(key)) {
            return defaultValue;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static String sanitizeCookie(String cookie) {
        return cookie == null ? "" : cookie.trim();
    }

    private static final class FileCandidate {
        private final String prefix;
        private final String extension;

        private FileCandidate(String prefix, String extension) {
            this.prefix = prefix;
            this.extension = extension;
        }

        private String buildFilename(String mediaMid) {
            return prefix + mediaMid + "." + extension;
        }
    }

    private static final class TrackInfo {
        private final String songName;
        private final int interval;
        private final String mediaMid;
        private final boolean vip;
        private final java.util.List<String> artists;

        private TrackInfo(String songName, int interval, String mediaMid, boolean vip, java.util.List<String> artists) {
            this.songName = songName;
            this.interval = interval;
            this.mediaMid = mediaMid;
            this.vip = vip;
            this.artists = artists;
        }
    }
}
