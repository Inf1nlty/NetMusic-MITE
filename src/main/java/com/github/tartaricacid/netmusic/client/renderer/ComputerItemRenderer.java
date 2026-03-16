package com.github.tartaricacid.netmusic.client.renderer;

import net.minecraft.Block;
import net.minecraft.RenderBlocks;

public final class ComputerItemRenderer {
    private ComputerItemRenderer() {
    }

    public static void render(RenderBlocks renderer, Block block, int metadata) {
        ComputerRenderer.renderInventoryBlock(renderer, block, metadata);
    }
}

