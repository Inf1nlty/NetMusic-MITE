package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.lyric.LyricParser;
import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayer;
import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicToClientMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "play_music");
    private static final String MUSIC_163_URL = "https://music.163.com/";
    private static final Pattern MUSIC_163_ID_PATTERN = Pattern.compile("^.*?[?&]id=(\\d+)\\.mp3$");

    public final int x;
    public final int y;
    public final int z;
    public final String url;
    public final int timeSecond;
    public final String songName;

    public MusicToClientMessage(PacketByteBuf packetByteBuf) {
        this(packetByteBuf.readInt(), packetByteBuf.readInt(), packetByteBuf.readInt(), packetByteBuf.readString(), packetByteBuf.readInt(), packetByteBuf.readString());
    }

    public MusicToClientMessage(int x, int y, int z, String url, int timeSecond, String songName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(this.x);
        packetByteBuf.writeInt(this.y);
        packetByteBuf.writeInt(this.z);
        packetByteBuf.writeString(this.url == null ? "" : this.url);
        packetByteBuf.writeInt(this.timeSecond);
        packetByteBuf.writeString(this.songName == null ? "" : this.songName);
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
                playerTile.setCurrentTime(this.timeSecond * 20 + 64);
            }
        }

        if (StringUtils.isBlank(this.url)) {
            ClientMusicPlayer.stop();
            return;
        }

        LyricRecord lyricRecord = null;
        if (GeneralConfig.ENABLE_PLAYER_LYRICS && this.url.startsWith(MUSIC_163_URL)) {
            Matcher matcher = MUSIC_163_ID_PATTERN.matcher(this.url);
            if (matcher.find()) {
                try {
                    long musicId = Long.parseLong(matcher.group(1));
                    String lyricJson = NetMusic.NET_EASE_WEB_API.lyric(musicId);
                    lyricRecord = LyricParser.parseLyric(lyricJson, this.songName);
                } catch (NumberFormatException | IOException e) {
                    NetMusic.LOGGER.warn("Failed to load lyric for {}", this.url, e);
                }
            }
        }

        if (playerTile != null) {
            playerTile.lyricRecord = lyricRecord;
        }

        LyricRecord finalLyricRecord = lyricRecord;
        MusicPlayManager.play(this.url, this.songName, resolved -> {
            ClientMusicPlayer.play(new NetMusicSound(this.x, this.y, this.z, resolved, this.timeSecond, finalLyricRecord));
            return null;
        });
    }
}