package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.qq.QqMusicApi;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public final class ComputerInputParser {
    private static final int MAX_SONG_TIME_SECONDS = 60 * 60 * 12;
    private static final Pattern URL_HTTP_REG = Pattern.compile("(http|ftp|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?");
    private static final Pattern URL_FILE_URI_REG = Pattern.compile("^file:/+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_FILE_REG = Pattern.compile("^[a-zA-Z]:\\\\(?:[^\\\\/:*?\"<>|\\r\\n]+\\\\)*[^\\\\/:*?\"<>|\\r\\n]*$");
    private static final Pattern TIME_REG = Pattern.compile("^\\d+$");

    private ComputerInputParser() {
    }

    public static ScreenSubmitResult parseSongInfo(String rawUrl, String rawName, String rawTime, boolean readOnly) {
        if (StringUtils.isBlank(rawUrl)) {
            return ScreenSubmitResult.fail("gui.netmusic.computer.url.empty");
        }

        String urlText = trimQuotes(rawUrl.trim());
        if (shouldTryResolveQq(urlText)) {
            try {
                ItemMusicCD.SongInfo qqSong = SongInfoHelper.sanitize(QqMusicApi.resolveSong(urlText));
                if (qqSong != null) {
                    qqSong.songUrl = QqMusicApi.toSongDetailUrl(urlText);
                    qqSong.readOnly = readOnly;
                    return ScreenSubmitResult.success(SongInfoHelper.sanitize(qqSong));
                }
            } catch (Exception e) {
                // Continue with normal URL parsing when this is not a QQ link.
                NetMusic.LOGGER.debug("Failed to resolve QQ input from computer URL: {}", urlText, e);
            }
        }

        if (StringUtils.isBlank(rawName)) {
            return ScreenSubmitResult.fail("gui.netmusic.computer.name.empty");
        }
        if (StringUtils.isBlank(rawTime)) {
            return ScreenSubmitResult.fail("gui.netmusic.computer.time.empty");
        }

        String timeText = rawTime.trim();
        if (!TIME_REG.matcher(timeText).matches()) {
            return ScreenSubmitResult.fail("gui.netmusic.computer.time.not_number");
        }

        int time;
        try {
            time = Integer.parseInt(timeText);
            if (time <= 0 || time > MAX_SONG_TIME_SECONDS) {
                return ScreenSubmitResult.fail("gui.netmusic.computer.time.not_number");
            }
        } catch (NumberFormatException e) {
            return ScreenSubmitResult.fail("gui.netmusic.computer.time.not_number");
        }

        if (URL_HTTP_REG.matcher(urlText).matches()) {
            return createResult(urlText, rawName.trim(), time, readOnly);
        }

        if (URL_FILE_URI_REG.matcher(urlText).matches()) {
            try {
                URL url = new URL(urlText);
                File file = Paths.get(url.toURI()).toFile();
                if (!file.isFile()) {
                    return ScreenSubmitResult.fail("gui.netmusic.computer.url.local_file_error");
                }
                return createResult(file.toURI().toURL().toString(), rawName.trim(), time, readOnly);
            } catch (MalformedURLException | URISyntaxException e) {
                NetMusic.LOGGER.error("Failed to parse local file url: {}", urlText, e);
                return ScreenSubmitResult.fail("gui.netmusic.computer.url.error");
            }
        }

        if (URL_FILE_REG.matcher(urlText).matches()) {
            File file = Paths.get(urlText).toFile();
            if (!file.isFile()) {
                return ScreenSubmitResult.fail("gui.netmusic.computer.url.local_file_error");
            }
            try {
                URL url = file.toURI().toURL();
                return createResult(url.toString(), rawName.trim(), time, readOnly);
            } catch (MalformedURLException e) {
                NetMusic.LOGGER.error("Failed to convert local path to URL: {}", urlText, e);
                return ScreenSubmitResult.fail("gui.netmusic.computer.url.error");
            }
        }

        return ScreenSubmitResult.fail("gui.netmusic.computer.url.error");
    }

    private static ScreenSubmitResult createResult(String url, String name, int time, boolean readOnly) {
        ItemMusicCD.SongInfo songInfo = new ItemMusicCD.SongInfo(url, name, time, readOnly);
        ItemMusicCD.SongInfo sanitized = SongInfoHelper.sanitize(songInfo);
        return sanitized != null
                ? ScreenSubmitResult.success(sanitized)
                : ScreenSubmitResult.fail("gui.netmusic.computer.url.error");
    }

    private static String trimQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static boolean shouldTryResolveQq(String urlText) {
        if (StringUtils.isBlank(urlText)) {
            return false;
        }
        if (QqMusicApi.isValidMid(urlText)) {
            return true;
        }
        String lower = urlText.toLowerCase();
        return lower.contains("y.qq.com") || lower.contains("qqmusic.qq.com") || lower.contains("i.y.qq.com");
    }
}
