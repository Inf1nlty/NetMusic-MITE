package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.config.MusicListManage;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.StatCollector;

public class GetMusicListMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "get_music_list");
    public final long musicListId;
    public static final long RELOAD_MESSAGE = -1;

    public GetMusicListMessage(PacketByteBuf packetByteBuf) {
        this(packetByteBuf.readLong());
    }

    public GetMusicListMessage(long musicListId) {
        this.musicListId = musicListId;
    }

    public ResourceLocation getChannel() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeLong(this.musicListId);
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        try {
            if (this.musicListId == RELOAD_MESSAGE) {
                MusicListManage.loadConfigSongs();
                if (entityPlayer != null) {
                    entityPlayer.addChatMessage(StatCollector.translateToLocal("command.netmusic.music_cd.reload.success"));
                }
            } else {
                MusicListManage.add163List(this.musicListId);
                if (entityPlayer != null) {
                    entityPlayer.addChatMessage(StatCollector.translateToLocal("command.netmusic.music_cd.add163.success"));
                }
            }
        } catch (Exception e) {
            if (entityPlayer != null) {
                entityPlayer.addChatMessage(StatCollector.translateToLocal("command.netmusic.music_cd.add163.fail"));
            }
            NetMusic.LOGGER.error("Failed to get music list from NetEase Cloud Music", e);
        }
    }
}
