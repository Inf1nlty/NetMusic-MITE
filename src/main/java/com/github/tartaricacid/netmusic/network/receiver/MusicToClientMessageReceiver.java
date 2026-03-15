package com.github.tartaricacid.netmusic.network.receiver;

import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;
import net.minecraft.Minecraft;

public final class MusicToClientMessageReceiver {
    private MusicToClientMessageReceiver() {
    }

    public static void handle(MusicToClientMessage message) {
        if (message == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.thePlayer != null) {
            message.apply(minecraft.thePlayer);
        }
    }
}
