package com.github.tartaricacid.netmusic.creativetab;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import huix.glacier.api.extension.creativetab.GlacierCreativeTabs;
import net.minecraft.ItemStack;

import java.util.List;

public class NetMusicCreativeTab extends GlacierCreativeTabs {

    public static final NetMusicCreativeTab TAB = new NetMusicCreativeTab();

    public NetMusicCreativeTab() {
        super("NetMusic");
    }

    public int getTabIconItemIndex() {
        return InitBlocks.MUSIC_PLAYER.blockID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void displayAllReleventItems(List itemList) {
        super.displayAllReleventItems(itemList);

        if (InitItems.MUSIC_CD == null) {
            return;
        }

        for (ItemMusicCD.SongInfo info : BuiltinMusicDiscs.getSongs()) {
            ItemStack stack = new ItemStack(InitItems.MUSIC_CD);
            ItemMusicCD.setSongInfo(info, stack);
            itemList.add(stack);
        }
    }
}
