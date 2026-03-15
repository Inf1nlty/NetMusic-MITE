package com.github.tartaricacid.netmusic.network.receiver;

import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;

public final class MusicToClientMessageReceiver {
    private MusicToClientMessageReceiver() {
    }

    public static void handle(MusicToClientMessage message) {
        if (message != null) {
            // Packet.apply handles client behavior in the low-version pipeline.
        }
    }
}
