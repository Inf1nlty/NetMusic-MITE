package com.github.tartaricacid.netmusic.client.renderer;

import com.github.tartaricacid.netmusic.client.model.ModelCDBurner;
import net.minecraft.Block;
import net.minecraft.Icon;
import net.minecraft.RenderBlocks;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import net.minecraft.TileEntitySpecialRenderer;
import net.minecraft.TextureManager;
import net.minecraft.Tessellator;
import net.minecraft.Minecraft;
import org.lwjgl.opengl.GL11;

public class CDBurnerTileEntityRenderer extends TileEntitySpecialRenderer {
    public static final ResourceLocation TEXTURE = new ResourceLocation("netmusic:textures/blocks/cd_burner.png");

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + 0.5F, (float) y + 1.0F, (float) z + 0.5F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        TextureManager manager = Minecraft.getMinecraft().getTextureManager();
        manager.bindTexture(TEXTURE);

        // Prepare a RenderBlocks for rendering faces like inventory rendering
        RenderBlocks renderer = new RenderBlocks(Minecraft.getMinecraft().theWorld);
        Block block = com.github.tartaricacid.netmusic.init.InitBlocks.CD_BURNER;
        Icon icon = block.getIcon(0, 0);
        Tessellator tessellator = Tessellator.instance;

        // Render each cuboid from the model
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
