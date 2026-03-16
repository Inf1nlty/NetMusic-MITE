package com.github.tartaricacid.netmusic.mixin;

import com.github.tartaricacid.netmusic.client.renderer.MusicPlayerItemRenderer;
import com.github.tartaricacid.netmusic.client.renderer.MusicPlayerRenderer;
import com.github.tartaricacid.netmusic.client.renderer.RenderTypes;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import net.minecraft.Block;
import net.minecraft.IBlockAccess;
import net.minecraft.RenderBlocks;
import net.minecraft.TileEntityRenderer;
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

    @Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderMusicPlayerWorld(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (block != InitBlocks.MUSIC_PLAYER || this.blockAccess == null) {
            return;
        }
        RenderBlocks renderer = (RenderBlocks) (Object) this;
        cir.setReturnValue(MusicPlayerRenderer.renderWorldBlock(renderer, block, this.blockAccess, x, y, z));
    }

    @Inject(method = "renderBlockAsItem", at = @At("HEAD"), cancellable = true)
    private void netmusic$renderMusicPlayerItem(Block block, int metadata, float brightness, CallbackInfo ci) {
        if (block != InitBlocks.MUSIC_PLAYER) {
            return;
        }
        RenderBlocks renderer = (RenderBlocks) (Object) this;
        MusicPlayerItemRenderer.render(renderer, block, metadata);
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

    @Inject(method = "renderBlockAsItem", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/Block;getRenderType()I"), cancellable = true)
    private void netmusic$renderCDBurnerItem(Block par1Block, int par2, float par3, CallbackInfo ci) {
        int renderType = par1Block.getRenderType();
        if (renderType == RenderTypes.cdBurnerRenderType) {
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            TileEntityRenderer.instance.renderTileEntityAt(new com.github.tartaricacid.netmusic.tileentity.TileEntityCDBurner(), 0.0D, 0.0D, 0.0D, 0.0F);
            GL11.glEnable(32826);
            ci.cancel();
        }
    }
}
