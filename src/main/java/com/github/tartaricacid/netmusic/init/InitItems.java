package com.github.tartaricacid.netmusic.init;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.Item;
import net.xiaoyu233.fml.reload.event.ItemRegistryEvent;

public class InitItems {
    public static Item MUSIC_CD;
    public static Item MUSIC_PLAYER;
    public static Item CD_BURNER;
    public static Item COMPUTER;

    public static void registerItems(ItemRegistryEvent event) {
        MUSIC_CD = new ItemMusicCD();
        event.register("Net Music Mod", "netmusic:music_cd", "music_cd", MUSIC_CD);

        event.registerItemBlock("Net Music Mod", "netmusic:music_player", "music_player", InitBlocks.MUSIC_PLAYER);
        MUSIC_PLAYER = Item.itemsList[InitBlocks.MUSIC_PLAYER.blockID];

        event.registerItemBlock("Net Music Mod", "netmusic:cd_burner", "cd_burner", InitBlocks.CD_BURNER);
        CD_BURNER = Item.itemsList[InitBlocks.CD_BURNER.blockID];

        event.registerItemBlock("Net Music Mod", "netmusic:computer", "computer", InitBlocks.COMPUTER);
        COMPUTER = Item.itemsList[InitBlocks.COMPUTER.blockID];

    }

    public static void init() {
    }
}