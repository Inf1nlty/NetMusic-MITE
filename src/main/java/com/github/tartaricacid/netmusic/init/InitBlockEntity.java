package com.github.tartaricacid.netmusic.init;

import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.xiaoyu233.fml.reload.event.TileEntityRegisterEvent;

public class InitBlockEntity {
    public static void registerTileEntities(TileEntityRegisterEvent event) {
        event.register(TileEntityMusicPlayer.class, "NetMusicMusicPlayer");
    }
}
