package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.netease.NeteaseVipMusicApi;
import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.api.qq.QqMusicApi;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class MusicPlayManager {
    public static final String ERROR_404 = "http://music.163.com/404";
    public static final String MUSIC_163_URL = "https://music.163.com/";
    private static final String LOCAL_FILE_PROTOCOL = "file";
    private static final Object RESOLVE_LOCK = new Object();
    private static volatile int resolveSession = 0;

    public static void play(String url, String songName, Function<URL, Object> sound) {
        final int session;
        synchronized (RESOLVE_LOCK) {
            session = ++resolveSession;
        }
        final String inputUrl = url;
        final String inputSongName = songName;
        Thread resolver = new Thread(() -> resolveAndPlay(session, inputUrl, inputSongName, sound), "NetMusic-Resolve");
        resolver.setDaemon(true);
        resolver.start();
    }

    private static void resolveAndPlay(int session, String url, String songName, Function<URL, Object> sound) {
        String rawUrl = url;
        if (GeneralConfig.ENABLE_DEBUG_MODE) {
            NetMusic.LOGGER.info("[NetMusic Debug][Play] request url={} song={}", rawUrl, songName);
        }

        boolean directAudio = isDirectAudioUrl(url);
        if (shouldResolveQqUrl(url)) {
            try {
                ItemMusicCD.SongInfo qqSong = QqMusicApi.resolveSong(url);
                if (qqSong == null || StringUtils.isBlank(qqSong.songUrl)) {
                    NetMusic.LOGGER.warn("Failed to resolve playable QQ url from input: {}", rawUrl);
                    if (!directAudio) {
                        return;
                    }
                    if (GeneralConfig.ENABLE_DEBUG_MODE) {
                        NetMusic.LOGGER.info("[NetMusic Debug][Play] Keep original direct QQ url due resolve miss: {}", rawUrl);
                    }
                } else {
                    if (GeneralConfig.ENABLE_DEBUG_MODE) {
                        NetMusic.LOGGER.info("[NetMusic Debug][Play] QQ resolved {} -> {}", rawUrl, qqSong.songUrl);
                    }
                    url = qqSong.songUrl;
                }
            } catch (Exception e) {
                NetMusic.LOGGER.error("Failed to resolve QQ url from input: {}", rawUrl, e);
                if (!directAudio) {
                    return;
                }
                if (GeneralConfig.ENABLE_DEBUG_MODE) {
                    NetMusic.LOGGER.info("[NetMusic Debug][Play] Keep original direct QQ url after resolve exception: {}", rawUrl);
                }
            }
        }

        if (url.startsWith(MUSIC_163_URL)) {
            if (GeneralConfig.hasNeteaseVipCookie()) {
                String vipUrl = NeteaseVipMusicApi.resolveByOuterUrl(url);
                if (StringUtils.isNotBlank(vipUrl)) {
                    url = vipUrl;
                }
            }
        }
        if (url.startsWith(MUSIC_163_URL)) {
            try {
                url = NetWorker.getRedirectUrl(url, NetMusic.NET_EASE_WEB_API.getRequestPropertyData());
                if (GeneralConfig.ENABLE_DEBUG_MODE) {
                    NetMusic.LOGGER.info("[NetMusic Debug][Play] NetEase redirect {} -> {}", rawUrl, url);
                }
            } catch (IOException e) {
                NetMusic.LOGGER.error("Failed to get redirect URL for: {}", url, e);
                return;
            }
        }
        if (url == null || url.equals(ERROR_404)) {
            if (url != null && url.equals(ERROR_404)) {
                NetMusic.LOGGER.info("Music not found: {}", rawUrl);
            }
            return;
        }
        if (!isCurrentSession(session)) {
            return;
        }
        playMusic(url, songName, sound);
    }

    private static boolean isCurrentSession(int session) {
        synchronized (RESOLVE_LOCK) {
            return session == resolveSession;
        }
    }

    private static boolean shouldResolveQqUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String text = url.trim();
        String mid = QqMusicApi.extractMid(text);
        boolean directAudio = isDirectAudioUrl(text);
        if (directAudio) {
            // For direct QQ media URLs, only re-resolve when an explicit songmid marker exists.
            return hasExplicitSongMidMarker(text) && QqMusicApi.isValidMid(mid) && isQqDomain(text);
        }
        if (!QqMusicApi.isValidMid(mid)) {
            return false;
        }

        if (isQqDomain(text)) {
            return true;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        boolean hasProtocol = lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:");
        return !hasProtocol;
    }

    private static boolean isDirectAudioUrl(String url) {
        String normalized = stripQueryAndFragment(url).toLowerCase(Locale.ROOT);
        return normalized.endsWith(".mp3")
                || normalized.endsWith(".flac")
                || normalized.endsWith(".m4a")
                || normalized.endsWith(".wav")
                || normalized.endsWith(".ogg");
    }

    private static String stripQueryAndFragment(String url) {
        if (url == null) {
            return "";
        }
        int end = url.length();
        int query = url.indexOf('?');
        if (query >= 0 && query < end) {
            end = query;
        }
        int fragment = url.indexOf('#');
        if (fragment >= 0 && fragment < end) {
            end = fragment;
        }
        return url.substring(0, end);
    }

    private static boolean isQqDomain(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("y.qq.com")
                || lower.contains("i.y.qq.com")
                || lower.contains("qqmusic.qq.com")
                || lower.contains("aqqmusic.tc.qq.com")
                || lower.contains("stream.qqmusic.qq.com")
                || lower.contains("ws.stream.qqmusic.qq.com");
    }

    private static boolean hasExplicitSongMidMarker(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("netmusic_songmid=")
                || lower.contains("songmid=")
                || lower.contains("mid=");
    }

    private static void playMusic(String url, String songName, Function<URL, Object> sound) {
        final URL urlFinal;
        try {
            urlFinal = new URL(url);
            // 如果是本地文件
            if (urlFinal.getProtocol().equals(LOCAL_FILE_PROTOCOL)) {
                File file = new File(urlFinal.toURI());
                if (!file.exists()) {
                    NetMusic.LOGGER.info("File not found: {}", url);
                    return;
                }
            }
            sound.apply(urlFinal);
        } catch (MalformedURLException | URISyntaxException e) {
            NetMusic.LOGGER.error("Malformed URL: {}", url, e);
        }
    }
}
