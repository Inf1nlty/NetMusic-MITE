package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetWorker;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;

public final class ClientMusicPlayer {
    private static final Object LOCK = new Object();

    private static NetMusicSound currentSound;
    private static Thread playThread;
    private static volatile boolean stopRequested;
    private static volatile int playSession;

    private ClientMusicPlayer() {
    }

    public static void play(NetMusicSound sound) {
        if (sound == null) {
            return;
        }
        synchronized (LOCK) {
            stopInternal();
            currentSound = sound;
            stopRequested = false;
            int session = ++playSession;
            playThread = new Thread(() -> stream(sound, session), "NetMusic-Player");
            playThread.setDaemon(true);
            playThread.start();
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            stopInternal();
        }
    }

    private static void stopInternal() {
        stopRequested = true;
        if (playThread != null) {
            playThread.interrupt();
            playThread = null;
        }
        currentSound = null;
    }

    private static void stream(NetMusicSound sound, int session) {
        long timeoutAt = System.currentTimeMillis() + Math.max(sound.getTimeSecond(), 1) * 1000L + 3000L;
        try (InputStream remote = new MusicBufferedInputStream(new ChunkedAudioStream(sound.getSongUrl(), NetWorker.getProxyFromConfig()));
             AudioInputStream compressed = AudioSystem.getAudioInputStream(remote)) {
            AudioFormat base = compressed.getFormat();
            AudioFormat decoded = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(), 16, base.getChannels(), base.getChannels() * 2,
                    base.getSampleRate(), false);

            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(decoded, compressed)) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decoded);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(decoded);
                    line.start();
                    byte[] buffer = new byte[8192];
                    int read;
                    while (session == playSession && !stopRequested && !Thread.currentThread().isInterrupted()
                            && System.currentTimeMillis() < timeoutAt
                            && (read = pcm.read(buffer, 0, buffer.length)) != -1) {
                        line.write(buffer, 0, read);
                    }
                    line.drain();
                }
            }
        } catch (Exception e) {
            if (!stopRequested) {
                NetMusic.LOGGER.error("Failed to stream music: {}", sound.getSongUrl(), e);
            }
        } finally {
            synchronized (LOCK) {
                if (currentSound == sound && session == playSession) {
                    currentSound = null;
                    playThread = null;
                }
            }
        }
    }
}
