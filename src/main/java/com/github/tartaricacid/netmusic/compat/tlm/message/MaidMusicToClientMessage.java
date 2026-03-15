package com.github.tartaricacid.netmusic.compat.tlm.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.network.message.Message;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

public class MaidMusicToClientMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "maid_music_to_client");

    public final int entityId;
    public final String url;
    public final int timeSecond;
    public final String songName;

    public MaidMusicToClientMessage(PacketByteBuf packetByteBuf) {
        this(packetByteBuf.readInt(), packetByteBuf.readString(), packetByteBuf.readInt(), packetByteBuf.readString());
    }

    public MaidMusicToClientMessage(int entityId, String url, int timeSecond, String songName) {
        this.entityId = entityId;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
    }

    public static void showLyric(Object maid, String url, String songName, int timeSecond) {
        // MITE low version: maid lyric feature is removed.
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(this.entityId);
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
    }
}