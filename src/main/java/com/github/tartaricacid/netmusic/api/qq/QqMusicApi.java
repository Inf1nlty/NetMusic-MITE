package com.github.tartaricacid.netmusic.api.qq;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.api.lyric.LyricParser;
import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QqMusicApi {
    private static final int DEFAULT_SONG_TIME_SECONDS = 300;
    private static final Pattern URL_SONG_DETAIL = Pattern.compile("^https?://y\\.qq\\.com/n/ryqq(?:_v2)?/songDetail/([A-Za-z0-9]+).*$");
    private static final Pattern URL_PLAY_SONG = Pattern.compile("^https?://i\\.y\\.qq\\.com/v8/playsong\\.html\\?songmid=([A-Za-z0-9]+).*$");
    private static final Pattern URL_QUERY_MID = Pattern.compile("^https?://.*(?:[?&#](?:songmid|mid|netmusic_songmid)=)([A-Za-z0-9]+).*$");
    private static final Pattern STREAM_FILE_MID = Pattern.compile("(?i)/(?:M800|M500|RS02|C600|C400|C200|C100|AI00|Q000|Q001|F000)([A-Za-z0-9]{4,})\\.(?:mp3|m4a|flac)(?:\\?.*)?(?:#.*)?$");
    private static final Pattern MID_REG = Pattern.compile("^[A-Za-z0-9]{4,}$");
    private static final Pattern UIN_REG = Pattern.compile("(?i)(?:^|;\\s*)(?:uin|p_uin)=o?(\\d+)");
    private static final String DEFAULT_SIP = "http://ws.stream.qqmusic.qq.com/";
    private static final String QQ_LYRIC_API = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg"
            + "?songmid=%s&g_tk=5381&format=json&inCharset=utf8&outCharset=utf-8"
            + "&nobase64=1&notice=0&platform=yqq.json&needNewCode=0";
    private static final String[] QQ_COOKIE_KEYS = new String[]{
            "uin", "p_uin", "qqmusic_uin",
            "qm_keyst", "qqmusic_key",
            "skey", "p_skey"
    };
    private static final Set<String> COOKIE_ATTRIBUTES = Set.of(
            "path", "domain", "expires", "max-age", "httponly", "secure", "samesite", "priority"
    );

    private static final FileCandidate[] QUALITY_CANDIDATES = new FileCandidate[]{
            new FileCandidate("M800", "mp3"),
            new FileCandidate("M500", "mp3"),
            new FileCandidate("RS02", "mp3"),
            new FileCandidate("C600", "m4a"),
            new FileCandidate("C400", "m4a"),
            new FileCandidate("C200", "m4a"),
            new FileCandidate("C100", "m4a"),
            new FileCandidate("AI00", "flac"),
            new FileCandidate("Q000", "flac"),
            new FileCandidate("Q001", "flac"),
            new FileCandidate("F000", "flac")
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

    public static String toSongDetailUrl(String inputOrMid) {
        String mid = extractMid(inputOrMid);
        if (!isValidMid(mid)) {
            return StringUtils.trimToEmpty(inputOrMid);
        }
        return "https://y.qq.com/n/ryqq_v2/songDetail/" + mid;
    }

    public static boolean isValidMid(String input) {
        return StringUtils.isNotBlank(input) && MID_REG.matcher(input).matches();
    }

    @Nullable
    public static String extractMid(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        String normalized = normalizeInput(input);
        if (isValidMid(normalized)) {
            return normalized;
        }
        Matcher matcher = STREAM_FILE_MID.matcher(input.trim());
        if (matcher.find()) {
            String mid = matcher.group(1);
            return isValidMid(mid) ? mid : null;
        }
        return null;
    }

    public static ItemMusicCD.SongInfo resolveSong(String input) throws Exception {
        return resolveSong(input, GeneralConfig.QQ_VIP_COOKIE);
    }

    public static ItemMusicCD.SongInfo resolveSong(String input, String cookieText) throws Exception {
        String mid = extractMid(input);
        if (!isValidMid(mid)) {
            debug("resolveSong invalid input={} normalizedMid={}", shorten(input, 160), mid);
            return null;
        }
        debug("resolveSong input={} mid={}", shorten(input, 160), mid);

        String cookie = sanitizeCookie(cookieText);
        String uin = extractUin(cookie);
        TrackInfo trackInfo = getTrackInfoByMid(mid, cookie, uin);
        if (trackInfo == null || StringUtils.isBlank(trackInfo.songName)) {
            debug("resolveSong trackInfo empty mid={}", mid);
            return null;
        }
        debug("resolveSong track mid={} name={} vip={} interval={} mediaMid={} artists={}",
                mid, trackInfo.songName, trackInfo.vip, trackInfo.interval, trackInfo.mediaMid, trackInfo.artists == null ? 0 : trackInfo.artists.size());

        String mediaMid = StringUtils.isBlank(trackInfo.mediaMid) ? mid : trackInfo.mediaMid;
        JsonObject vkeyData = requestVkeyData(mid, mediaMid, buildRequestHeaders(cookie), uin);
        debug("resolveSong vkey summary mid={} => {}", mid, summarizeVkeyData(vkeyData));
        String baseUrl = resolveBaseUrl(vkeyData);
        String purl = selectBestPurl(vkeyData == null ? null : vkeyData.getAsJsonArray("midurlinfo"));
        if (StringUtils.isBlank(purl)) {
            debug("resolveSong no playable purl mid={}", mid);
            return null;
        }
        debug("resolveSong chosen mid={} baseUrl={} purl={}", mid, shorten(baseUrl, 96), shorten(purl, 196));

        ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo();
        info.songUrl = withSongMidFragment(baseUrl + purl, mid);
        info.songName = trackInfo.songName;
        info.songTime = normalizeSongTime(trackInfo.interval);
        info.vip = trackInfo.vip;
        if (trackInfo.artists != null) {
            info.artists.addAll(trackInfo.artists);
        }
        return info;
    }

    @Nullable
    public static LyricRecord resolveLyric(String input, String songName) {
        String mid = extractMid(input);
        if (!isValidMid(mid)) {
            return null;
        }
        try {
            String cookie = sanitizeCookie(GeneralConfig.QQ_VIP_COOKIE);
            String uin = extractUin(cookie);
            LyricRecord record = resolveLyricByMid(mid, songName, cookie, uin);
            if (record != null) {
                return record;
            }

            String searchedMid = searchSongMidByName(songName, cookie, uin);
            if (isValidMid(searchedMid) && !StringUtils.equalsIgnoreCase(searchedMid, mid)) {
                return resolveLyricByMid(searchedMid, songName, cookie, uin);
            }
            return null;
        } catch (Exception e) {
            NetMusic.LOGGER.warn("Failed to resolve QQ lyric for {}", input, e);
            return null;
        }
    }

    @Nullable
    private static LyricRecord resolveLyricByMid(String mid, String songName, String cookie, String uin) {
        if (!isValidMid(mid)) {
            return null;
        }
        JsonObject root = requestLyricByMusicu(mid, cookie, uin);
        if (root == null) {
            root = requestLyricByLegacy(mid, cookie);
        }
        if (root == null) {
            return null;
        }
        String original = decodeLyricField(getStringOrEmpty(root, "lyric"));
        if (StringUtils.isBlank(original)) {
            return null;
        }
        String translated = decodeLyricField(getStringOrEmpty(root, "trans"));
        return LyricParser.parseLrcText(original, translated, songName);
    }

    private static TrackInfo getTrackInfoByMid(String mid, String cookie, String uin) throws Exception {
        String payload = "{\"req_1\":{\"module\":\"music.pf_song_detail_svr\",\"method\":\"get_song_detail\",\"param\":{\"song_mid\":\""
                + mid
                + "\",\"song_id\":0},\"loginUin\":\"" + safeUin(uin) + "\",\"comm\":{\"uin\":\"" + safeUin(uin) + "\",\"format\":\"json\",\"ct\":24,\"cv\":0}}}";
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

    private static JsonObject requestVkeyData(String songMid, String mediaMid, Map<String, String> requestHeaders, String uin) throws Exception {
        JsonArray filenameList = new JsonArray();
        JsonArray songMidList = new JsonArray();
        JsonArray songTypeList = new JsonArray();
        for (FileCandidate candidate : QUALITY_CANDIDATES) {
            filenameList.add(new JsonPrimitive(candidate.buildFilename(mediaMid)));
            songMidList.add(new JsonPrimitive(songMid));
            songTypeList.add(new JsonPrimitive(0));
        }

        JsonObject param = new JsonObject();
        param.add("filename", filenameList);
        param.addProperty("guid", "10000");
        param.add("songmid", songMidList);
        param.add("songtype", songTypeList);
        param.addProperty("uin", safeUin(uin));
        param.addProperty("loginflag", 1);
        param.addProperty("platform", "20");

        JsonObject req = new JsonObject();
        req.addProperty("module", "vkey.GetVkeyServer");
        req.addProperty("method", "CgiGetVkey");
        req.add("param", param);

        JsonObject comm = new JsonObject();
        comm.addProperty("uin", safeUin(uin));
        comm.addProperty("format", "json");
        comm.addProperty("ct", 24);
        comm.addProperty("cv", 0);

        JsonObject body = new JsonObject();
        body.add("req_1", req);
        body.addProperty("loginUin", safeUin(uin));
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
        int limit = midurlinfo.size();
        String bestPurl = "";
        int bestRank = -1;
        for (int i = 0; i < limit; i++) {
            JsonObject info = midurlinfo.get(i).getAsJsonObject();
            if (info != null && info.has("purl")) {
                String purl = info.get("purl").getAsString();
                if (StringUtils.isNotBlank(purl)) {
                    int rank = getPlayableRank(purl, getStringOrEmpty(info, "filename"));
                    debug("selectBestPurl candidate rank={} filename={} purl={}",
                            rank, getStringOrEmpty(info, "filename"), shorten(purl, 160));
                    if (rank > bestRank) {
                        bestRank = rank;
                        bestPurl = purl;
                    }
                }
            }
        }
        debug("selectBestPurl selected rank={} purl={}", bestRank, shorten(bestPurl, 180));
        return bestPurl;
    }

    private static int getPlayableRank(String purl, String filename) {
        if (StringUtils.isBlank(purl)) {
            return -1;
        }
        String extension = detectAudioExtension(purl, filename);
        if ("mp3".equals(extension)) {
            return 100 + getQualityBonus(purl, filename);
        }
        if ("flac".equals(extension)) {
            // FLAC works for some environments but may fail to seek/skip reliably in others.
            // Keep it as fallback behind MP3 for cross-client stability.
            return 40 + getQualityBonus(purl, filename);
        }
        return -1;
    }

    private static int getQualityBonus(String purl, String filename) {
        String merged = ((purl == null ? "" : purl) + " " + (filename == null ? "" : filename)).toLowerCase(Locale.ROOT);
        if (merged.contains("m800")) {
            return 30;
        }
        if (merged.contains("m500")) {
            return 20;
        }
        if (merged.contains("rs02")) {
            return 10;
        }
        // FLAC family bonus remains lower than high bitrate MP3.
        if (merged.contains("f000")) {
            return 6;
        }
        if (merged.contains("q001")) {
            return 5;
        }
        if (merged.contains("q000")) {
            return 4;
        }
        if (merged.contains("ai00")) {
            return 3;
        }
        return 0;
    }

    private static String detectAudioExtension(String purl, String filename) {
        String normalizedPurl = stripQueryAndFragment(purl).toLowerCase(Locale.ROOT);
        if (normalizedPurl.endsWith(".mp3")) {
            return "mp3";
        }
        if (normalizedPurl.endsWith(".flac")) {
            return "flac";
        }
        String normalizedFilename = (filename == null ? "" : filename).toLowerCase(Locale.ROOT);
        if (normalizedFilename.endsWith(".mp3")) {
            return "mp3";
        }
        if (normalizedFilename.endsWith(".flac")) {
            return "flac";
        }
        if (hasMp3PrefixMarker(normalizedPurl) || hasMp3PrefixMarker(normalizedFilename)) {
            return "mp3";
        }
        if (hasFlacPrefixMarker(normalizedPurl) || hasFlacPrefixMarker(normalizedFilename)) {
            return "flac";
        }
        return "";
    }

    private static boolean hasMp3PrefixMarker(String text) {
        return text.contains("m800") || text.contains("m500") || text.contains("rs02");
    }

    private static boolean hasFlacPrefixMarker(String text) {
        return text.contains("ai00") || text.contains("q000") || text.contains("q001") || text.contains("f000");
    }

    private static String stripQueryAndFragment(String urlPart) {
        if (StringUtils.isBlank(urlPart)) {
            return "";
        }
        int cut = urlPart.length();
        int queryIndex = urlPart.indexOf('?');
        if (queryIndex >= 0 && queryIndex < cut) {
            cut = queryIndex;
        }
        int fragmentIndex = urlPart.indexOf('#');
        if (fragmentIndex >= 0 && fragmentIndex < cut) {
            cut = fragmentIndex;
        }
        return urlPart.substring(0, cut);
    }

    private static int normalizeSongTime(int rawTimeSecond) {
        return rawTimeSecond > 0 ? rawTimeSecond : DEFAULT_SONG_TIME_SECONDS;
    }

    private static String withSongMidFragment(String url, String mid) {
        if (StringUtils.isBlank(url) || !isValidMid(mid)) {
            return url;
        }
        int fragmentIndex = url.indexOf('#');
        String base = fragmentIndex >= 0 ? url.substring(0, fragmentIndex) : url;
        return base + "#netmusic_songmid=" + mid;
    }

    private static String searchSongMidByName(String songName, String cookie, String uin) {
        if (StringUtils.isBlank(songName)) {
            return null;
        }
        try {
            JsonObject comm = new JsonObject();
            comm.addProperty("ct", "19");
            comm.addProperty("cv", "1859");
            comm.addProperty("uin", safeUin(uin));

            JsonObject param = new JsonObject();
            param.addProperty("grp", 1);
            param.addProperty("num_per_page", 10);
            param.addProperty("page_num", 1);
            param.addProperty("query", songName);
            param.addProperty("search_type", 0);

            JsonObject req = new JsonObject();
            req.addProperty("method", "DoSearchForQQMusicDesktop");
            req.addProperty("module", "music.search.SearchCgiService");
            req.add("param", param);

            JsonObject body = new JsonObject();
            body.add("comm", comm);
            body.add("req", req);

            JsonObject tree = parseJsonObject(postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", body.toString(), buildRequestHeaders(cookie)));
            JsonObject reqObj = getObject(tree, "req");
            JsonObject data = getObject(reqObj, "data");
            JsonObject bodyObj = getObject(data, "body");
            JsonObject songObj = getObject(bodyObj, "song");
            JsonArray list = songObj == null ? null : songObj.getAsJsonArray("list");
            if (list == null || list.size() <= 0) {
                return null;
            }

            String normalizedQuery = normalizeSongName(songName);
            String firstMid = null;
            for (JsonElement element : list) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject song = element.getAsJsonObject();
                String mid = getStringOrEmpty(song, "mid");
                if (!isValidMid(mid)) {
                    continue;
                }
                if (firstMid == null) {
                    firstMid = mid;
                }
                String foundName = normalizeSongName(getStringOrEmpty(song, "name"));
                if (StringUtils.isNotBlank(foundName) && foundName.equals(normalizedQuery)) {
                    return mid;
                }
            }
            return firstMid;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeSongName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String postJson(String url, String body, Map<String, String> requestHeaders) throws IOException {
        debug("POST {} payload={}", url, shorten(body, 600));
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
        debug("POST {} response={}", url, shorten(result.toString(), 1200));
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
        JsonElement element = new JsonParser().parse(json);
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
        if (cookie == null) {
            return "";
        }
        String text = cookie.trim();
        if (text.isEmpty() || !text.contains("=")) {
            return text;
        }

        Map<String, String> parsed = parseCookiePairs(text);
        if (parsed.isEmpty()) {
            return text;
        }

        boolean hasUin = containsKey(parsed, "uin") || containsKey(parsed, "p_uin");
        boolean hasKey = containsKey(parsed, "qm_keyst") || containsKey(parsed, "qqmusic_key");
        if (!hasUin || !hasKey) {
            return text;
        }
        String normalized = joinCookiePairs(parsed);
        return normalized.isEmpty() ? text : normalized;
    }

    private static String extractUin(String cookie) {
        if (StringUtils.isBlank(cookie)) {
            return "0";
        }
        Matcher matcher = UIN_REG.matcher(cookie);
        if (!matcher.find()) {
            return "0";
        }
        String uin = matcher.group(1);
        return StringUtils.isBlank(uin) ? "0" : uin.trim();
    }

    private static String safeUin(String uin) {
        return StringUtils.isBlank(uin) ? "0" : uin;
    }

    @Nullable
    private static JsonObject requestLyricByMusicu(String mid, String cookie, String uin) {
        try {
            JsonObject param = new JsonObject();
            param.addProperty("songMID", mid);
            param.addProperty("songID", 0);
            param.addProperty("trans_t", 0);
            param.addProperty("roma_t", 0);
            param.addProperty("crypt", 0);
            param.addProperty("lrc_t", 0);
            param.addProperty("qrc_t", 0);

            JsonObject req = new JsonObject();
            req.addProperty("module", "music.musichallSong.PlayLyricInfo");
            req.addProperty("method", "GetPlayLyricInfo");
            req.add("param", param);

            JsonObject comm = new JsonObject();
            comm.addProperty("uin", safeUin(uin));
            comm.addProperty("format", "json");
            comm.addProperty("ct", 24);
            comm.addProperty("cv", 0);

            JsonObject body = new JsonObject();
            body.add("req_1", req);
            body.addProperty("loginUin", safeUin(uin));
            body.add("comm", comm);

            Map<String, String> headers = buildRequestHeaders(cookie);
            headers.put("Referer", "https://y.qq.com/");
            JsonObject tree = parseJsonObject(postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", body.toString(), headers));
            JsonObject req1 = getObject(tree, "req_1");
            JsonObject data = getObject(req1, "data");
            if (data != null) {
                return data;
            }
            JsonObject playLyricInfo = getObject(tree, "PlayLyricInfo");
            if (playLyricInfo != null) {
                JsonObject nestedData = getObject(playLyricInfo, "data");
                if (nestedData != null) {
                    return nestedData;
                }
            }
            if (tree != null && tree.has("lyric")) {
                return tree;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static JsonObject requestLyricByLegacy(String mid, String cookie) {
        try {
            Map<String, String> headers = buildRequestHeaders(cookie);
            headers.put("Referer", "https://y.qq.com/portal/player.html");
            headers.put("Origin", "https://y.qq.com");
            String json = NetWorker.get(String.format(Locale.ROOT, QQ_LYRIC_API, mid), headers);
            return parseJsonObject(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static String decodeLyricField(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("[") || text.contains("\n")) {
            return text;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            String decodedText = new String(decoded, StandardCharsets.UTF_8);
            return decodedText.trim();
        } catch (Exception ignored) {
            return text;
        }
    }

    private static boolean containsKey(Map<String, String> pairs, String key) {
        for (String k : pairs.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> parseCookiePairs(String raw) {
        Map<String, String> pairs = new LinkedHashMap<String, String>();
        String[] segments = raw.split("[;\\n]");
        for (String segment : segments) {
            String item = segment == null ? "" : segment.trim();
            if (item.isEmpty()) {
                continue;
            }
            int eq = item.indexOf('=');
            if (eq <= 0 || eq >= item.length() - 1) {
                continue;
            }
            String key = item.substring(0, eq).trim();
            String value = item.substring(eq + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if (COOKIE_ATTRIBUTES.contains(lower)) {
                continue;
            }
            pairs.put(key, value);
        }
        return pairs;
    }

    private static Map<String, String> pickCookiePairs(Map<String, String> source, String[] orderedKeys) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String wantedKey : orderedKeys) {
            String actualKey = findActualKey(source, wantedKey);
            if (actualKey == null) {
                continue;
            }
            String value = source.get(actualKey);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            result.put(actualKey, value);
        }
        return result;
    }

    private static String findActualKey(Map<String, String> pairs, String key) {
        for (String existingKey : pairs.keySet()) {
            if (existingKey.equalsIgnoreCase(key)) {
                return existingKey;
            }
        }
        return null;
    }

    private static String joinCookiePairs(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return builder.toString();
    }

    private static String summarizeVkeyData(JsonObject data) {
        if (data == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        JsonArray midurlinfo = data.getAsJsonArray("midurlinfo");
        int size = midurlinfo == null ? 0 : midurlinfo.size();
        sb.append("midurlinfo=").append(size);
        if (midurlinfo != null) {
            int limit = Math.min(6, midurlinfo.size());
            for (int i = 0; i < limit; i++) {
                JsonElement element = midurlinfo.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String filename = getStringOrEmpty(item, "filename");
                String purl = getStringOrEmpty(item, "purl");
                sb.append(" | ").append(i).append(":")
                        .append(filename)
                        .append(" purl=")
                        .append(StringUtils.isBlank(purl) ? 0 : purl.length());
            }
        }
        return sb.toString();
    }

    private static void debug(String pattern, Object... args) {
        if (!GeneralConfig.ENABLE_DEBUG_MODE) {
            return;
        }
        NetMusic.LOGGER.info("[NetMusic Debug][QQ] " + pattern, args);
    }

    private static String shorten(String text, int maxLen) {
        if (text == null) {
            return "null";
        }
        if (maxLen <= 0 || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...(len=" + text.length() + ")";
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
