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
}

