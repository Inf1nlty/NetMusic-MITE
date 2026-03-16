package com.github.tartaricacid.netmusic.mixin;

import com.github.tartaricacid.netmusic.client.renderer.MusicPlayerItemRenderer;
import com.github.tartaricacid.netmusic.client.renderer.MusicPlayerRenderer;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import net.minecraft.Block;
import net.minecraft.IBlockAccess;
import net.minecraft.RenderBlocks;
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
}
