package com.github.tartaricacid.netmusic.compat.tlm.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.network.message.Message;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

public class MaidStopMusicMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "maid_stop_music");
    private final int entityId;

    public MaidStopMusicMessage(PacketByteBuf packetByteBuf) {
        this(packetByteBuf.readInt());
    }

    public MaidStopMusicMessage(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(this.entityId);
    }

    public int getEntityId() {
        return entityId;
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
    }
}