package com.github.tartaricacid.netmusic.creativetab;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import huix.glacier.api.extension.creativetab.GlacierCreativeTabs;

public class NetMusicCreativeTab extends GlacierCreativeTabs {

    public static final NetMusicCreativeTab TAB = new NetMusicCreativeTab();

    public NetMusicCreativeTab() {
        super("Net Music");
    }

    public int getTabIconItemIndex() {
        return InitBlocks.MUSIC_PLAYER.blockID;
    }
}
