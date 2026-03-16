package com.github.tartaricacid.netmusic.mixin;

import com.github.tartaricacid.netmusic.client.renderer.CDBurnerTileEntityRenderer;
import com.github.tartaricacid.netmusic.client.renderer.ComputerTileEntityRenderer;
import com.github.tartaricacid.netmusic.client.renderer.MusicPlayerTileEntityRenderer;
import com.github.tartaricacid.netmusic.client.renderer.RenderTypes;
import net.minecraft.Block;
import net.minecraft.IBlockAccess;
import net.minecraft.RenderBlocks;
import net.minecraft.Tessellator;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderBlocks.class)
public abstract class RenderBlocksMixin {
    @Shadow public IBlockAccess blockAccess;

    @Inject(method = "renderItemIn3d", at = @At("HEAD"), cancellable = true)
    private static void netmusic$renderCustomBlockItemsIn3d(int renderType, CallbackInfoReturnable<Boolean> cir) {
        if (renderType == RenderTypes.musicPlayerRenderType
                || renderType == RenderTypes.cdBurnerRenderType
                || renderType == RenderTypes.computerRenderType) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderMusicPlayerWorld(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (this.blockAccess == null || block.getRenderType() != RenderTypes.musicPlayerRenderType) {
            return;
        }
        // Match 1.20 behavior: music player base is rendered in TileEntity renderer.
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "renderBlockAsItem", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderMusicPlayerItem(Block block, int metadata, float brightness, CallbackInfo ci) {
        if (block.getRenderType() != RenderTypes.musicPlayerRenderType) {
            return;
        }
        GL11.glPushMatrix();
        Tessellator t = Tessellator.instance;
        double ox = t.xOffset, oy = t.yOffset, oz = t.zOffset;
        t.setTranslation(0.0D, 0.0D, 0.0D);
        try {
            MusicPlayerTileEntityRenderer.renderModelAsItem(metadata);
        } finally {
            t.setTranslation(ox, oy, oz);
        }
        GL11.glPopMatrix();
        ci.cancel();
    }

    @Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderComputerWorld(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (this.blockAccess == null || block.getRenderType() != RenderTypes.computerRenderType) {
            return;
        }
        // Rendered in ComputerTileEntityRenderer to support model UVs from the Blockbench json.
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "renderBlockAsItem", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderComputerItem(Block block, int metadata, float brightness, CallbackInfo ci) {
        if (block.getRenderType() != RenderTypes.computerRenderType) {
            return;
        }
        GL11.glPushMatrix();
        Tessellator t = Tessellator.instance;
        double ox = t.xOffset, oy = t.yOffset, oz = t.zOffset;
        t.setTranslation(0.0D, 0.0D, 0.0D);
        try {
            ComputerTileEntityRenderer.renderModelAsItem(metadata);
        } finally {
            t.setTranslation(ox, oy, oz);
            GL11.glPopMatrix();
        }
        ci.cancel();
    }

    @Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderCDBurnerWorld(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (this.blockAccess == null) {
            return;
        }
        int renderType = block.getRenderType();
        if (renderType != RenderTypes.cdBurnerRenderType) {
            return;
        }
        // Skip default block rendering for cd burner; the TileEntitySpecialRenderer will render it in the tile entity pass.
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "renderBlockAsItem", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderCDBurnerItem(Block par1Block, int par2, float par3, CallbackInfo ci) {
        int renderType = par1Block.getRenderType();
        if (renderType == RenderTypes.cdBurnerRenderType) {
            GL11.glPushMatrix();
            Tessellator t = Tessellator.instance;
            double ox = t.xOffset, oy = t.yOffset, oz = t.zOffset;
            t.setTranslation(0.0D, 0.0D, 0.0D);
            try {
                CDBurnerTileEntityRenderer.renderModelAsItem(par2);
                GL11.glEnable(32826);
            } finally {
                t.setTranslation(ox, oy, oz);
            }
            GL11.glPopMatrix();
            ci.cancel();
        }
    }
}
