package com.github.tartaricacid.netmusic.client.renderer;

import net.minecraft.*;
import org.lwjgl.opengl.GL11;

public class ComputerTileEntityRenderer extends TileEntitySpecialRenderer {

    private static final BlockbenchModel MODEL = BlockbenchModel.load(
            "assets/netmusic/models/block/computer.json",
            new ResourceLocation("netmusic:textures/block/computer.png"),
            64,
            64
    );

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        int metadata = tile == null ? 0 : tile.getBlockMetadata();
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        renderModel(metadata, tile);
        GL11.glPopMatrix();
    }

    public static void renderModelAsItem(int metadata) {
        GL11.glPushMatrix();
        // Follow the upstream gui display transform from block/computer.json.
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glTranslatef(0.0F, -0.75F / 16.0F, 0.0F);
        GL11.glRotatef(10.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(145.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.6F, 0.6F, 0.6F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        renderModel(2, null);
        GL11.glPopMatrix();
    }

    private static void renderModel(int metadata, TileEntity tile) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        mc.getTextureManager().bindTexture(MODEL.texture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator t = Tessellator.instance;
        double ox = t.xOffset;
        double oy = t.yOffset;
        double oz = t.zOffset;
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            t.setTranslation(0.0D, 0.0D, 0.0D);
            if (cullEnabled) {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
            if (tile != null && tile.getBlockType() != null && tile.getWorldObj() != null) {
                t.setBrightness(tile.getBlockType().getMixedBrightnessForBlock(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord));
            }
            t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            BlockbenchModelRenderer.render(MODEL, getTurnsFromMetadata(metadata));
        } finally {
            if (cullEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
            t.setTranslation(ox, oy, oz);
        }
    }

    private static int getTurnsFromMetadata(int metadata) {
        switch (metadata & 3) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 0;
            case 3:
                return 1;
            default:
                return 0;
        }
    }
}
