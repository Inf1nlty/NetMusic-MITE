package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;

public class MusicPlayerStateMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "music_player_state");

    private final int x;
    private final int y;
    private final int z;
    private final boolean play;
    private final int currentTime;
    private final boolean signal;
    private final ItemStack stack;

    public MusicPlayerStateMessage(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readItemStack());
    }

    public MusicPlayerStateMessage(int x, int y, int z, boolean play, int currentTime, boolean signal, ItemStack stack) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.play = play;
        this.currentTime = currentTime;
        this.signal = signal;
        this.stack = stack == null ? null : stack.copy();
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
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        if (entityPlayer == null || entityPlayer.worldObj == null || !entityPlayer.worldObj.isRemote) {
            return;
        }
        TileEntity tileEntity = entityPlayer.worldObj.getBlockTileEntity(this.x, this.y, this.z);
        if (tileEntity instanceof TileEntityMusicPlayer musicPlayer) {
            musicPlayer.applyClientSync(this.play, this.currentTime, this.signal, this.stack == null ? null : this.stack.copy());
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }
}
