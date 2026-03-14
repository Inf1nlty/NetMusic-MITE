package com.github.tartaricacid.netmusic.tileentity;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.TileEntity;

import javax.annotation.Nullable;

public class TileEntityMusicPlayer extends TileEntity {
    private static final String IS_PLAY_TAG = "IsPlay";
    private static final String CURRENT_TIME_TAG = "CurrentTime";
    private static final String SIGNAL_TAG = "RedStoneSignal";
    private static final String ITEM_TAG = "MusicCd";

    private ItemStack[] items = new ItemStack[1];
    private boolean isPlay = false;
    private int currentTime;
    private boolean hasSignal = false;

    /**
     * 仅客户端使用，记录当前音乐的歌词信息，用于渲染歌词
     */
    public @Nullable LyricRecord lyricRecord = null;

    public TileEntityMusicPlayer() {
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.isPlay = nbt.getBoolean(IS_PLAY_TAG);
        this.currentTime = nbt.getInteger(CURRENT_TIME_TAG);
        this.hasSignal = nbt.getBoolean(SIGNAL_TAG);
        if (nbt.hasKey(ITEM_TAG)) {
            this.items[0] = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(ITEM_TAG));
        } else {
            this.items[0] = null;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean(IS_PLAY_TAG, this.isPlay);
        nbt.setInteger(CURRENT_TIME_TAG, this.currentTime);
        nbt.setBoolean(SIGNAL_TAG, this.hasSignal);
        if (this.items[0] != null) {
            nbt.setCompoundTag(ITEM_TAG, this.items[0].writeToNBT(new NBTTagCompound()));
        }
    }

    public ItemStack getItem(int slot) {
        return slot == 0 ? this.items[0] : null;
    }

    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        this.items[0] = stack;
    }

    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0) {
            return null;
        }
        ItemStack stack = this.items[0];
        if (stack == null) {
            return null;
        }
        if (stack.stackSize <= amount) {
            this.items[0] = null;
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            this.items[0] = null;
        }
        return split;
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        isPlay = play;
    }

    public void setPlayToClient(ItemMusicCD.SongInfo info) {
        this.setCurrentTime(info.songTime * 20 + 64);
        this.isPlay = true;
        // Legacy migration note: network broadcast is reconnected when packet layer is ported to FML3.4.2 APIs.
    }

    public void setChanged() {
        ItemStack stack = getItem(0);
        if (stack == null) {
            setPlay(false);
            setCurrentTime(0);
        }
        this.onInventoryChanged();
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }

        this.tickTime();
        if (0 < this.currentTime && this.currentTime < 16 && this.currentTime % 5 == 0) {
            int metadata = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
            if (BlockMusicPlayer.isCycleDisabled(metadata)) {
                this.setPlay(false);
                this.setChanged();
            } else {
                ItemStack stackInSlot = this.getItem(0);
                if (stackInSlot == null) {
                    return;
                }
                ItemMusicCD.SongInfo songInfo = ItemMusicCD.getSongInfo(stackInSlot);
                if (songInfo != null) {
                    this.setPlayToClient(songInfo);
                }
            }
        }
    }

    public void setCurrentTime(int time) {
        this.currentTime = time;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public boolean hasSignal() {
        return hasSignal;
    }

    public void setSignal(boolean signal) {
        this.hasSignal = signal;
    }

    public void tickTime() {
        if (currentTime > 0) {
            currentTime--;
        }
    }
}
