package com.github.tartaricacid.netmusic.client;

import com.github.tartaricacid.netmusic.client.init.InitContainerGui;
import com.github.tartaricacid.netmusic.client.init.InitModel;
import com.github.tartaricacid.netmusic.client.init.ClientReceiverRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.xiaoyu233.fml.reload.event.MITEEvents;

public class NetMusicClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InitContainerGui.init();
        InitModel.init();
        ClientReceiverRegistry.register();
        // Register client event listener for tile entity renderers
        MITEEvents.MITE_EVENT_BUS.register(new ClientEventListener());
    }
}