package com.github.tartaricacid.netmusic;

import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.api.WebApi;
import com.github.tartaricacid.netmusic.init.CommandRegistry;
import com.github.tartaricacid.netmusic.init.InitContainer;
import com.github.tartaricacid.netmusic.init.InitEvents;
import com.github.tartaricacid.netmusic.init.InitSounds;
import com.github.tartaricacid.netmusic.init.ServerReceiverRegistry;
import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.ModResourceManager;
import net.xiaoyu233.fml.reload.event.MITEEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetMusic implements ModInitializer {
    public static final String MOD_ID = "netmusic";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static WebApi NET_EASE_WEB_API;

    @Override
    public void onInitialize() {
        ModResourceManager.addResourcePackDomain(MOD_ID);
        NET_EASE_WEB_API = new NetEaseMusic().getApi();

        InitEvents.init();
        InitContainer.init();
        InitSounds.init();
        CommandRegistry.registryCommand();
        ServerReceiverRegistry.register();

        MITEEvents.MITE_EVENT_BUS.register(new NetMusicFMLEvents());
    }
}