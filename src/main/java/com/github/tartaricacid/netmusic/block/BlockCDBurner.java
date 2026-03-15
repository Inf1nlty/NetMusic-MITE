package com.github.tartaricacid.netmusic.block;

import com.github.tartaricacid.netmusic.creativetab.NetMusicCreativeTab;
import com.github.tartaricacid.netmusic.util.MusicCdWriteHelper;
import com.github.tartaricacid.netmusic.util.PendingSongTracker;
import com.github.tartaricacid.netmusic.util.PlayerInteractionTracker;
import net.minecraft.*;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public class BlockCDBurner extends BlockDirectional {

    public BlockCDBurner() {
        this(IdUtil.getNextBlockID());
    }

    public BlockCDBurner(int id) {
        super(id, Material.wood, new BlockConstants());
        this.setHardness(0.5F);
        this.setStepSound(soundWoodFootstep);
        this.setCreativeTab(NetMusicCreativeTab.TAB);
        this.setTextureName("netmusic:block/cd_burner");
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
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
        if (player.onServer()) {
            PlayerInteractionTracker.markCdBurner(player, world.getTotalWorldTime(), x, y, z);
            if (tryApplyPendingSong(world, player)) {
                return true;
            }
            player.addChatMessage("message.netmusic.cd_burner.hint");
            // Temporary compatibility bridge: keeps right-click interaction alive until custom Container migration is finished.
            player.displayGUIWorkbench(x, y, z);
        }
        return true;
    }

    private static boolean tryApplyPendingSong(World world, EntityPlayer player) {
        PendingSongTracker.PendingSong pending = PendingSongTracker.getPendingForSource(
                player, PendingSongTracker.Source.CD_BURNER, world.getTotalWorldTime(), 20L * 120L);
        if (pending == null) {
            return false;
        }

        if (!MusicCdWriteHelper.writeSongToPlayerCd(player, pending.songInfo)) {
            player.addChatMessage("message.netmusic.music_cd.need_writable_cd");
            return true;
        }

        PendingSongTracker.clear(player);
        player.addChatMessage("message.netmusic.cd_burner.applied");
        return true;
    }

    @Override
    public void setBlockBoundsBasedOnStateAndNeighbors(IBlockAccess blockAccess, int x, int y, int z) {
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
    }

    @Override
    public void setBlockBoundsForItemRender(int itemDamage) {
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
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
}
