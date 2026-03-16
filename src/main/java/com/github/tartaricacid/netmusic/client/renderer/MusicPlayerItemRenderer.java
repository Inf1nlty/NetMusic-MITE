package com.github.tartaricacid.netmusic.client.renderer;

import net.minecraft.Block;
import net.minecraft.RenderBlocks;

public final class MusicPlayerItemRenderer {
    private MusicPlayerItemRenderer() {
    }

    public static void render(RenderBlocks renderer, Block block, int metadata) {
        MusicPlayerRenderer.renderInventoryBlock(renderer, block, metadata);
    }
}