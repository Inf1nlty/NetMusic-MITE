package com.github.tartaricacid.netmusic.init;

import com.github.tartaricacid.netmusic.block.BlockCDBurner;
import com.github.tartaricacid.netmusic.block.BlockComputer;
import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import net.minecraft.Block;
import net.minecraft.Item;
import net.minecraft.ItemBlock;
import net.xiaoyu233.fml.reload.event.BlockRegistryEvent;

public class InitBlocks {
    public static Block MUSIC_PLAYER;
    public static Block CD_BURNER;
    public static Block COMPUTER;

    public static void registerBlocks(BlockRegistryEvent event) {
        MUSIC_PLAYER = new BlockMusicPlayer().setUnlocalizedName("netmusic.music_player");
        CD_BURNER = new BlockCDBurner().setUnlocalizedName("netmusic.cd_burner");
        COMPUTER = new BlockComputer().setUnlocalizedName("netmusic.computer");


        Item.itemsList[MUSIC_PLAYER.blockID] = new ItemBlock(MUSIC_PLAYER);
        event.registerBlock("Net Music Mod", "netmusic:music_player", "music_player", MUSIC_PLAYER);

        Item.itemsList[CD_BURNER.blockID] = new ItemBlock(CD_BURNER);
        event.registerBlock("Net Music Mod", "netmusic:cd_burner", "cd_burner", CD_BURNER);

        Item.itemsList[COMPUTER.blockID] = new ItemBlock(COMPUTER);
        event.registerBlock("Net Music Mod", "netmusic:computer", "computer", COMPUTER);
    }

    public static void init() {
    }
}