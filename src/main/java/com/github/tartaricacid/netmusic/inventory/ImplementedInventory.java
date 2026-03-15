package com.github.tartaricacid.netmusic.inventory;

import net.minecraft.EntityPlayer;
import net.minecraft.IInventory;
import net.minecraft.ItemStack;

import java.util.List;

public interface ImplementedInventory extends IInventory {
    List<ItemStack> getItems();

    @Override
    default int getSizeInventory() {
        return getItems().size();
    }

    default boolean isEmpty() {
        for (int i = 0; i < getSizeInventory(); i++) {
            if (getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getStackInSlot(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack decrStackSize(int slot, int amount) {
        ItemStack stack = getStackInSlot(slot);
        if (stack == null) {
            return null;
        }
        if (stack.stackSize <= amount) {
            setInventorySlotContents(slot, null);
            onInventoryChanged();
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            setInventorySlotContents(slot, null);
        }
        onInventoryChanged();
        return split;
    }

    @Override
    default ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = getStackInSlot(slot);
        setInventorySlotContents(slot, null);
        return stack;
    }

    @Override
    default void setInventorySlotContents(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    default String getCustomNameOrUnlocalized() {
        return "container.netmusic";
    }

    @Override
    default boolean hasCustomName() {
        return false;
    }

    @Override
    default int getInventoryStackLimit() {
        return 64;
    }

    @Override
    default boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    default void openChest() {
    }

    @Override
    default void closeChest() {
    }

    @Override
    default boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    default void destroyInventory() {
        for (int i = 0; i < getSizeInventory(); i++) {
            setInventorySlotContents(i, null);
        }
    }
}
