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
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class MusicPlayManager {
    public static final String ERROR_404 = "http://music.163.com/404";
    public static final String MUSIC_163_URL = "https://music.163.com/";
    private static final String LOCAL_FILE_PROTOCOL = "file";

    public static void play(String url, String songName, Function<URL, Object> sound) {
        String rawUrl = url;

        String qqMid = QqMusicApi.extractMid(url);
        if (QqMusicApi.isValidMid(qqMid)) {
            try {
                ItemMusicCD.SongInfo qqSong = QqMusicApi.resolveSong(url);
                if (qqSong == null || StringUtils.isBlank(qqSong.songUrl)) {
                    NetMusic.LOGGER.warn("Failed to resolve playable QQ url from input: {}", rawUrl);
                    return;
                }
                url = qqSong.songUrl;
            } catch (Exception e) {
                NetMusic.LOGGER.error("Failed to resolve QQ url from input: {}", rawUrl, e);
                return;
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
            } catch (IOException e) {
                NetMusic.LOGGER.error("Failed to get redirect URL for: {}", url, e);
                return;
            }
        }
        if (url != null) {
            if (url.equals(ERROR_404)) {
                NetMusic.LOGGER.info("Music not found: {}", rawUrl);
                return;
            }
            playMusic(url, songName, sound);
        }
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
