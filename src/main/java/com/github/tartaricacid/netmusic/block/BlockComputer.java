package com.github.tartaricacid.netmusic.block;

import com.github.tartaricacid.netmusic.client.renderer.RenderTypes;
import com.github.tartaricacid.netmusic.creativetab.NetMusicCreativeTab;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.OpenMenuMessage;
import com.github.tartaricacid.netmusic.tileentity.TileEntityComputer;
import com.github.tartaricacid.netmusic.util.ServerWindowIdHelper;
import net.minecraft.*;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public class BlockComputer extends BlockDirectionalWithTileEntity {

    public BlockComputer() {
        this(IdUtil.getNextBlockID());
    }

    public BlockComputer(int id) {
        super(id, Material.wood, new BlockConstants());
        this.setHardness(0.5F);
        this.setStepSound(soundWoodFootstep);
        this.setCreativeTab(NetMusicCreativeTab.TAB);
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.03125, 1.0, 1.0625, 1.0);
    }

    @Override
    public EnumDirection getDirectionFacing(int metadata) {
        return this.getDirectionFacingStandard4(metadata);
    }

    @Override
    public int getMetadataForDirectionFacing(int metadata, EnumDirection direction) {
        if (direction.isSouth()) {
            return 0;
        }
        if (direction.isWest()) {
            return 1;
        }
        if (direction.isNorth()) {
            return 2;
        }
        if (direction.isEast()) {
            return 3;
        }
        return metadata;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, EnumFace face, float offsetX, float offsetY, float offsetZ) {
        if (world.isRemote) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, x, y, z);
        }
        return true;
    }

    private static void openMenu(ServerPlayer player, int x, int y, int z) {
        ComputerMenu menu = new ComputerMenu(player);
        if (player.openContainer != player.inventoryContainer) {
            player.closeScreen();
        }

        int windowId = ServerWindowIdHelper.nextWindowId(player);
        player.openContainer = menu;
        player.openContainer.windowId = windowId;

        NetworkHandler.sendToClientPlayer(
                new OpenMenuMessage(OpenMenuMessage.Type.COMPUTER, windowId, x, y, z),
                player
        );
        player.openContainer.addCraftingToCrafters(player);
    }

    @Override
    public void setBlockBoundsBasedOnStateAndNeighbors(IBlockAccess blockAccess, int x, int y, int z) {
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.03125, 1.0, 1.0625, 1.0);
    }

    @Override
    public void setBlockBoundsForItemRender(int itemDamage) {
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.03125, 1.0, 1.0625, 1.0);
    }

    @Override
    public boolean isStandardFormCube(boolean[] is_standard_form_cube, int metadata) {
        return false;
    }

    @Override
    public String getMetadataNotes() {
        String[] array = new String[4];
        for (int i = 0; i < array.length; ++i) {
            array[i] = i + "=" + this.getDirectionFacing(i).getDescriptor(true);
        }
        return StringHelper.implode(array, ", ", true, false);
    }

    @Override
    public boolean isValidMetadata(int metadata) {
        return metadata >= 0 && metadata < 4;
    }

    @Override
    public boolean isPortable(World world, EntityLivingBase entity_living_base, int x, int y, int z) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world) {
        return new TileEntityComputer();
    }

    @Override
    public int getRenderType() {
        return RenderTypes.computerRenderType;
    }
}
