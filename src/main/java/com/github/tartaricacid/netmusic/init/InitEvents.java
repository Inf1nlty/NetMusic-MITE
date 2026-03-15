package com.github.tartaricacid.netmusic.init;

import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.event.ConfigEvent;

public class InitEvents {
    public static void init() {
        GeneralConfig.init();
        ConfigEvent.onConfigLoading();
    }
}
