package com.github.tartaricacid.netmusic.inventory;

import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.Container;
import net.minecraft.EntityPlayer;
import net.minecraft.IInventory;
import net.minecraft.ItemStack;
import net.minecraft.Slot;

public class CDBurnerMenu extends Container {
    private final Slot input;
    private final Slot output;

    private ItemMusicCD.SongInfo songInfo;

    public CDBurnerMenu(EntityPlayer player) {
        super(player);

        IInventory inputInv = new OneSlotInventory();
        IInventory outputInv = new OneSlotInventory();

        this.input = this.addSlotToContainer(new Slot(inputInv, 0, 147, 14) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack != null && stack.itemID == InitItems.MUSIC_CD.itemID;
            }
        });
        this.output = this.addSlotToContainer(new Slot(outputInv, 0, 147, 67) {
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
        this.tryWriteSong(setSongInfo);
    }

    public String tryWriteSong(ItemMusicCD.SongInfo setSongInfo) {
        this.songInfo = setSongInfo;
        String failure = this.getWriteFailureKey();
        if (failure != null) {
            return failure;
        }

        ItemStack itemStack = this.input.getStack().copy();
        itemStack.stackSize = 1;
        this.input.getStack().stackSize -= 1;
        if (this.input.getStack().stackSize <= 0) {
            this.input.putStack(null);
        }
        ItemMusicCD.setSongInfo(setSongInfo, itemStack);
        this.output.putStack(itemStack);
        return null;
    }

    public boolean canWriteSong() {
        return this.getWriteFailureKey() == null;
    }

    public String getWriteFailureKey() {
        ItemStack in = this.input.getStack();
        if (in == null) {
            return "gui.netmusic.cd_burner.cd_is_empty";
        }
        if (this.output.getStack() != null) {
            return "gui.netmusic.cd_burner.output_not_empty";
        }
        ItemMusicCD.SongInfo raw = ItemMusicCD.getSongInfo(in);
        if (raw != null && raw.readOnly) {
            return "gui.netmusic.cd_burner.cd_read_only";
        }
        if (this.songInfo == null || this.songInfo.songTime <= 0) {
            return "gui.netmusic.cd_burner.get_info_error";
        }
        return null;
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
            return "container.netmusic.cd_burner";
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
