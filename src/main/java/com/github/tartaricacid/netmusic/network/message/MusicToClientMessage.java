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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicToClientMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "play_music");
    private static final String MUSIC_163_URL = "https://music.163.com/";
    private static final Pattern MUSIC_163_ID_PATTERN = Pattern.compile("^.*?[?&]id=(\\d+)\\.mp3$");
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
        if (entityPlayer == null || entityPlayer.worldObj == null || !entityPlayer.worldObj.isRemote) {
            return;
        }

        TileEntity tile = entityPlayer.worldObj.getBlockTileEntity(this.x, this.y, this.z);
        TileEntityMusicPlayer playerTile = tile instanceof TileEntityMusicPlayer ? (TileEntityMusicPlayer) tile : null;
        if (playerTile != null) {
            if (StringUtils.isBlank(this.url)) {
                playerTile.setPlay(false);
                playerTile.setCurrentTime(0);
                playerTile.lyricRecord = null;
            } else {
                playerTile.setPlay(true);
                int totalTicks = Math.max(1, this.timeSecond) * 20 + 64;
                int syncedCurrent = Math.max(0, totalTicks - this.startTick);
                playerTile.setCurrentTime(syncedCurrent);
            }
        }

        if (StringUtils.isBlank(this.url)) {
            ClientMusicPlayer.stop();
            return;
        }

        LyricRecord lyricRecord = resolveLyricRecord(this.url, this.songName, this.startTick > 0);
        if (lyricRecord != null) {
            lyricRecord.updateCurrentLine(Math.max(0, this.startTick));
        }

        if (playerTile != null) {
            playerTile.lyricRecord = lyricRecord;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null && StringUtils.isNotBlank(this.songName)) {
            minecraft.ingameGUI.setRecordPlayingMessage(this.songName);
        }

        LyricRecord finalLyricRecord = lyricRecord;
        MusicPlayManager.play(this.url, this.songName, resolved -> {
            ClientMusicPlayer.play(new NetMusicSound(this.x, this.y, this.z, resolved, this.timeSecond, finalLyricRecord, this.startTick));
            return null;
        });
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

        if (recovery) {
            synchronized (LYRIC_CACHE) {
                Long until = LYRIC_MISS_UNTIL.get(key);
                if (until != null && System.currentTimeMillis() < until.longValue()) {
                    return null;
                }
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
