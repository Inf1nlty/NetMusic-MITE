package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;

public final class MusicCdWriteHelper {
    private MusicCdWriteHelper() {
    }

    public static boolean writeSongToPlayerCd(EntityPlayer player, ItemMusicCD.SongInfo songInfo) {
        if (player == null || songInfo == null || player.inventory == null || InitItems.MUSIC_CD == null) {
            return false;
        }

        int slot = findWritableMusicCdSlot(player);
        if (slot < 0) {
            return false;
        }

        ItemStack stack = player.inventory.mainInventory[slot];
        if (stack == null) {
            return false;
        }

        if (stack.stackSize <= 1) {
            ItemMusicCD.setSongInfo(songInfo, stack);
            player.inventory.onInventoryChanged();
            return true;
        }

        ItemStack singleCd = stack.copy();
        singleCd.stackSize = 1;
        ItemMusicCD.setSongInfo(songInfo, singleCd);

        stack.stackSize -= 1;
        if (stack.stackSize <= 0) {
            player.inventory.mainInventory[slot] = null;
        }

        if (!player.inventory.addItemStackToInventory(singleCd)) {
            player.dropPlayerItem(singleCd);
        }
        player.inventory.onInventoryChanged();
        return true;
    }

    public static ItemStack createMusicCdFromSong(ItemMusicCD.SongInfo songInfo) {
        if (songInfo == null || InitItems.MUSIC_CD == null) {
            return null;
        }
        ItemStack musicCd = new ItemStack(InitItems.MUSIC_CD, 1);
        ItemMusicCD.setSongInfo(songInfo, musicCd);
        return musicCd;
    }

    public static boolean giveSongCdToPlayer(EntityPlayer player, ItemMusicCD.SongInfo songInfo) {
        if (player == null) {
            return false;
        }
        ItemStack musicCd = createMusicCdFromSong(songInfo);
        if (musicCd == null) {
            return false;
        }

        if (!player.inventory.addItemStackToInventory(musicCd)) {
            player.dropPlayerItem(musicCd);
        }
        player.inventory.onInventoryChanged();
        return true;
    }

    public static int findWritableMusicCdSlot(EntityPlayer player) {
        int current = player.inventory.currentItem;
        if (current >= 0 && current < player.inventory.mainInventory.length) {
            ItemStack held = player.inventory.mainInventory[current];
            if (isWritableCd(held)) {
                return current;
            }
        }

        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (isWritableCd(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isWritableCd(ItemStack stack) {
        if (stack == null || InitItems.MUSIC_CD == null || stack.itemID != InitItems.MUSIC_CD.itemID) {
            return false;
        }
        ItemMusicCD.SongInfo existing = ItemMusicCD.getSongInfo(stack);
        return existing == null || !existing.readOnly;
    }
}
