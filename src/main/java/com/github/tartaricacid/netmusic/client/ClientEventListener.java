package com.github.tartaricacid.netmusic.client;

import com.github.tartaricacid.netmusic.client.renderer.CDBurnerTileEntityRenderer;
import com.github.tartaricacid.netmusic.tileentity.TileEntityCDBurner;
import com.google.common.eventbus.Subscribe;
import net.xiaoyu233.fml.reload.event.TileEntityRendererRegisterEvent;

public class ClientEventListener {

    @Subscribe
    public void onTileEntityRendererRegister(TileEntityRendererRegisterEvent event) {
        event.register(TileEntityCDBurner.class, new CDBurnerTileEntityRenderer());
    }
}
