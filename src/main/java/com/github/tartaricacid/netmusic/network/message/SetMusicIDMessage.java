package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

public class SetMusicIDMessage implements Message {
    public final static ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "set_music_id");
    private final ItemMusicCD.SongInfo song;

    public SetMusicIDMessage(PacketByteBuf packetByteBuf) {
        this(readSongInfo(packetByteBuf));
    }

    public SetMusicIDMessage(ItemMusicCD.SongInfo song) {
        this.song = song;
    }

    public ResourceLocation getChannel() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        writeSongInfo(packetByteBuf, this.song);
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        if (entityPlayer == null || this.song == null) {
            return;
        }
        if (entityPlayer.openContainer instanceof CDBurnerMenu menu) {
            menu.setSongInfo(this.song);
            return;
        }
        if (entityPlayer.openContainer instanceof ComputerMenu menu) {
            menu.setSongInfo(this.song);
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
        int size = buf.readInt();
        info.artists.clear();
        for (int i = 0; i < size; i++) {
            info.artists.add(buf.readString());
        }
        return info;
    }
}