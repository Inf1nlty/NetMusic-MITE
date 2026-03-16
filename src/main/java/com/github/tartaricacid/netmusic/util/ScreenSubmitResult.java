package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;

public final class ScreenSubmitResult {
    private final boolean success;
    private final String messageKey;
    private final ItemMusicCD.SongInfo songInfo;

    private ScreenSubmitResult(boolean success, String messageKey, ItemMusicCD.SongInfo songInfo) {
        this.success = success;
        this.messageKey = messageKey;
        this.songInfo = songInfo;
    }

    public static ScreenSubmitResult success(ItemMusicCD.SongInfo songInfo) {
        return new ScreenSubmitResult(true, null, songInfo);
    }

    public static ScreenSubmitResult fail(String messageKey) {
        return new ScreenSubmitResult(false, messageKey, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public ItemMusicCD.SongInfo getSongInfo() {
        return songInfo;
    }

    public static String mapGuiKeyToCommandKey(String key, String fallback) {
        return resolveFeedbackKey(key, fallback);
    }

    public static String resolveFeedbackKey(String key, String fallback) {
        if (key == null) {
            return fallback;
        }
        if (key.startsWith("command.netmusic.") || key.startsWith("gui.netmusic.") || key.startsWith("message.netmusic.")) {
            // Already a localization key that the caller can display directly.
            return key;
        }
        if ("gui.netmusic.computer.name.empty".equals(key)) {
            return "command.netmusic.music_cd.name.fail";
        }
        if ("gui.netmusic.computer.time.empty".equals(key) || "gui.netmusic.computer.time.not_number".equals(key)) {
            return "command.netmusic.music_cd.time.fail";
        }
        if ("gui.netmusic.computer.url.empty".equals(key) || "gui.netmusic.computer.url.error".equals(key)
                || "gui.netmusic.computer.url.local_file_error".equals(key)) {
            return "command.netmusic.music_cd.addurlcd.fail";
        }
        if ("gui.netmusic.computer.url.not_supported".equals(key)) {
            return "command.netmusic.music_cd.url.not_supported";
        }
        if ("gui.netmusic.cd_burner.cd_read_only".equals(key)) {
            return "command.netmusic.music_cd.cd_read_only";
        }
        if ("gui.netmusic.cd_burner.cd_is_empty".equals(key)
                || "gui.netmusic.computer.cd_is_empty".equals(key)) {
            return "command.netmusic.music_cd.need_writable_cd";
        }
        if ("gui.netmusic.cd_burner.output_not_empty".equals(key)) {
            return "command.netmusic.music_cd.output_not_empty";
        }
        if ("gui.netmusic.computer.cd_read_only".equals(key)) {
            return "command.netmusic.music_cd.cd_read_only";
        }
        if ("gui.netmusic.computer.output_not_empty".equals(key)) {
            return "command.netmusic.music_cd.output_not_empty";
        }
        return fallback;
    }
}
