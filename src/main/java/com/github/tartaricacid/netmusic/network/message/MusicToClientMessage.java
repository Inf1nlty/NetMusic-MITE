package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayer;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import org.apache.commons.lang3.StringUtils;

public class MusicToClientMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "play_music");

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
        if (tile instanceof TileEntityMusicPlayer playerTile) {
            if (StringUtils.isBlank(this.url)) {
                playerTile.setPlay(false);
                playerTile.setCurrentTime(0);
            } else {
                playerTile.setPlay(true);
                playerTile.setCurrentTime(this.timeSecond * 20 + 64);
            }
        }

        if (StringUtils.isBlank(this.url)) {
            ClientMusicPlayer.stop();
            return;
        }

        MusicPlayManager.play(this.url, this.songName, resolved -> {
            ClientMusicPlayer.play(new NetMusicSound(this.x, this.y, this.z, resolved, this.timeSecond, null));
            return null;
        });
    }
}