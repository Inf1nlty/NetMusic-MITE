package com.github.tartaricacid.netmusic.tileentity;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;
import com.github.tartaricacid.netmusic.network.message.MusicPlayerStateMessage;
import com.github.tartaricacid.netmusic.util.SongInfoHelper;
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
    private static final String PLAY_SESSION_TAG = "PlaySessionId";
    private static final String ITEM_TAG = "MusicCd";
    private static final String ITEM_INFO_TAG = "MusicCdInfo";
    private static final String ACTIVE_INFO_TAG = "ActiveSongInfo";
    private static final String ITEM_COUNT_TAG = "MusicCdCount";
    private static final String ITEM_SUBTYPE_TAG = "MusicCdSubtype";

    private ItemStack[] items = new ItemStack[1];
    private boolean isPlay = false;
    private int currentTime;
    private boolean hasSignal = false;
    private int syncTickCounter = 0;
    private int playSessionId = 0;
    private @Nullable ItemMusicCD.SongInfo activeSongInfo;

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
        this.playSessionId = Math.max(0, nbt.getInteger(PLAY_SESSION_TAG));
        if (this.isPlay && this.playSessionId <= 0) {
            this.playSessionId = 1;
        }
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
        ItemMusicCD.SongInfo activeFromNbt = null;
        if (nbt.hasKey(ACTIVE_INFO_TAG)) {
            activeFromNbt = SongInfoHelper.sanitize(ItemMusicCD.SongInfo.deserializeNBT(nbt.getCompoundTag(ACTIVE_INFO_TAG)));
        }
        this.activeSongInfo = this.isPlay ? (activeFromNbt != null ? activeFromNbt : resolvePlaybackInfo(this.items[0])) : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean(IS_PLAY_TAG, this.isPlay);
        nbt.setInteger(CURRENT_TIME_TAG, this.currentTime);
        nbt.setBoolean(SIGNAL_TAG, this.hasSignal);
        nbt.setInteger(PLAY_SESSION_TAG, this.playSessionId);
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
        ItemMusicCD.SongInfo active = SongInfoHelper.sanitize(this.activeSongInfo);
        if (active != null) {
            NBTTagCompound activeTag = new NBTTagCompound();
            ItemMusicCD.SongInfo.serializeNBT(active, activeTag);
            nbt.setCompoundTag(ACTIVE_INFO_TAG, activeTag);
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
        if (!play) {
            this.activeSongInfo = null;
        }
        isPlay = play;
    }

    public void setPlayToClient(ItemMusicCD.SongInfo info) {
        ItemMusicCD.SongInfo sanitized = SongInfoHelper.sanitize(info);
        if (sanitized == null) {
            return;
        }
        this.activeSongInfo = sanitized;
        this.setCurrentTime(sanitized.songTime * 20 + 64);
        this.isPlay = true;
        this.playSessionId = nextPlaySessionId(this.playSessionId);
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.syncStateToClients();
        }
    }

    public void setChanged() {
        ItemStack stack = getItem(0);
        if (stack == null) {
            setPlay(false);
            setCurrentTime(0);
            this.activeSongInfo = null;
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
        if (this.syncTickCounter >= 20) {
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
                ItemMusicCD.SongInfo songInfo = resolvePlaybackInfo(stackInSlot);
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

    public void applyClientSync(boolean play, int currentTime, boolean signal, int playSessionId, @Nullable ItemStack stack) {
        this.isPlay = play;
        this.currentTime = currentTime;
        this.hasSignal = signal;
        this.playSessionId = Math.max(0, playSessionId);
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
        ItemMusicCD.SongInfo info = this.isPlay ? SongInfoHelper.copy(this.activeSongInfo) : null;
        if (info == null) {
            info = stack == null ? null : ItemMusicCD.getSongInfo(stack);
        }
        String songUrl = info == null || info.songUrl == null ? "" : info.songUrl;
        int songTime = info == null ? 0 : Math.max(0, info.songTime);
        String songName = info == null || info.songName == null ? "" : info.songName;

        if (this.isPlay && this.playSessionId <= 0) {
            this.playSessionId = 1;
        }

        NetworkHandler.sendToNearBy(this.worldObj, this.xCoord, this.yCoord, this.zCoord,
                new MusicPlayerStateMessage(this.xCoord, this.yCoord, this.zCoord,
                        this.isPlay, this.currentTime, this.hasSignal, this.playSessionId, stack, songUrl, songTime, songName));

        if (this.isPlay && songTime > 0 && !songUrl.isEmpty()) {
            int startTick = computeStartTick(songTime, this.currentTime);
            NetworkHandler.sendToNearBy(this.worldObj, this.xCoord, this.yCoord, this.zCoord,
                    new MusicToClientMessage(this.xCoord, this.yCoord, this.zCoord,
                            songUrl, songTime, songName, startTick, this.playSessionId));
        }
    }

    public static int computeStartTick(int songTimeSecond, int currentTime) {
        int totalTicks = Math.max(1, songTimeSecond) * 20;
        int remainingMusicTicks = Math.max(0, currentTime - 64);
        return Math.max(0, Math.min(totalTicks, totalTicks - remainingMusicTicks));
    }

    public void setPreparedSongInfo(@Nullable ItemMusicCD.SongInfo songInfo) {
        this.activeSongInfo = SongInfoHelper.sanitize(songInfo);
    }

    @Nullable
    public ItemMusicCD.SongInfo getPreparedSongInfo() {
        return SongInfoHelper.copy(this.activeSongInfo);
    }

    @Nullable
    private ItemMusicCD.SongInfo resolvePlaybackInfo(@Nullable ItemStack stack) {
        ItemMusicCD.SongInfo preferred = SongInfoHelper.sanitize(this.activeSongInfo);
        if (preferred != null) {
            return preferred;
        }
        if (stack == null) {
            return null;
        }
        return SongInfoHelper.sanitize(ItemMusicCD.getSongInfo(stack));
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

    private static int nextPlaySessionId(int current) {
        if (current >= Integer.MAX_VALUE - 1) {
            return 1;
        }
        return current + 1;
    }
}
