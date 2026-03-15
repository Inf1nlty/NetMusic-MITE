package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public final class ComputerInputParser {
    private static final Pattern URL_HTTP_REG = Pattern.compile("(http|ftp|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?");
    private static final Pattern URL_FILE_REG = Pattern.compile("^[a-zA-Z]:\\\\(?:[^\\\\/:*?\"<>|\\r\\n]+\\\\)*[^\\\\/:*?\"<>|\\r\\n]*$");
    private static final Pattern TIME_REG = Pattern.compile("^\\d+$");

    private ComputerInputParser() {
    }

    public static ScreenSubmitResult parseSongInfo(String rawUrl, String rawName, String rawTime, boolean readOnly) {
        if (StringUtils.isBlank(rawUrl)) {
            return ScreenSubmitResult.fail("gui.netmusic.computer.url.empty");
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
            if (time <= 0) {
                return ScreenSubmitResult.fail("gui.netmusic.computer.time.not_number");
            }
        } catch (NumberFormatException e) {
            return ScreenSubmitResult.fail("gui.netmusic.computer.time.not_number");
        }

        String urlText = rawUrl.trim();
        if (URL_HTTP_REG.matcher(urlText).matches()) {
            return ScreenSubmitResult.success(new ItemMusicCD.SongInfo(urlText, rawName.trim(), time, readOnly));
        }

        if (URL_FILE_REG.matcher(urlText).matches()) {
            File file = Paths.get(urlText).toFile();
            if (!file.isFile()) {
                return ScreenSubmitResult.fail("gui.netmusic.computer.url.local_file_error");
            }
            try {
                URL url = file.toURI().toURL();
                return ScreenSubmitResult.success(new ItemMusicCD.SongInfo(url.toString(), rawName.trim(), time, readOnly));
            } catch (MalformedURLException e) {
                NetMusic.LOGGER.error("Failed to convert local path to URL: {}", urlText, e);
                return ScreenSubmitResult.fail("gui.netmusic.computer.url.error");
            }
        }

        return ScreenSubmitResult.fail("gui.netmusic.computer.url.error");
    }
}
