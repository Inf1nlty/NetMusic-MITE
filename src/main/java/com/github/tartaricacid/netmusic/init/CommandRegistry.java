package com.github.tartaricacid.netmusic.init;

import com.github.tartaricacid.netmusic.command.NetMusicCommand;
import moddedmite.rustedironcore.api.event.Handlers;

public class CommandRegistry {
    public static void registryCommand() {
        Handlers.Command.register(event -> event.register(new NetMusicCommand()));
    }
}
