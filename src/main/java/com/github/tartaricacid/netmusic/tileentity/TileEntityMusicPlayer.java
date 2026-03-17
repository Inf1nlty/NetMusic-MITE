package com.github.tartaricacid.netmusic.tileentity;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;
import com.github.tartaricacid.netmusic.network.message.MusicPlayerStateMessage;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.Packet;
import net.minecraft.Packet132TileEntityData;
import net.minecraft.TileEntity;

import javax.annotation.Nullable;

public class TileEntityMusicPlayer extends TileEntity {
    private static final String IS_PLAY_TAG = "IsPlay";
    private static final String CURRENT_TIME_TAG = "CurrentTime";
    private static final String SIGNAL_TAG = "RedStoneSignal";
    private static final String ITEM_TAG = "MusicCd";
    private static final String ITEM_INFO_TAG = "MusicCdInfo";
    private static final String ITEM_COUNT_TAG = "MusicCdCount";
    private static final String ITEM_SUBTYPE_TAG = "MusicCdSubtype";

    private ItemStack[] items = new ItemStack[1];
    private boolean isPlay = false;
    private int currentTime;
    private boolean hasSignal = false;
    private int syncTickCounter = 0;

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
        NBTTagCompound infoTag = nbt.hasKey(ITEM_INFO_TAG) ? nbt.getCompoundTag(ITEM_INFO_TAG) : null;
        int savedCount = Math.max(1, nbt.getInteger(ITEM_COUNT_TAG));
        int savedSubtype = nbt.getInteger(ITEM_SUBTYPE_TAG);
        if (nbt.hasKey(ITEM_TAG)) {
            NBTTagCompound itemTag = nbt.getCompoundTag(ITEM_TAG);
            ItemStack loaded = ItemStack.loadItemStackFromNBT(itemTag);
            this.items[0] = resolveLoadedMusicCd(loaded, itemTag, infoTag, savedCount, savedSubtype);
        } else if (infoTag != null) {
            this.items[0] = rebuildMusicCdFromInfoTag(infoTag, savedCount, savedSubtype);
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
            ItemStack stack = this.items[0];
            nbt.setCompoundTag(ITEM_TAG, stack.writeToNBT(new NBTTagCompound()));

            ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(stack);
            if (info != null) {
                NBTTagCompound infoTag = new NBTTagCompound();
                ItemMusicCD.SongInfo.serializeNBT(info, infoTag);
                nbt.setCompoundTag(ITEM_INFO_TAG, infoTag);
                nbt.setInteger(ITEM_COUNT_TAG, Math.max(1, stack.stackSize));
                nbt.setInteger(ITEM_SUBTYPE_TAG, stack.getItemSubtype());
            }
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
        this.setChanged();
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
            this.setChanged();
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            this.items[0] = null;
        }
        this.setChanged();
        return split;
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        if (this.isPlay && !play && this.worldObj != null && !this.worldObj.isRemote) {
            NetworkHandler.sendToNearBy(this.worldObj, this.xCoord, this.yCoord, this.zCoord,
                    new MusicToClientMessage(this.xCoord, this.yCoord, this.zCoord, "", 0, ""));
        }
        isPlay = play;
    }

    public void setPlayToClient(ItemMusicCD.SongInfo info) {
        this.setCurrentTime(info.songTime * 20 + 64);
        this.isPlay = true;
        if (this.worldObj != null && !this.worldObj.isRemote && info != null) {
            NetworkHandler.sendToNearBy(this.worldObj, this.xCoord, this.yCoord, this.zCoord,
                    new MusicToClientMessage(this.xCoord, this.yCoord, this.zCoord,
                            info.songUrl, info.songTime, info.songName));
            this.syncStateToClients();
        }
    }

    public void setChanged() {
        ItemStack stack = getItem(0);
        if (stack == null) {
            setPlay(false);
            setCurrentTime(0);
        }
        this.onInventoryChanged();
        if (this.worldObj != null) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            this.worldObj.markBlockForRenderUpdate(this.xCoord, this.yCoord, this.zCoord);
            if (!this.worldObj.isRemote) {
                this.syncStateToClients();
            }
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }

        this.tickTime();
        this.syncTickCounter++;
        if (this.syncTickCounter >= 10) {
            this.syncTickCounter = 0;
            this.syncStateToClients();
        }
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

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        return new Packet132TileEntityData(this.xCoord, this.yCoord, this.zCoord, 1, nbt);
    }

    public void applyClientSync(boolean play, int currentTime, boolean signal, @Nullable ItemStack stack) {
        this.isPlay = play;
        this.currentTime = currentTime;
        this.hasSignal = signal;
        this.items[0] = stack;
        if (stack == null) {
            this.lyricRecord = null;
        }
        if (this.worldObj != null) {
            this.worldObj.markBlockForRenderUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    private void syncStateToClients() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        ItemStack stack = this.items[0] == null ? null : this.items[0].copy();
        ItemMusicCD.SongInfo info = stack == null ? null : ItemMusicCD.getSongInfo(stack);
        String songUrl = info == null || info.songUrl == null ? "" : info.songUrl;
        int songTime = info == null ? 0 : Math.max(0, info.songTime);
        String songName = info == null || info.songName == null ? "" : info.songName;
        NetworkHandler.sendToNearBy(this.worldObj, this.xCoord, this.yCoord, this.zCoord,
                new MusicPlayerStateMessage(this.xCoord, this.yCoord, this.zCoord,
                        this.isPlay, this.currentTime, this.hasSignal, stack, songUrl, songTime, songName));
    }

    private static ItemStack resolveLoadedMusicCd(@Nullable ItemStack loaded, NBTTagCompound itemTag,
                                                  @Nullable NBTTagCompound infoTag, int count, int subtype) {
        if (isValidMusicCdStack(loaded)) {
            return loaded;
        }
        ItemStack rebuilt = rebuildMusicCdFallback(itemTag);
        if (isValidMusicCdStack(rebuilt)) {
            return rebuilt;
        }
        return rebuildMusicCdFromInfoTag(infoTag, count, subtype);
    }

    private static boolean isValidMusicCdStack(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() != InitItems.MUSIC_CD) {
            return false;
        }
        return ItemMusicCD.getSongInfo(stack) != null;
    }

    private static ItemStack rebuildMusicCdFallback(NBTTagCompound itemTag) {
        if (itemTag == null || !itemTag.hasKey("tag")) {
            return null;
        }
        NBTTagCompound rootTag = itemTag.getCompoundTag("tag");
        if (!rootTag.hasKey(ItemMusicCD.SONG_INFO_TAG)) {
            return null;
        }

        ItemStack rebuilt = new ItemStack(InitItems.MUSIC_CD, 1);
        if (itemTag.hasKey("Count")) {
            int count = Math.max(1, itemTag.getByte("Count"));
            rebuilt.stackSize = count;
        }
        if (itemTag.hasKey("Damage")) {
            rebuilt.setItemDamage(itemTag.getShort("Damage"));
        }

        NBTTagCompound rebuiltTag = new NBTTagCompound();
        rebuiltTag.setCompoundTag(
                ItemMusicCD.SONG_INFO_TAG,
                (NBTTagCompound) rootTag.getCompoundTag(ItemMusicCD.SONG_INFO_TAG).copy()
        );
        rebuilt.setTagCompound(rebuiltTag);
        return rebuilt;
    }

    private static ItemStack rebuildMusicCdFromInfoTag(@Nullable NBTTagCompound infoTag, int count, int subtype) {
        if (infoTag == null || InitItems.MUSIC_CD == null) {
            return null;
        }
        ItemMusicCD.SongInfo info = ItemMusicCD.SongInfo.deserializeNBT(infoTag);
        if (info == null || info.songTime <= 0) {
            return null;
        }
        ItemStack rebuilt = new ItemStack(InitItems.MUSIC_CD, Math.max(1, count), subtype);
        ItemMusicCD.setSongInfo(info, rebuilt);
        return rebuilt;
    }
}
