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

        MUSIC_PLAYER = new BlockMusicPlayer();
        Item.itemsList[MUSIC_PLAYER.blockID] = new ItemBlock(MUSIC_PLAYER);
        event.registerBlock("NetMusic", "netmusic:music_player", "music_player", MUSIC_PLAYER);

        CD_BURNER = new BlockCDBurner();
        Item.itemsList[CD_BURNER.blockID] = new ItemBlock(CD_BURNER);
        event.registerBlock("NetMusic", "netmusic:cd_burner", "cd_burner", CD_BURNER);

        COMPUTER = new BlockComputer();
        Item.itemsList[COMPUTER.blockID] = new ItemBlock(COMPUTER);
        event.registerBlock("NetMusic", "netmusic:computer", "computer", COMPUTER);
    }
}