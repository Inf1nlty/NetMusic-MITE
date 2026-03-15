package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;

import javax.annotation.Nullable;
import java.net.URL;

public class NetMusicSound {
    private final int x;
    private final int y;
    private final int z;
    private final URL songUrl;
    private final int timeSecond;
    private final @Nullable LyricRecord lyricRecord;

    public NetMusicSound(int x, int y, int z, URL songUrl, int timeSecond, @Nullable LyricRecord lyricRecord) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.songUrl = songUrl;
        this.timeSecond = timeSecond;
        this.lyricRecord = lyricRecord;
    }

    public URL getSongUrl() {
        return this.songUrl;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getTimeSecond() {
        return this.timeSecond;
    }

    public @Nullable LyricRecord getLyricRecord() {
        return this.lyricRecord;
    }
}
