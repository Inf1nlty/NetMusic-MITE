package com.github.tartaricacid.netmusic.tileentity;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.TileEntity;
import javax.annotation.Nullable;

public class TileEntityCDBurner extends TileEntity {
    private static final String INPUT_TAG = "Input";
    private static final String OUTPUT_TAG = "Output";
    private static final String PROGRESS_TAG = "Progress";
    private static final String SONG_INFO_TAG = "SongInfo";
    private static final String LYRICS_TAG = "Lyrics";

    private ItemStack input = null;
    private ItemStack output = null;
    private int progress = 0;
    private ItemMusicCD.SongInfo songInfo = null;
    public @Nullable LyricRecord lyricRecord = null;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey(INPUT_TAG)) {
            this.input = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(INPUT_TAG));
        } else {
            this.input = null;
        }
        if (nbt.hasKey(OUTPUT_TAG)) {
            this.output = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(OUTPUT_TAG));
        } else {
            this.output = null;
        }
        this.progress = nbt.getInteger(PROGRESS_TAG);
        // SongInfo and Lyrics can be handled as needed (custom serialization)
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (this.input != null) {
            nbt.setCompoundTag(INPUT_TAG, this.input.writeToNBT(new NBTTagCompound()));
        }
        if (this.output != null) {
            nbt.setCompoundTag(OUTPUT_TAG, this.output.writeToNBT(new NBTTagCompound()));
        }
        nbt.setInteger(PROGRESS_TAG, this.progress);
        // SongInfo and Lyrics can be handled as needed (custom serialization)
    }

    public ItemStack getInput() {
        return input;
    }

    public void setInput(ItemStack input) {
        this.input = input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack output) {
        this.output = output;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public ItemMusicCD.SongInfo getSongInfo() {
        return songInfo;
    }

    public void setSongInfo(ItemMusicCD.SongInfo songInfo) {
        this.songInfo = songInfo;
    }
}
