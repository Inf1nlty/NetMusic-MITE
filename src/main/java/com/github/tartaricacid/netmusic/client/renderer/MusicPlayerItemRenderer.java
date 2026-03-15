package com.github.tartaricacid.netmusic.client.renderer;

public class MusicPlayerItemRenderer {
    private int renderCount;

    public void render() {
        this.renderCount++;
        MusicPlayerRenderer.INSTANCE.renderMusicPlayer();
    }

    public int getRenderCount() {
        return this.renderCount;
    }
}