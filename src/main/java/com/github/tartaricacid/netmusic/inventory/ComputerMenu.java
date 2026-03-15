package com.github.tartaricacid.netmusic.inventory;

import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.Container;
import net.minecraft.EntityPlayer;
import net.minecraft.IInventory;
import net.minecraft.ItemStack;
import net.minecraft.Slot;

public class ComputerMenu extends Container {
    private final Slot input;
    private final Slot output;

    private ItemMusicCD.SongInfo songInfo;

    public ComputerMenu(EntityPlayer player) {
        super(player);
        IInventory inputInv = new OneSlotInventory();
        IInventory outputInv = new OneSlotInventory();

        this.input = this.addSlotToContainer(new Slot(inputInv, 0, 147, 14) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack != null && stack.itemID == InitItems.MUSIC_CD.itemID;
            }
        });
        this.output = this.addSlotToContainer(new Slot(outputInv, 0, 147, 79) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        giveItemToPlayer(player, input.getStack());
        giveItemToPlayer(player, output.getStack());
    }

    private static void giveItemToPlayer(EntityPlayer player, ItemStack stack) {
        if (stack != null) {
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropPlayerItem(stack);
            }
        }
    }

    public void setSongInfo(ItemMusicCD.SongInfo setSongInfo) {
        this.songInfo = setSongInfo;
        if (this.input.getStack() != null && this.output.getStack() == null) {
            ItemStack itemStack = this.input.getStack().copy();
            itemStack.stackSize = 1;
            this.input.getStack().stackSize -= 1;
            if (this.input.getStack().stackSize <= 0) {
                this.input.putStack(null);
            }
            ItemMusicCD.SongInfo rawSongInfo = ItemMusicCD.getSongInfo(itemStack);
            if (rawSongInfo == null || !rawSongInfo.readOnly) {
                ItemMusicCD.setSongInfo(this.songInfo, itemStack);
            }
            this.output.putStack(itemStack);
        }
    }

    public boolean canWriteSong() {
        ItemStack in = this.input.getStack();
        if (in == null || this.output.getStack() != null) {
            return false;
        }
        ItemMusicCD.SongInfo raw = ItemMusicCD.getSongInfo(in);
        return raw == null || !raw.readOnly;
    }

    public ItemMusicCD.SongInfo getSongInfo() {
        return this.songInfo;
    }

    public Slot getInput() {
        return input;
    }

    private static class OneSlotInventory implements IInventory {
        private ItemStack stack;

        @Override
        public int getSizeInventory() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            return this.stack;
        }

        @Override
        public ItemStack decrStackSize(int i, int amount) {
            if (this.stack == null) {
                return null;
            }
            if (this.stack.stackSize <= amount) {
                ItemStack result = this.stack;
                this.stack = null;
                return result;
            }
            ItemStack split = this.stack.splitStack(amount);
            if (this.stack.stackSize <= 0) {
                this.stack = null;
            }
            return split;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int i) {
            ItemStack result = this.stack;
            this.stack = null;
            return result;
        }

        @Override
        public void setInventorySlotContents(int i, ItemStack itemStack) {
            this.stack = itemStack;
        }

        @Override
        public String getCustomNameOrUnlocalized() {
            return "container.netmusic.computer";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void onInventoryChanged() {
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer entityPlayer) {
            return true;
        }

        @Override
        public void openChest() {
        }

        @Override
        public void closeChest() {
        }

        @Override
        public boolean isItemValidForSlot(int i, ItemStack itemStack) {
            return true;
        }

        @Override
        public void destroyInventory() {
            this.stack = null;
        }
    }
}
