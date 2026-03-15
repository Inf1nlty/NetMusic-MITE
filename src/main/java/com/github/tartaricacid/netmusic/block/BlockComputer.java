package com.github.tartaricacid.netmusic.block;

import com.github.tartaricacid.netmusic.util.MusicCdWriteHelper;
import com.github.tartaricacid.netmusic.util.PendingSongTracker;
import com.github.tartaricacid.netmusic.util.PlayerInteractionTracker;
import net.minecraft.*;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public class BlockComputer extends BlockDirectional {

    public BlockComputer() {
        this(IdUtil.getNextBlockID());
    }

    public BlockComputer(int id) {
        super(id, Material.wood, new BlockConstants());
        this.setHardness(0.5F);
        this.setStepSound(soundWoodFootstep);
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
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
            PlayerInteractionTracker.markComputer(player, world.getTotalWorldTime(), x, y, z);
            if (tryApplyPendingSong(world, player)) {
                return true;
            }
            player.addChatMessage("message.netmusic.computer.hint");
            // Temporary compatibility bridge: keep interaction alive until legacy custom container wiring is finished.
            player.displayGUIWorkbench(x, y, z);
        }
        return true;
    }

    private static boolean tryApplyPendingSong(World world, EntityPlayer player) {
        PendingSongTracker.PendingSong pending = PendingSongTracker.getPendingForSource(
                player, PendingSongTracker.Source.COMPUTER, world.getTotalWorldTime(), 20L * 120L);
        if (pending == null) {
            return false;
        }

        if (!MusicCdWriteHelper.writeSongToPlayerCd(player, pending.songInfo)) {
            player.addChatMessage("message.netmusic.music_cd.need_writable_cd");
            return true;
        }

        PendingSongTracker.clear(player);
        player.addChatMessage("message.netmusic.computer.applied");
        return true;
    }

    @Override
    public void setBlockBoundsBasedOnStateAndNeighbors(IBlockAccess blockAccess, int x, int y, int z) {
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }

    @Override
    public void setBlockBoundsForItemRender(int itemDamage) {
        this.setBlockBoundsForAllThreads(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
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
