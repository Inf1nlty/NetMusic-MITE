package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.lyric.LyricParser;
import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.api.qq.QqMusicApi;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayer;
import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import net.minecraft.Minecraft;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicToClientMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "play_music");
    private static final String MUSIC_163_URL = "https://music.163.com/";
    private static final Pattern MUSIC_163_ID_PATTERN = Pattern.compile("^.*?[?&]id=(\\d+)\\.mp3$");
    private static final Pattern NETEASE_ID_PATTERN = Pattern.compile("[?&]id=(\\d+)");
    private static final Pattern QQ_MID_PATTERN = Pattern.compile("(?i)(?:songmid|netmusic_songmid)(?:=|/)([0-9a-z]{14})");
    private static final Pattern QQ_SONG_DETAIL_PATTERN = Pattern.compile("(?i)/songDetail/([0-9a-z]{14})");
    private static final Pattern QQ_FILENAME_MID_PATTERN = Pattern.compile("(?i)(?:^|/)[a-z]{1,2}\\d{3}([0-9a-z]{14})\\.(?:mp3|m4a|flac|wav|ogg)");
    private static final int LYRIC_CACHE_MAX = 64;
    private static final long RECOVERY_LYRIC_COOLDOWN_MS = 8000L;
    private static final Map<String, LyricRecord> LYRIC_CACHE = new LinkedHashMap<String, LyricRecord>(LYRIC_CACHE_MAX + 1, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, LyricRecord> eldest) {
            return this.size() > LYRIC_CACHE_MAX;
        }
    };
    private static final Map<String, Long> LYRIC_MISS_UNTIL = new LinkedHashMap<String, Long>();

    public final int x;
    public final int y;
    public final int z;
    public final String url;
    public final int timeSecond;
    public final String songName;
    public final int startTick;

    public MusicToClientMessage(PacketByteBuf packetByteBuf) {
        this(
                packetByteBuf.readInt(),
                packetByteBuf.readInt(),
                packetByteBuf.readInt(),
                packetByteBuf.readString(),
                packetByteBuf.readInt(),
                packetByteBuf.readString(),
                readOptionalStartTick(packetByteBuf)
        );
    }

    public MusicToClientMessage(int x, int y, int z, String url, int timeSecond, String songName) {
        this(x, y, z, url, timeSecond, songName, 0);
    }

    public MusicToClientMessage(int x, int y, int z, String url, int timeSecond, String songName, int startTick) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
        this.startTick = Math.max(0, startTick);
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(this.x);
        packetByteBuf.writeInt(this.y);
        packetByteBuf.writeInt(this.z);
        packetByteBuf.writeString(this.url == null ? "" : this.url);
        packetByteBuf.writeInt(this.timeSecond);
        packetByteBuf.writeString(this.songName == null ? "" : this.songName);
        packetByteBuf.writeInt(this.startTick);
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        applyClientPlayback(entityPlayer, this.x, this.y, this.z, this.url, this.timeSecond, this.songName, this.startTick, true);
    }

    public static void applyClientPlayback(EntityPlayer entityPlayer, int x, int y, int z,
                                           String url, int timeSecond, String songName,
                                           int startTick, boolean updateTile) {
        if (entityPlayer == null || entityPlayer.worldObj == null || !entityPlayer.worldObj.isRemote) {
            return;
        }

        TileEntity tile = entityPlayer.worldObj.getBlockTileEntity(x, y, z);
        TileEntityMusicPlayer playerTile = tile instanceof TileEntityMusicPlayer ? (TileEntityMusicPlayer) tile : null;
        int safeStartTick = Math.max(0, startTick);
        int safeTimeSecond = Math.max(0, timeSecond);
        String safeUrl = url == null ? "" : url;
        String safeSongName = songName == null ? "" : songName;

        if (updateTile && playerTile != null) {
            if (StringUtils.isBlank(safeUrl)) {
                playerTile.setPlay(false);
                playerTile.setCurrentTime(0);
                playerTile.lyricRecord = null;
            } else {
                playerTile.setPlay(true);
                int totalTicks = Math.max(1, safeTimeSecond) * 20 + 64;
                int syncedCurrent = Math.max(0, totalTicks - safeStartTick);
                playerTile.setCurrentTime(syncedCurrent);
            }
        }

        if (StringUtils.isBlank(safeUrl)) {
            if (ClientMusicPlayer.isPlayingAt(x, y, z)) {
                ClientMusicPlayer.stop();
            }
            return;
        }

        String sourceId = buildPlaybackSourceId(safeUrl, safeTimeSecond, safeSongName);
        if (ClientMusicPlayer.isPendingAtSource(x, y, z, sourceId)) {
            return;
        }
        if (ClientMusicPlayer.isPlayingAtSource(x, y, z, sourceId)) {
            return;
        }
        if (ClientMusicPlayer.isPlayingAt(x, y, z)) {
            ClientMusicPlayer.stop();
            if (GeneralConfig.ENABLE_DEBUG_MODE) {
                NetMusic.LOGGER.info("[NetMusic Debug][Play] restart playback at ({},{},{}) startTick={} source={}",
                        x, y, z, safeStartTick, sourceId);
            }
        }

        LyricRecord lyricRecord = resolveLyricRecord(safeUrl, safeSongName, safeStartTick > 0);
        if (lyricRecord != null) {
            lyricRecord.updateCurrentLine(safeStartTick);
        }

        if (playerTile != null) {
            playerTile.lyricRecord = lyricRecord;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null && StringUtils.isNotBlank(safeSongName)) {
            minecraft.ingameGUI.setRecordPlayingMessage(safeSongName);
        }

        LyricRecord finalLyricRecord = lyricRecord;
        if (isDirectAudioUrl(safeUrl)) {
            try {
                ClientMusicPlayer.play(new NetMusicSound(x, y, z, new URL(safeUrl), safeTimeSecond, finalLyricRecord, safeStartTick), sourceId);
                return;
            } catch (Exception e) {
                NetMusic.LOGGER.warn("Failed to play direct url from server sync: {}", safeUrl, e);
            }
        }
        ClientMusicPlayer.markPendingPlayback(x, y, z, sourceId, 15000L);
        MusicPlayManager.play(safeUrl, safeSongName, resolved -> {
            if (finalLyricRecord != null) {
                finalLyricRecord.updateCurrentLine(safeStartTick);
            }
            ClientMusicPlayer.play(new NetMusicSound(x, y, z, resolved, safeTimeSecond, finalLyricRecord, safeStartTick), sourceId);
            return null;
        });
    }

    public static String buildPlaybackSourceId(String url, int timeSecond, String songName) {
        return normalizeSourceUrl(url);
    }

    private static String normalizeSourceUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        String value = url.trim();

        String qqMid = extractQqSongMid(value);
        if (StringUtils.isNotBlank(qqMid)) {
            return "qq:" + qqMid;
        }

        String neteaseId = extractNeteaseId(value);
        if (StringUtils.isNotBlank(neteaseId)) {
            return "netease:" + neteaseId;
        }

        int end = value.length();
        int query = value.indexOf('?');
        if (query >= 0 && query < end) {
            end = query;
        }
        int fragment = value.indexOf('#');
        if (fragment >= 0 && fragment < end) {
            end = fragment;
        }
        return value.substring(0, end).toLowerCase(Locale.ROOT);
    }

    private static String extractQqSongMid(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        Matcher fragmentMid = QQ_MID_PATTERN.matcher(url);
        if (fragmentMid.find()) {
            return fragmentMid.group(1);
        }
        Matcher detailMid = QQ_SONG_DETAIL_PATTERN.matcher(url);
        if (detailMid.find()) {
            return detailMid.group(1);
        }
        Matcher fileMid = QQ_FILENAME_MID_PATTERN.matcher(url);
        if (fileMid.find()) {
            return fileMid.group(1);
        }
        return "";
    }

    private static String extractNeteaseId(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        Matcher matcher = NETEASE_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static boolean isDirectAudioUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        int cut = lower.length();
        int query = lower.indexOf('?');
        if (query >= 0 && query < cut) {
            cut = query;
        }
        int fragment = lower.indexOf('#');
        if (fragment >= 0 && fragment < cut) {
            cut = fragment;
        }
        String base = lower.substring(0, cut);
        return base.endsWith(".mp3")
                || base.endsWith(".flac")
                || base.endsWith(".m4a")
                || base.endsWith(".wav")
                || base.endsWith(".ogg");
    }

    private static int readOptionalStartTick(PacketByteBuf buf) {
        try {
            if (buf.getInputStream() != null && buf.getInputStream().available() >= 4) {
                return Math.max(0, buf.readInt());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static LyricRecord resolveLyricRecord(String url, String songName, boolean recovery) {
        if (!GeneralConfig.ENABLE_PLAYER_LYRICS || StringUtils.isBlank(url)) {
            return null;
        }
        String key = lyricKey(url, songName);
        synchronized (LYRIC_CACHE) {
            LyricRecord cached = LYRIC_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            Long until = LYRIC_MISS_UNTIL.get(key);
            if (until != null && System.currentTimeMillis() < until.longValue()) {
                return null;
            }
        }

        LyricRecord resolved = doResolveLyric(url, songName);
        synchronized (LYRIC_CACHE) {
            if (resolved != null) {
                LYRIC_CACHE.put(key, resolved);
                LYRIC_MISS_UNTIL.remove(key);
            } else {
                long cooldown = recovery ? RECOVERY_LYRIC_COOLDOWN_MS : 3000L;
                LYRIC_MISS_UNTIL.put(key, System.currentTimeMillis() + cooldown);
            }
        }
        return resolved;
    }

    private static LyricRecord doResolveLyric(String url, String songName) {
        if (url.startsWith(MUSIC_163_URL)) {
            Matcher matcher = MUSIC_163_ID_PATTERN.matcher(url);
            if (matcher.find()) {
                try {
                    long musicId = Long.parseLong(matcher.group(1));
                    String lyricJson = NetMusic.NET_EASE_WEB_API.lyric(musicId);
                    return LyricParser.parseLyric(lyricJson, songName);
                } catch (NumberFormatException | IOException e) {
                    NetMusic.LOGGER.warn("Failed to load lyric for {}", url, e);
                    return null;
                }
            }
            return null;
        }
        if (QqMusicApi.isValidMid(QqMusicApi.extractMid(url))) {
            return QqMusicApi.resolveLyric(url, songName);
        }
        return null;
    }

    private static String lyricKey(String url, String songName) {
        String safeUrl = url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
        String safeName = songName == null ? "" : songName.trim().toLowerCase(Locale.ROOT);
        return safeUrl + "|" + safeName;
    }
}
