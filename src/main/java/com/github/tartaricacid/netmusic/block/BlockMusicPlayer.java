package com.github.tartaricacid.netmusic.block;

import com.github.tartaricacid.netmusic.client.renderer.RenderTypes;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.github.tartaricacid.netmusic.creativetab.NetMusicCreativeTab;
import net.minecraft.BlockBreakInfo;
import net.minecraft.BlockConstants;
import net.minecraft.BlockDirectionalWithTileEntity;
import net.minecraft.Container;
import net.minecraft.Entity;
import net.minecraft.EntityPlayer;
import net.minecraft.EnumDirection;
import net.minecraft.EnumFace;
import net.minecraft.IBlockAccess;
import net.minecraft.ItemStack;
import net.minecraft.Material;
import net.minecraft.StringHelper;
import net.minecraft.TileEntity;
import net.minecraft.World;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public class BlockMusicPlayer extends BlockDirectionalWithTileEntity {
    public static final int CYCLE_DISABLE_MASK = 4;

    public BlockMusicPlayer() {
        this(IdUtil.getNextBlockID());
    }

    public BlockMusicPlayer(int id) {
        super(id, Material.wood, new BlockConstants());
        this.setHardness(0.5F);
        this.setStepSound(soundWoodFootstep);
        this.setCreativeTab(NetMusicCreativeTab.TAB);
        this.setBlockBoundsForAllThreads(0.125, 0.0, 0.125, 0.875, 0.375, 0.875);
    }

    @Override
    public TileEntity createNewTileEntity(World world) {
        return new TileEntityMusicPlayer();
    }

    @Override
    public EnumDirection getDirectionFacing(int metadata) {
        return this.getDirectionFacingStandard4(metadata & 3);
    }

    @Override
    public int getMetadataForDirectionFacing(int metadata, EnumDirection direction) {
        int facingBits;
        if (direction.isSouth()) {
            facingBits = 0;
        } else if (direction.isWest()) {
            facingBits = 1;
        } else if (direction.isNorth()) {
            facingBits = 2;
        } else if (direction.isEast()) {
            facingBits = 3;
        } else {
            facingBits = metadata & 3;
        }
        return (metadata & CYCLE_DISABLE_MASK) | facingBits;
    }

    @Override
    public int getMetadataForPlacement(World world, int x, int y, int z, ItemStack itemStack, Entity entity, EnumFace face, float offsetX, float offsetY, float offsetZ) {
        int metadata = super.getMetadataForPlacement(world, x, y, z, itemStack, entity, face, offsetX, offsetY, offsetZ);
        return metadata | CYCLE_DISABLE_MASK;
    }

    public static boolean isCycleDisabled(int metadata) {
        return (metadata & CYCLE_DISABLE_MASK) != 0;
    }

    @Override
    public boolean isStandardFormCube(boolean[] is_standard_form_cube, int metadata) {
        return false;
    }

    @Override
    public int getRenderType() {
        return RenderTypes.musicPlayerRenderType;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityMusicPlayer musicPlayer) {
            if (musicPlayer.getItem(0) != null) {
                if (musicPlayer.isPlay()) {
                    return 15;
                }
                return 7;
            }
        }
        return 0;
    }

    @Override
    public boolean onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockId) {
        playMusic(world, x, y, z, world.isBlockIndirectlyGettingPowered(x, y, z));
        return false;
    }

    private static void playMusic(World world, int x, int y, int z, boolean signal) {
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityMusicPlayer musicPlayer) {
            if (signal != musicPlayer.hasSignal()) {
                if (signal) {
                    if (musicPlayer.isPlay()) {
                        musicPlayer.setPlay(false);
                        musicPlayer.setSignal(signal);
                        musicPlayer.setChanged();
                        return;
                    }
                    ItemStack stackInSlot = musicPlayer.getItem(0);
                    if (stackInSlot == null) {
                        musicPlayer.setSignal(signal);
                        musicPlayer.setChanged();
                        return;
                    }
                    ItemMusicCD.SongInfo songInfo = ItemMusicCD.getSongInfo(stackInSlot);
                    if (songInfo != null) {
                        musicPlayer.setPlayToClient(songInfo);
                    }
                }
                musicPlayer.setSignal(signal);
                musicPlayer.setChanged();
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, EnumFace face, float offsetX, float offsetY, float offsetZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntityMusicPlayer musicPlayer)) {
            return false;
        }

        ItemStack stack = musicPlayer.getItem(0);
        if (stack != null) {
            if (musicPlayer.isPlay()) {
                musicPlayer.setPlay(false);
                musicPlayer.setCurrentTime(0);
            }
            ItemStack removed = musicPlayer.removeItem(0, 1);
            if (removed != null && !player.inventory.addItemStackToInventory(removed)) {
                player.dropPlayerItem(removed);
            }
            musicPlayer.setChanged();
            return true;
        }

        ItemStack heldStack = player.getHeldItemStack();
        ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(heldStack);
        if (info == null) {
            return false;
        }
        if (info.vip && !GeneralConfig.hasVipCookieForUrl(info.songUrl)) {
            player.addChatMessage("message.netmusic.music_player.need_vip");
            return true;
        }

        ItemStack one = heldStack.copy();
        one.stackSize = 1;
        musicPlayer.setItem(0, one);
        if (!player.inCreativeMode()) {
            heldStack.stackSize--;
            if (heldStack.stackSize <= 0) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
        }
        musicPlayer.setPlayToClient(info);
        musicPlayer.setChanged();
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, int blockId, int metadata) {
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityMusicPlayer musicPlayer) {
            ItemStack stack = musicPlayer.getItem(0);
            if (stack != null) {
                this.dropBlockAsEntityItem(new BlockBreakInfo(world, x, y, z), stack);
            }
        }
        super.breakBlock(world, x, y, z, blockId, metadata);
    }

    @Override
    public void setBlockBoundsBasedOnStateAndNeighbors(IBlockAccess blockAccess, int x, int y, int z) {
        this.setBlockBoundsForAllThreads(0.125, 0.0, 0.125, 0.875, 0.375, 0.875);
    }

    @Override
    public void setBlockBoundsForItemRender(int itemDamage) {
        this.setBlockBoundsForAllThreads(0.125, 0.0, 0.125, 0.875, 0.375, 0.875);
    }

    @Override
    public String getMetadataNotes() {
        String[] array = new String[8];
        for (int i = 0; i < array.length; ++i) {
            array[i] = i + "=" + this.getDirectionFacing(i).getDescriptor(true) + ",cycle_disabled=" + isCycleDisabled(i);
        }
        return StringHelper.implode(array, ", ", true, false);
    }

    @Override
    public boolean isValidMetadata(int metadata) {
        return metadata >= 0 && metadata < 8;
    }
}
