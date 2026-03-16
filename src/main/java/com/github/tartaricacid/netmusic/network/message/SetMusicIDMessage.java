package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

public class SetMusicIDMessage implements Message {
    public final static ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "set_music_id");
    private static final int MAX_ARTIST_COUNT = 32;
    private static final int MAX_SONG_TIME_SECONDS = 60 * 60 * 12;
    private final Source source;
    private final ItemMusicCD.SongInfo song;

    public enum Source {
        CD_BURNER,
        COMPUTER;

        public static Source fromOrdinal(int ordinal) {
            Source[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return CD_BURNER;
            }
            return values[ordinal];
        }
    }

    public SetMusicIDMessage(PacketByteBuf packetByteBuf) {
        this(Source.fromOrdinal(packetByteBuf.readInt()), readSongInfo(packetByteBuf));
    }

    public SetMusicIDMessage(Source source, ItemMusicCD.SongInfo song) {
        this.source = source == null ? Source.CD_BURNER : source;
        this.song = sanitizeSong(song);
    }

    public ResourceLocation getChannel() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(this.source.ordinal());
        writeSongInfo(packetByteBuf, this.song == null ? new ItemMusicCD.SongInfo() : this.song);
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        if (entityPlayer == null || this.song == null) {
            return;
        }
        if (this.source == Source.CD_BURNER) {
            if (entityPlayer.openContainer instanceof CDBurnerMenu menu) {
                String failure = menu.tryWriteSong(this.song);
                if (failure != null) {
                    entityPlayer.addChatMessage(failure);
                }
            }
            return;
        }
        if (this.source == Source.COMPUTER) {
            if (entityPlayer.openContainer instanceof ComputerMenu menu) {
                String failure = menu.tryWriteSong(this.song);
                if (failure != null) {
                    entityPlayer.addChatMessage(failure);
                }
            }
        }
    }

    private static void writeSongInfo(PacketByteBuf buf, ItemMusicCD.SongInfo song) {
        buf.writeString(song.songUrl == null ? "" : song.songUrl);
        buf.writeString(song.songName == null ? "" : song.songName);
        buf.writeInt(song.songTime);
        buf.writeString(song.transName == null ? "" : song.transName);
        buf.writeBoolean(song.vip);
        buf.writeBoolean(song.readOnly);
        int size = song.artists == null ? 0 : song.artists.size();
        size = Math.min(size, MAX_ARTIST_COUNT);
        buf.writeInt(size);
        for (int i = 0; i < size; i++) {
            String artist = song.artists.get(i);
            buf.writeString(artist == null ? "" : artist);
        }
    }

    private static ItemMusicCD.SongInfo readSongInfo(PacketByteBuf buf) {
        ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo();
        info.songUrl = buf.readString();
        info.songName = buf.readString();
        info.songTime = buf.readInt();
        info.transName = buf.readString();
        info.vip = buf.readBoolean();
        info.readOnly = buf.readBoolean();
        int size = Math.max(0, Math.min(MAX_ARTIST_COUNT, buf.readInt()));
        info.artists.clear();
        for (int i = 0; i < size; i++) {
            info.artists.add(buf.readString());
        }
        return sanitizeSong(info);
    }

    private static ItemMusicCD.SongInfo sanitizeSong(ItemMusicCD.SongInfo input) {
        if (input == null) {
            return null;
        }
        String url = StringUtils.trimToEmpty(input.songUrl);
        String name = StringUtils.trimToEmpty(input.songName);
        if (StringUtils.isBlank(url) || StringUtils.isBlank(name)) {
            return null;
        }

        ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo();
        info.songUrl = url;
        info.songName = StringUtils.substring(name, 0, 256);
        info.songTime = Math.max(1, Math.min(MAX_SONG_TIME_SECONDS, input.songTime));
        info.transName = StringUtils.substring(StringUtils.trimToEmpty(input.transName), 0, 256);
        info.vip = input.vip;
        info.readOnly = input.readOnly;
        info.artists.clear();
        if (input.artists != null) {
            for (String artist : input.artists) {
                if (info.artists.size() >= MAX_ARTIST_COUNT) {
                    break;
                }
                String value = StringUtils.trimToEmpty(artist);
                if (!StringUtils.isBlank(value)) {
                    info.artists.add(StringUtils.substring(value, 0, 128));
                }
            }
        }
        return info;
    }
}