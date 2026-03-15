package com.github.tartaricacid.netmusic.compat.tlm.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class NetworkInit {

    @Environment(EnvType.CLIENT)
    public static void clientInit() {
        // MITE low version: maid networking is removed.
    }

    public static void serverInit() {
    }
}