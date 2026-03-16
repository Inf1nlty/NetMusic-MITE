package com.github.tartaricacid.netmusic.inventory;

import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityCDBurner;
import com.github.tartaricacid.netmusic.util.SongInfoHelper;
import net.minecraft.Container;
import net.minecraft.EntityPlayer;
import net.minecraft.IInventory;
import net.minecraft.ItemStack;
import net.minecraft.Slot;

public class CDBurnerMenu extends Container {

    private final Slot input;
    private final Slot output;

    private ItemMusicCD.SongInfo songInfo;
    private boolean closed;
    private final TileEntityCDBurner tileEntity;

    public CDBurnerMenu(EntityPlayer player, TileEntityCDBurner tileEntity)
    {
        super(player);
        this.tileEntity = tileEntity == null ? new TileEntityCDBurner() : tileEntity;
        IInventory inputInv = new OneSlotInventory(this.tileEntity, true);
        IInventory outputInv = new OneSlotInventory(this.tileEntity, false);
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

        for (int i = 0; i < 9; ++i)
        {
            this.addSlotToContainer(new Slot(player.inventory, i, 8 + i * 18, 152));
        }

        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 8 + j * 18, 94 + i * 18));
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
        ItemStack copied = null;
        Slot slot = (Slot) this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack())
        {
            ItemStack stack = slot.getStack();
            copied = stack.copy();

            if (index < 2)
            {
                if (!this.mergeItemStack(stack, 2, this.inventorySlots.size(), true)) {
                    return null;
                }
            }
            else if (!this.mergeItemStack(stack, 0, 2, false))
            {
                return null;
            }

            if (stack.stackSize <= 0)
            {
                slot.putStack(null);
            }
            else
            {
                slot.onSlotChanged();
            }
        }

        return copied;
    }

    @Override
    public void onContainerClosed(EntityPlayer player)
    {
        if (this.closed)
        {
            return;
        }
        this.closed = true;
        super.onContainerClosed(player);

        if (player == null || player.worldObj == null || player.worldObj.isRemote)
        {
            return;
        }

        ItemStack in = input.getStack();
        ItemStack out = output.getStack();
        input.putStack(null);
        output.putStack(null);
        tileEntity.setInput(null);
        tileEntity.setOutput(null);
        giveItemToPlayer(player, in);
        giveItemToPlayer(player, out);
    }

    private static void giveItemToPlayer(EntityPlayer player, ItemStack stack)
    {
        if (stack != null)
        {
            if (!player.inventory.addItemStackToInventory(stack))
            {
                player.dropPlayerItem(stack);
            }
        }
    }

    public void setSongInfo(ItemMusicCD.SongInfo setSongInfo)
    {
        this.songInfo = SongInfoHelper.sanitize(setSongInfo);
        tileEntity.setSongInfo(this.songInfo);
        this.tryWriteSong(this.songInfo);
    }

    public String tryWriteSong(ItemMusicCD.SongInfo setSongInfo)
    {
        if (this.closed)
        {
            return "gui.netmusic.cd_burner.get_info_error";
        }

        this.songInfo = SongInfoHelper.sanitize(setSongInfo);
        tileEntity.setSongInfo(this.songInfo);
        String failure = this.getWriteFailureKey();

        if (failure != null)
        {
            return failure;
        }

        ItemStack inputStack = this.input.getStack();
        ItemStack itemStack = inputStack.copy();
        itemStack.stackSize = 1;
        inputStack.stackSize -= 1;

        if (inputStack.stackSize <= 0)
        {
            this.input.putStack(null);
        }
        ItemMusicCD.SongInfo rawSongInfo = ItemMusicCD.getSongInfo(itemStack);

        if (rawSongInfo == null || !rawSongInfo.readOnly)
        {
            ItemMusicCD.setSongInfo(this.songInfo, itemStack);
        }

        this.output.putStack(itemStack);
        tileEntity.setInput(this.input.getStack());
        tileEntity.setOutput(this.output.getStack());
        return null;
    }

    public boolean canWriteSong() {
        return this.getWriteFailureKey() == null;
    }

    public String getWriteFailureKey()
    {
        ItemStack in = this.input.getStack();

        if (in == null)
        {
            return "gui.netmusic.cd_burner.cd_is_empty";
        }

        if (this.output.getStack() != null)
        {
            return "gui.netmusic.cd_burner.output_not_empty";
        }

        ItemMusicCD.SongInfo raw = ItemMusicCD.getSongInfo(in);
        if (raw != null && raw.readOnly)
        {
            return "gui.netmusic.cd_burner.cd_read_only";
        }

        if (this.songInfo == null || this.songInfo.songTime <= 0)
        {
            return "gui.netmusic.cd_burner.get_info_error";
        }

        return null;
    }

    public ItemMusicCD.SongInfo getSongInfo() {
        return SongInfoHelper.sanitize(this.songInfo);
    }

    public Slot getInput() {
        return input;
    }

    public Slot getOutput() {
        return output;
    }


    private static class OneSlotInventory implements IInventory
    {
        private final TileEntityCDBurner tileEntity;
        private final boolean isInput;

        public OneSlotInventory(TileEntityCDBurner tileEntity, boolean isInput)
        {
            this.tileEntity = tileEntity;
            this.isInput = isInput;
        }

        @Override
        public int getSizeInventory() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            return isInput ? tileEntity.getInput() : tileEntity.getOutput();
        }

        @Override
        public ItemStack decrStackSize(int i, int amount)
        {
            ItemStack stack = getStackInSlot(i);

            if (stack == null) return null;

            if (stack.stackSize <= amount)
            {
                setInventorySlotContents(i, null);
                return stack;
            }
            ItemStack split = stack.splitStack(amount);

            if (stack.stackSize <= 0) setInventorySlotContents(i, null);
            return split;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int i)
        {
            ItemStack result = getStackInSlot(i);
            setInventorySlotContents(i, null);
            return result;
        }

        @Override
        public void setInventorySlotContents(int i, ItemStack itemStack) {
            if (isInput) tileEntity.setInput(itemStack); else tileEntity.setOutput(itemStack);
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
        public void onInventoryChanged() {}

        @Override
        public boolean isUseableByPlayer(EntityPlayer entityPlayer) {
            return true;
        }

        @Override
        public void openChest() {}

        @Override
        public void closeChest() {}

        @Override
        public boolean isItemValidForSlot(int i, ItemStack itemStack) {
            return true;
        }

        @Override
        public void destroyInventory() {
            // No-op: nothing to destroy, method required by interface
        }
    }
}
