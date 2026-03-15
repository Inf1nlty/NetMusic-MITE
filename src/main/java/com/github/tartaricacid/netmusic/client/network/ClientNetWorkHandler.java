package com.github.tartaricacid.netmusic.client.network;

import com.github.tartaricacid.netmusic.network.message.Message;
import moddedmite.rustedironcore.network.Network;

public class ClientNetWorkHandler {

    public static void sendToServer(Message message) {
        Network.sendToServer(message);
    }
}