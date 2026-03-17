package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.qq.QqMusicApi;
import com.github.tartaricacid.netmusic.client.config.MusicListManage;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.config.MusicProviderType;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CDBurnerInputParser {
    private static final Pattern ID_REG = Pattern.compile("^\\d{4,}$");
    private static final Pattern DJ_ID_REG = Pattern.compile("^dj/(\\d+)$");
    private static final Pattern URL_1_REG = Pattern.compile("^https://music\\.163\\.com/song\\?id=(\\d+).*$");
    private static final Pattern URL_2_REG = Pattern.compile("^https://music\\.163\\.com/#/song\\?id=(\\d+).*$");
    private static final Pattern DJ_URL_1_REG = Pattern.compile("^https://music\\.163\\.com/dj\\?id=(\\d+).*$");
    private static final Pattern DJ_URL_2_REG = Pattern.compile("^https://music\\.163\\.com/#/dj\\?id=(\\d+).*$");

    private CDBurnerInputParser() {
    }

    public static String normalizeInput(String text) {
        if (GeneralConfig.CD_PROVIDER == MusicProviderType.QQ) {
            return QqMusicApi.normalizeInput(text);
        }
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String trimmed = text.trim();
        Matcher matcher1 = URL_1_REG.matcher(trimmed);
        if (matcher1.find()) {
            return matcher1.group(1);
        }
        Matcher matcher2 = URL_2_REG.matcher(trimmed);
        if (matcher2.find()) {
            return matcher2.group(1);
        }
        Matcher matcher3 = DJ_URL_1_REG.matcher(trimmed);
        if (matcher3.find()) {
            return "dj/" + matcher3.group(1);
        }
        Matcher matcher4 = DJ_URL_2_REG.matcher(trimmed);
        if (matcher4.find()) {
            return "dj/" + matcher4.group(1);
        }
        return trimmed;
    }

    public static ScreenSubmitResult parseSongInfo(String rawInput, boolean readOnly) {
        if (GeneralConfig.CD_PROVIDER == MusicProviderType.QQ) {
            return parseQqSongInfo(rawInput, readOnly);
        }
        return parseNeteaseSongInfo(rawInput, readOnly);
    }

    private static ScreenSubmitResult parseNeteaseSongInfo(String rawInput, boolean readOnly) {
        String input = normalizeInput(rawInput);
        if (StringUtils.isBlank(input)) {
            return ScreenSubmitResult.fail("gui.netmusic.cd_burner.no_music_id");
        }

        Matcher djMatcher = DJ_ID_REG.matcher(input);
        if (djMatcher.find()) {
            try {
                long djId = Long.parseLong(djMatcher.group(1));
                ItemMusicCD.SongInfo song = SongInfoHelper.sanitize(MusicListManage.getDjSong(djId));
                if (song == null) {
                    return ScreenSubmitResult.fail("gui.netmusic.cd_burner.get_info_error");
                }
                song.readOnly = readOnly;
                return ScreenSubmitResult.success(song);
            } catch (Exception e) {
                NetMusic.LOGGER.error("Failed to parse DJ song from input: {}", input, e);
                return ScreenSubmitResult.fail("gui.netmusic.cd_burner.get_info_error");
            }
        }

        if (!ID_REG.matcher(input).matches()) {
            return ScreenSubmitResult.fail("gui.netmusic.cd_burner.music_id_error");
        }

        try {
            long id = Long.parseLong(input);
            ItemMusicCD.SongInfo song = SongInfoHelper.sanitize(MusicListManage.get163Song(id));
            if (song == null) {
                return ScreenSubmitResult.fail("gui.netmusic.cd_burner.get_info_error");
            }
            song.readOnly = readOnly;
            return ScreenSubmitResult.success(song);
        } catch (Exception e) {
            NetMusic.LOGGER.error("Failed to parse NetEase song from input: {}", input, e);
            return ScreenSubmitResult.fail("gui.netmusic.cd_burner.get_info_error");
        }
    }

    private static ScreenSubmitResult parseQqSongInfo(String rawInput, boolean readOnly) {
        String input = QqMusicApi.normalizeInput(rawInput);
        if (StringUtils.isBlank(input)) {
            return ScreenSubmitResult.fail("gui.netmusic.cd_burner.no_music_id");
        }
        if (!QqMusicApi.isValidMid(input)) {
            return ScreenSubmitResult.fail("gui.netmusic.cd_burner.music_id_error");
        }

        try {
            ItemMusicCD.SongInfo song = SongInfoHelper.sanitize(QqMusicApi.resolveSong(input));
            if (song == null) {
                return ScreenSubmitResult.fail("gui.netmusic.cd_burner.get_info_error");
            }
            song.songUrl = QqMusicApi.toSongDetailUrl(input);
            song.readOnly = readOnly;
            return ScreenSubmitResult.success(SongInfoHelper.sanitize(song));
        } catch (Exception e) {
            NetMusic.LOGGER.error("Failed to parse QQ song from input: {}", input, e);
            return ScreenSubmitResult.fail("gui.netmusic.cd_burner.get_info_error");
        }
    }
}
