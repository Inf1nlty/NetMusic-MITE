package com.github.tartaricacid.netmusic.inventory;

import com.github.tartaricacid.netmusic.init.InitItems;
import net.minecraft.ItemStack;

public interface MusicPlayerInv extends ImplementedInventory {
    @Override
    default boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot != 0 || stack == null) {
            return false;
        }
        ItemStack current = getStackInSlot(0);
        if (current != null && current.stackSize >= getInventoryStackLimit()) {
            return false;
        }
        return stack.itemID == InitItems.MUSIC_CD.itemID;
    }
}
