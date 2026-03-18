package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayer;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import org.apache.commons.lang3.StringUtils;

public class MusicPlayerStateMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "music_player_state");

    private final int x;
    private final int y;
    private final int z;
    private final boolean play;
    private final int currentTime;
    private final boolean signal;
    private final int playSessionId;
    private final ItemStack stack;
    private final String songUrl;
    private final int songTime;
    private final String songName;

    public MusicPlayerStateMessage(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readItemStack(),
                readOptionalString(buf), readOptionalInt(buf), readOptionalString(buf), readOptionalInt(buf));
    }

    public MusicPlayerStateMessage(int x, int y, int z, boolean play, int currentTime, boolean signal, ItemStack stack) {
        this(x, y, z, play, currentTime, signal, 0, stack, "", 0, "");
    }

    public MusicPlayerStateMessage(int x, int y, int z, boolean play, int currentTime, boolean signal, int playSessionId,
                                   ItemStack stack, String songUrl, int songTime, String songName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.play = play;
        this.currentTime = currentTime;
        this.signal = signal;
        this.playSessionId = Math.max(0, playSessionId);
        this.stack = stack == null ? null : stack.copy();
        this.songUrl = songUrl == null ? "" : songUrl;
        this.songTime = Math.max(0, songTime);
        this.songName = songName == null ? "" : songName;
    }

    private MusicPlayerStateMessage(int x, int y, int z, boolean play, int currentTime, boolean signal, ItemStack stack,
                                   String songUrl, int songTime, String songName, int playSessionId) {
        this(x, y, z, play, currentTime, signal, playSessionId, stack, songUrl, songTime, songName);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeBoolean(this.play);
        buf.writeInt(this.currentTime);
        buf.writeBoolean(this.signal);
        buf.writeItemStack(this.stack);
        buf.writeString(this.songUrl);
        buf.writeInt(this.songTime);
        buf.writeString(this.songName);
        buf.writeInt(this.playSessionId);
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        if (entityPlayer == null || entityPlayer.worldObj == null || !entityPlayer.worldObj.isRemote) {
            return;
        }
        TileEntity tileEntity = entityPlayer.worldObj.getBlockTileEntity(this.x, this.y, this.z);
        if (tileEntity instanceof TileEntityMusicPlayer musicPlayer) {
            musicPlayer.applyClientSync(this.play, this.currentTime, this.signal, this.playSessionId, this.stack == null ? null : this.stack.copy());
        }

        if (!this.play || this.songTime <= 0 || StringUtils.isBlank(this.songUrl)) {
            if (ClientMusicPlayer.isPlayingAt(this.x, this.y, this.z)) {
                ClientMusicPlayer.stop();
            }
            return;
        }
        // Playback start/recovery is server-driven through MusicToClientMessage.
        // State packets keep tile state synced only, to avoid client-side replay loops.
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }

    private static String readOptionalString(PacketByteBuf buf) {
        try {
            return buf.readString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int readOptionalInt(PacketByteBuf buf) {
        try {
            return buf.readInt();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
