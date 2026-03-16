package com.github.tartaricacid.netmusic.client.renderer;

import com.github.tartaricacid.netmusic.client.model.ModelCDBurner;
import net.minecraft.Block;
import net.minecraft.IBlockAccess;
import net.minecraft.Minecraft;
import net.minecraft.RenderBlocks;
import net.minecraft.Tessellator;
import net.minecraft.TextureMap;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class CDBurnerRenderer {
    public static final CDBurnerRenderer INSTANCE = new CDBurnerRenderer();

    private CDBurnerRenderer() {}

    public static boolean renderWorldBlock(RenderBlocks renderer, Block block, IBlockAccess blockAccess, int x, int y, int z) {
        if (renderer == null || block == null) {
            return false;
        }
        int metadata = blockAccess.getBlockMetadata(x, y, z);
        List<CuboidRenderHelper.Cuboid> cuboids = CuboidRenderHelper.rotateCuboidsY(ModelCDBurner.getCuboids(), getTurnsFromMetadata(metadata));
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(blockAccess, x, y, z));
        Tessellator.instance.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        for (CuboidRenderHelper.Cuboid cuboid : cuboids) {
            CuboidRenderHelper.renderCuboidFacesWorld(renderer, block, x, y, z, metadata, cuboid, cuboids);
        }
        return true;
    }

    public static void renderInventoryBlock(RenderBlocks renderer, Block block, int metadata) {
        if (renderer == null || block == null) {
            return;
        }
        // Our RenderBlocks mixin cancels vanilla renderBlockAsItem() before it binds the blocks atlas.
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        // Tessellator translation is global state and is NOT reset by startDrawingQuads().
        // If anything left a non-zero translation, item rendering will explode into "torn" geometry.
        Tessellator t = Tessellator.instance;
        double ox = t.xOffset, oy = t.yOffset, oz = t.zOffset;
        try {
            t.setTranslation(0.0D, 0.0D, 0.0D);
            List<CuboidRenderHelper.Cuboid> cuboids = CuboidRenderHelper.rotateCuboidsY(ModelCDBurner.getCuboids(), 3);
            for (CuboidRenderHelper.Cuboid cuboid : cuboids) {
                CuboidRenderHelper.renderCuboidFacesImmediate(renderer, block, metadata, cuboid, cuboids);
            }
        } finally {
            t.setTranslation(ox, oy, oz);
        }
        GL11.glPopMatrix();
    }

    private static int getTurnsFromMetadata(int metadata) {
        switch (metadata & 3) {
            case 0:
                return 2; // south
            case 1:
                return 3; // west
            case 2:
                return 0; // north
            case 3:
                return 1; // east
            default:
                return 0;
        }
    }
}
