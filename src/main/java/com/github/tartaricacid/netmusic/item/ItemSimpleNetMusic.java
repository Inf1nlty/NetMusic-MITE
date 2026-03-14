package com.github.tartaricacid.netmusic.item;

import net.minecraft.Item;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public class ItemSimpleNetMusic extends Item {
    public ItemSimpleNetMusic(String texture) {
        super(IdUtil.getNextItemID(), texture);
        this.setUnlocalizedName("netmusic." + texture);
    }
}

