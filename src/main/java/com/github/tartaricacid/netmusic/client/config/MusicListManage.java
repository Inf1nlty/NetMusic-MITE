package com.github.tartaricacid.netmusic.client.config;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.ExtraMusicList;
import com.github.tartaricacid.netmusic.api.pojo.NetEaseMusicList;
import com.github.tartaricacid.netmusic.api.pojo.NetEaseMusicSong;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class MusicListManage {
    private static final int MAX_NUM = 100;
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_DIR = Paths.get("config").resolve("net_music");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("music.json");
    public static List<ItemMusicCD.SongInfo> SONGS = Lists.newArrayList();

    private MusicListManage() {
    }

    public static void loadConfigSongs() throws IOException {
        if (!Files.isDirectory(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }

        File file = CONFIG_FILE.toFile();
        if (!Files.exists(CONFIG_FILE)) {
            SONGS = Lists.newArrayList();
            return;
        }

        try (InputStream stream = Files.newInputStream(file.toPath())) {
            SONGS = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8),
                    new TypeToken<List<ItemMusicCD.SongInfo>>() {
                    }.getType());
        }
        if (SONGS == null) {
            SONGS = Lists.newArrayList();
        }
    }

    public static ItemMusicCD.SongInfo get163Song(long id) throws Exception {
        NetEaseMusicSong pojo = GSON.fromJson(NetMusic.NET_EASE_WEB_API.song(id), NetEaseMusicSong.class);
        return new ItemMusicCD.SongInfo(pojo);
    }

    public static ItemMusicCD.SongInfo getDjSong(long id) throws Exception {
        String result = NetMusic.NET_EASE_WEB_API.dj(id);
        JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
        JsonObject program = jsonObject.getAsJsonObject("program");
        if (program == null) {
            NetMusic.LOGGER.error("Failed to get DJ song info, program is null for id: {}", id);
            return new ItemMusicCD.SongInfo();
        }
        String mainSong = program.getAsJsonObject("mainSong").toString();
        if (mainSong == null) {
            NetMusic.LOGGER.error("Failed to get DJ song info, mainSong is null for id: {}", id);
            return new ItemMusicCD.SongInfo();
        }
        NetEaseMusicSong.Song netEaseMusicSong = new Gson().fromJson(mainSong, NetEaseMusicSong.Song.class);
        return new ItemMusicCD.SongInfo(netEaseMusicSong);
    }

    public static void add163List(long id) throws Exception {
        if (!Files.isDirectory(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }

        NetEaseMusicList pojo = GSON.fromJson(NetMusic.NET_EASE_WEB_API.list(id), NetEaseMusicList.class);

        int count = pojo.getPlayList().getTracks().size();
        int size = Math.min(pojo.getPlayList().getTrackIds().size(), MAX_NUM);
        // 获取额外歌曲
        if (count < size) {
            long[] ids = new long[size - count];
            for (int i = count; i < size; i++) {
                ids[i - count] = pojo.getPlayList().getTrackIds().get(i).getId();
            }
            String extraTrackInfo = NetMusic.NET_EASE_WEB_API.songs(ids);
            ExtraMusicList extra = GSON.fromJson(extraTrackInfo, ExtraMusicList.class);
            pojo.getPlayList().getTracks().addAll(extra.getTracks());
        }

        SONGS.clear();
        for (NetEaseMusicList.Track track : pojo.getPlayList().getTracks()) {
            SONGS.add(new ItemMusicCD.SongInfo(track));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        FileUtils.write(CONFIG_FILE.toFile(), gson.toJson(SONGS), StandardCharsets.UTF_8);
    }
}
