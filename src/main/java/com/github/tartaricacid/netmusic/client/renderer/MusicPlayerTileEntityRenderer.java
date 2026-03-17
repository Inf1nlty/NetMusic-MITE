package com.github.tartaricacid.netmusic.client.renderer;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.github.tartaricacid.netmusic.client.model.ModelMusicPlayerLegacy;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.event.ConfigEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import net.minecraft.ItemStack;
import net.minecraft.Minecraft;
import net.minecraft.RenderManager;
import net.minecraft.TileEntity;
import net.minecraft.TileEntitySpecialRenderer;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;

public class MusicPlayerTileEntityRenderer extends TileEntitySpecialRenderer {
    private static final ModelMusicPlayerLegacy MODEL = new ModelMusicPlayerLegacy();
    private static final net.minecraft.ResourceLocation TEXTURE = new net.minecraft.ResourceLocation("netmusic:textures/blocks/music_player.png");
    private static final float MODEL_SCALE = 0.75F;

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityMusicPlayer te)) {
            return;
        }
        renderPlayerModel(te, x, y, z);
        renderLyric(te, x, y, z);
    }

    public static void renderModelAsItem(int metadata) {
        MODEL.getDiscPart().showModel = true;
        MODEL.getDiscPart().rotateAngleY = 0.0F;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        mc.getTextureManager().bindTexture(TEXTURE);
        GL11.glPushMatrix();
        // Match assets/netmusic/models/item/music_player.json GUI display transform.
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glTranslatef(0.0F, -2.5F / 16.0F, 0.0F);
        GL11.glRotatef(22.5F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(22.5F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        renderModelCore(0.0F);
        GL11.glPopMatrix();
    }

    private void renderPlayerModel(TileEntityMusicPlayer te, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        mc.getTextureManager().bindTexture(TEXTURE);

        ItemStack cd = te.getItem(0);
        boolean hasCd = cd != null;
        MODEL.getDiscPart().showModel = hasCd;
        MODEL.getDiscPart().rotateAngleY = hasCd && te.isPlay()
                ? (float) ((2.0D * Math.PI / 40.0D) * (((double) System.currentTimeMillis() / 50.0D) % 40.0D))
                : 0.0F;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glScalef(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        GL11.glTranslatef(0.5F / MODEL_SCALE, 1.5F, 0.5F / MODEL_SCALE);
        renderModelCore(getYawFromMetadata(te.getBlockMetadata()));
        GL11.glPopMatrix();
    }

    private static void renderModelCore(float yaw) {
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (cullEnabled) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        try {
            MODEL.render(0.0625F);
        } finally {
            if (cullEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        }
    }

    private void renderLyric(TileEntityMusicPlayer te, double x, double y, double z) {
        if (!GeneralConfig.ENABLE_PLAYER_LYRICS) {
            return;
        }
        LyricRecord lyricRecord = te.lyricRecord;
        if (lyricRecord == null) {
            return;
        }
        Int2ObjectSortedMap<String> lyrics = lyricRecord.getLyrics();
        if (lyrics == null || lyrics.isEmpty()) {
            return;
        }
        if (!te.isPlay()) {
            te.lyricRecord = null;
            return;
        }

        String current = lyrics.get(lyrics.firstIntKey());
        if (StringUtils.isBlank(current)) {
            current = "";
        }

        String translated = null;
        int currentLyricColor = ConfigEvent.PLAYER_ORIGINAL_COLOR;
        int transLyricColor = ConfigEvent.PLAYER_TRANSLATED_COLOR;

        Int2ObjectSortedMap<String> transLyrics = lyricRecord.getTransLyrics();
        if (transLyrics != null && !transLyrics.isEmpty()) {
            translated = transLyrics.get(transLyrics.firstIntKey());
            if (StringUtils.isBlank(translated)) {
                translated = null;
            }
        } else {
            // Match upstream: when no translation exists, use the translated color for the only line.
            currentLyricColor = ConfigEvent.PLAYER_TRANSLATED_COLOR;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y + 1.625D, z + 0.5D);

        // Billboard towards camera.
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-0.025F, -0.025F, 0.025F);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Slightly offset to avoid z-fighting when multiple players are around.
        int yOffset = translated == null ? 0 : 6;
        net.minecraft.FontRenderer font = this.getFontRenderer();
        int currentWidth = font.getStringWidth(current);
        font.drawStringWithShadow(current, -(currentWidth / 2), -yOffset, currentLyricColor);

        if (translated != null) {
            int transWidth = font.getStringWidth(translated);
            font.drawStringWithShadow(translated, -(transWidth / 2), -yOffset - 12, transLyricColor);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private static float getYawFromMetadata(int metadata) {
        switch (metadata & 3) {
            case 0:
                return 180.0F;
            case 1:
                return 90.0F;
            case 2:
                return 0.0F;
            case 3:
                return 270.0F;
            default:
                return 0.0F;
        }
    }
}
