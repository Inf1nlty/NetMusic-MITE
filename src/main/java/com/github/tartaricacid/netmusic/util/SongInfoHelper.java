package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import org.apache.commons.lang3.StringUtils;

public final class SongInfoHelper {
    private static final int MAX_ARTIST_COUNT = 32;
    private static final int MAX_SONG_TIME_SECONDS = 60 * 60 * 12;
    private static final int MAX_SONG_NAME_LENGTH = 256;
    private static final int MAX_TRANS_NAME_LENGTH = 256;
    private static final int MAX_ARTIST_NAME_LENGTH = 128;

    private SongInfoHelper() {
    }

    public static ItemMusicCD.SongInfo copy(ItemMusicCD.SongInfo source) {
        if (source == null) {
            return null;
        }
        ItemMusicCD.SongInfo copy = new ItemMusicCD.SongInfo();
        copy.songUrl = source.songUrl;
        copy.songName = source.songName;
        copy.songTime = source.songTime;
        copy.transName = source.transName;
        copy.vip = source.vip;
        copy.readOnly = source.readOnly;
        copy.artists.clear();
        if (source.artists != null) {
            copy.artists.addAll(source.artists);
        }
        return copy;
    }

    public static boolean isValid(ItemMusicCD.SongInfo songInfo) {
        return songInfo != null
                && StringUtils.isNotBlank(songInfo.songUrl)
                && StringUtils.isNotBlank(songInfo.songName)
                && songInfo.songTime > 0;
    }

    public static ItemMusicCD.SongInfo sanitize(ItemMusicCD.SongInfo source) {
        ItemMusicCD.SongInfo copy = copy(source);
        if (copy == null) {
            return null;
        }

        copy.songUrl = StringUtils.trimToEmpty(copy.songUrl);
        copy.songName = StringUtils.substring(StringUtils.trimToEmpty(copy.songName), 0, MAX_SONG_NAME_LENGTH);
        copy.transName = StringUtils.substring(StringUtils.trimToEmpty(copy.transName), 0, MAX_TRANS_NAME_LENGTH);
        copy.songTime = Math.max(1, Math.min(MAX_SONG_TIME_SECONDS, copy.songTime));

        copy.artists.clear();
        if (source.artists != null) {
            for (String artist : source.artists) {
                if (copy.artists.size() >= MAX_ARTIST_COUNT) {
                    break;
                }
                String value = StringUtils.substring(StringUtils.trimToEmpty(artist), 0, MAX_ARTIST_NAME_LENGTH);
                if (StringUtils.isNotBlank(value)) {
                    copy.artists.add(value);
                }
            }
        }
        return isValid(copy) ? copy : null;
    }
}
