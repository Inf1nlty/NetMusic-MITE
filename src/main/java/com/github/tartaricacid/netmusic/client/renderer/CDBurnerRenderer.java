package com.github.tartaricacid.netmusic.client.renderer;

import com.github.tartaricacid.netmusic.client.model.ModelCDBurner;
import net.minecraft.Block;
import net.minecraft.IBlockAccess;
import net.minecraft.Icon;
import net.minecraft.RenderBlocks;
import net.minecraft.Tessellator;
import org.lwjgl.opengl.GL11;

public final class CDBurnerRenderer {
    public static final CDBurnerRenderer INSTANCE = new CDBurnerRenderer();

    private CDBurnerRenderer() {}

    public static boolean renderWorldBlock(RenderBlocks renderer, Block block, IBlockAccess blockAccess, int x, int y, int z) {
        if (renderer == null || block == null) {
            return false;
        }
        for (ModelCDBurner.Cuboid cuboid : ModelCDBurner.getCuboids()) {
            renderer.setRenderBounds(cuboid.minX, cuboid.minY, cuboid.minZ, cuboid.maxX, cuboid.maxY, cuboid.maxZ);
            renderer.renderStandardBlock(block, x, y, z);
        }
        return true;
    }

    public static void renderInventoryBlock(RenderBlocks renderer, Block block, int metadata) {
        if (renderer == null || block == null) {
            return;
        }
        Icon icon = block.getIcon(0, metadata);
        Tessellator tessellator = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        for (ModelCDBurner.Cuboid cuboid : ModelCDBurner.getCuboids()) {
            renderer.setRenderBounds(cuboid.minX, cuboid.minY, cuboid.minZ, cuboid.maxX, cuboid.maxY, cuboid.maxZ);
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, -1.0F, 0.0F);
            renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, icon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, icon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, -1.0F);
            renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, icon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, icon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(-1.0F, 0.0F, 0.0F);
            renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, icon);
            tessellator.draw();
            tessellator.startDrawingQuads();
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, icon);
            tessellator.draw();
        }
        GL11.glPopMatrix();
    }
}
