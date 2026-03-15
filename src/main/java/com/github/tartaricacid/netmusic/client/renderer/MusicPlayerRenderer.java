package com.github.tartaricacid.netmusic.client.renderer;

import com.github.tartaricacid.netmusic.client.model.ModelMusicPlayer;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;

import java.util.Map;

public class MusicPlayerRenderer {
    public static final MusicPlayerRenderer INSTANCE = new MusicPlayerRenderer();
    private String lastRenderSummary = "";

    public void render(TileEntityMusicPlayer tileEntity) {
        if (tileEntity == null) {
            this.lastRenderSummary = "tile=null";
            return;
        }
        Map<String, float[]> parts = ModelMusicPlayer.getParts();
        if (parts.isEmpty()) {
            ModelMusicPlayer.createBodyLayer();
            parts = ModelMusicPlayer.getParts();
        }
        this.lastRenderSummary = "parts=" + parts.size() + ",play=" + tileEntity.isPlay() + ",time=" + tileEntity.getCurrentTime();
    }

    public void renderMusicPlayer() {
        Map<String, float[]> parts = ModelMusicPlayer.getParts();
        if (parts.isEmpty()) {
            ModelMusicPlayer.createBodyLayer();
            parts = ModelMusicPlayer.getParts();
        }
        this.lastRenderSummary = "item_parts=" + parts.size();
    }

    public String getLastRenderSummary() {
        return this.lastRenderSummary;
    }
}
