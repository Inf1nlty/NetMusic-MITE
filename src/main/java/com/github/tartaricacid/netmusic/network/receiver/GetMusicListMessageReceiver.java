package com.github.tartaricacid.netmusic.network.receiver;

import com.github.tartaricacid.netmusic.network.message.GetMusicListMessage;

public final class GetMusicListMessageReceiver {
    private GetMusicListMessageReceiver() {
    }

    public static void handle(GetMusicListMessage message) {
        if (message != null) {
            // Packet.apply handles client behavior in the low-version pipeline.
        }
    }
}
