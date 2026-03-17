package com.github.tartaricacid.netmusic.init;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InitSounds {
    public static final String NET_MUSIC = "netmusic.net_music";
    private static final Set<String> REGISTERED = new HashSet<>();

    public static void init() {
        REGISTERED.add(NET_MUSIC);
    }

    public static boolean isRegistered(String soundId) {
        return REGISTERED.contains(soundId);
    }

    public static Set<String> getRegistered() {
        return Collections.unmodifiableSet(REGISTERED);
    }
}
