package com.github.tartaricacid.netmusic.network.receiver;

import com.github.tartaricacid.netmusic.network.message.GetMusicListMessage;
import net.minecraft.Minecraft;

public final class GetMusicListMessageReceiver {
    private GetMusicListMessageReceiver() {
    }

    public static void handle(GetMusicListMessage message) {
        if (message == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.thePlayer != null) {
            message.apply(minecraft.thePlayer);
        }
    }
}
