package com.github.tartaricacid.netmusic.creativetab;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.util.SongInfoHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BuiltinMusicDiscs {

    private static final String RESOURCE_PATH = "assets/netmusic/music.json";
    private static final Gson GSON = new Gson();
    private static final Type SONG_LIST_TYPE = new TypeToken<List<ItemMusicCD.SongInfo>>() {}.getType();
    private static volatile List<ItemMusicCD.SongInfo> cache;

    private BuiltinMusicDiscs() {}

    public static List<ItemMusicCD.SongInfo> getSongs() {
        List<ItemMusicCD.SongInfo> songs = cache;
        if (songs != null) {
            return songs;
        }
        synchronized (BuiltinMusicDiscs.class) {
            songs = cache;
            if (songs == null) {
                songs = Collections.unmodifiableList(loadSongs());
                cache = songs;
            }
        }
        return songs;
    }

    private static List<ItemMusicCD.SongInfo> loadSongs() {
        InputStream stream = BuiltinMusicDiscs.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (stream == null) {
            return Collections.emptyList();
        }

        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            List<ItemMusicCD.SongInfo> rawSongs = GSON.fromJson(reader, SONG_LIST_TYPE);
            if (rawSongs == null || rawSongs.isEmpty()) {
                return Collections.emptyList();
            }
            List<ItemMusicCD.SongInfo> songs = new ArrayList<>(rawSongs.size());
            for (ItemMusicCD.SongInfo song : rawSongs) {
                ItemMusicCD.SongInfo sanitized = SongInfoHelper.sanitize(song);
                if (sanitized != null) {
                    songs.add(sanitized);
                }
            }
            return songs;
        } catch (Exception e) {
            NetMusic.LOGGER.warn("Failed to load built-in music disc list from {}", RESOURCE_PATH, e);
            return Collections.emptyList();
        }
    }
}
