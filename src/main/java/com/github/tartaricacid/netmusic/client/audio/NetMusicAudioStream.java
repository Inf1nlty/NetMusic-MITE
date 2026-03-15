package com.github.tartaricacid.netmusic.client.audio;

import java.net.URL;

public class NetMusicAudioStream {
    private final URL songUrl;

    public NetMusicAudioStream(URL songUrl) {
        this.songUrl = songUrl;
    }

    public URL getSongUrl() {
        return this.songUrl;
    }
}
