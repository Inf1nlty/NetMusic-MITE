package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.minecraft.Minecraft;
import net.minecraft.TileEntity;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

public final class ClientMusicPlayer {
    private static final Object LOCK = new Object();

    private static NetMusicSound currentSound;
    private static Thread playThread;
    private static volatile boolean stopRequested;
    private static volatile int playSession;
    private static volatile float dynamicVolume = 1.0F;
    private static volatile boolean gamePaused;
    private static int currentTick;
    private static final Random RANDOM = new Random();
    private static final float MAX_HEAR_DISTANCE = 32.0F;

    private ClientMusicPlayer() {
    }

    public static void play(NetMusicSound sound) {
        if (sound == null) {
            return;
        }
        synchronized (LOCK) {
            stopInternal();
            currentSound = sound;
            currentTick = 0;
            dynamicVolume = (float) GeneralConfig.MUSIC_PLAYER_VOLUME;
            gamePaused = false;
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
        currentTick = 0;
        dynamicVolume = 0.0F;
        gamePaused = false;
    }

    /**
     * Runs on the client thread once per tick (hooked via {@link com.github.tartaricacid.netmusic.mixin.MinecraftMixin}).
     * Keeps lyric progress, stop conditions, and client-side effects in sync with the original 1.20.1 behavior.
     */
    public static void clientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            stop();
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            stop();
            return;
        }

        NetMusicSound sound;
        synchronized (LOCK) {
            sound = currentSound;
        }
        if (sound == null) {
            return;
        }

        if (mc.isGamePaused) {
            gamePaused = true;
            return;
        }
        gamePaused = false;

        // Distance attenuation only: don't hard-stop when out of range, so returning to range resumes audio.
        double dx = mc.thePlayer.posX - (sound.getX() + 0.5D);
        double dy = mc.thePlayer.posY - (sound.getY() + 0.5D);
        double dz = mc.thePlayer.posZ - (sound.getZ() + 0.5D);
        double distSq = dx * dx + dy * dy + dz * dz;
        float distance = (float) Math.sqrt(distSq);
        float attenuation = Math.max(0.0F, 1.0F - distance / MAX_HEAR_DISTANCE);
        dynamicVolume = clampVolume((float) GeneralConfig.MUSIC_PLAYER_VOLUME * attenuation);

        currentTick++;

        // Particle effects: spawn note particles periodically while playing.
        if (attenuation > 0.0F && mc.theWorld.getTotalWorldTime() % 8L == 0L) {
            for (int i = 0; i < 2; i++) {
                mc.theWorld.spawnParticle(net.minecraft.EnumParticle.note,
                        sound.getX() + RANDOM.nextDouble(),
                        sound.getY() + 1.0D + RANDOM.nextDouble(),
                        sound.getZ() + RANDOM.nextDouble(),
                        RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextInt(3));
            }
        }

        // Update lyric line based on current tick.
        LyricRecord lyricRecord = sound.getLyricRecord();
        if (lyricRecord != null) {
            lyricRecord.updateCurrentLine(currentTick);
        }

        // Stop when the tile is no longer playing or missing.
        TileEntity te = mc.theWorld.getBlockTileEntity(sound.getX(), sound.getY(), sound.getZ());
        if (te instanceof TileEntityMusicPlayer musicPlayer) {
            if (!musicPlayer.isPlay()) {
                stopAndClearTile(mc, sound);
                return;
            }
            musicPlayer.lyricRecord = lyricRecord;
        } else {
            stopAndClearTile(mc, sound);
            return;
        }

        // Fallback stop: avoid lingering playback if the stream stalls or server missed a stop update.
        int maxTick = Math.max(sound.getTimeSecond(), 1) * 20 + 50;
        if (currentTick > maxTick) {
            stopAndClearTile(mc, sound);
        }
    }

    private static void stopAndClearTile(Minecraft mc, NetMusicSound sound) {
        TileEntity te = mc.theWorld.getBlockTileEntity(sound.getX(), sound.getY(), sound.getZ());
        if (te instanceof TileEntityMusicPlayer musicPlayer) {
            musicPlayer.lyricRecord = null;
        }
        stop();
    }

    private static void stream(NetMusicSound sound, int session) {
        long timeoutAt = System.currentTimeMillis() + Math.max(sound.getTimeSecond(), 1) * 1000L + 3000L;
        InputStream source = createSourceStream(sound.getSongUrl());
        if (source == null) {
            return;
        }
        try (InputStream remote = new MusicBufferedInputStream(source);
             InputStream prepared = prepareAudioStream(remote);
             AudioInputStream compressed = AudioSystem.getAudioInputStream(prepared)) {
            AudioFormat base = compressed.getFormat();
            AudioFormat decoded = chooseDecodedPcmFormat(base);
            if (decoded == null) {
                throw new IllegalArgumentException("Unsupported conversion from " + base + " to PCM");
            }

            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(decoded, compressed)) {
                AudioFormat finalFormat = applyStereoConfig(decoded);
                AudioFormat pcmFormat = pcm.getFormat();
                AudioFormat playbackFormat = AudioSystem.isConversionSupported(finalFormat, pcmFormat) ? finalFormat : pcmFormat;
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, playbackFormat);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    try (AudioInputStream finalPcm = AudioSystem.getAudioInputStream(playbackFormat, pcm)) {
                        line.open(playbackFormat);
                        line.start();
                        byte[] buffer = new byte[8192];
                        int read;
                        boolean paused = false;
                        while (session == playSession && !stopRequested && !Thread.currentThread().isInterrupted()
                                && System.currentTimeMillis() < timeoutAt) {
                            if (gamePaused) {
                                if (!paused) {
                                    line.stop();
                                    paused = true;
                                }
                                try {
                                    Thread.sleep(50L);
                                } catch (InterruptedException interruptedException) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                continue;
                            } else if (paused) {
                                line.start();
                                paused = false;
                            }
                            read = finalPcm.read(buffer, 0, buffer.length);
                            if (read == -1) {
                                break;
                            }
                            applyPcmVolume(buffer, read, dynamicVolume, playbackFormat.getSampleSizeInBits(), playbackFormat.isBigEndian());
                            line.write(buffer, 0, read);
                        }
                        line.drain();
                    }
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
                    currentTick = 0;
                }
            }
        }
    }

    private static InputStream createSourceStream(URL songUrl) {
        if (songUrl == null) {
            return null;
        }
        try {
            if ("file".equalsIgnoreCase(songUrl.getProtocol())) {
                return new FileInputStream(new java.io.File(songUrl.toURI()));
            }
            return new ChunkedAudioStream(songUrl, NetWorker.getProxyFromConfig());
        } catch (Exception e) {
            NetMusic.LOGGER.error("Failed to open audio source: {}", songUrl, e);
            return null;
        }
    }

    private static InputStream prepareAudioStream(InputStream input) {
        try {
            skipID3(input);
        } catch (Exception ignored) {
            // Best-effort: ID3 skipping is only needed for some MP3 streams.
        }
        return input;
    }

    private static AudioFormat chooseDecodedPcmFormat(AudioFormat source) {
        int channels = Math.max(1, source.getChannels());
        float sampleRate = source.getSampleRate() > 0 ? source.getSampleRate() : 44100.0F;

        int sourceBits = source.getSampleSizeInBits();
        int[] candidateBits = sourceBits > 16 ? new int[]{24, 16, 32} : new int[]{16, 24, 32};
        for (int bits : candidateBits) {
            if (bits <= 0 || bits % 8 != 0) {
                continue;
            }
            AudioFormat candidate = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, bits,
                    channels, channels * (bits / 8), sampleRate, false);
            if (AudioSystem.isConversionSupported(candidate, source)) {
                return candidate;
            }
        }
        return null;
    }

    private static AudioFormat applyStereoConfig(AudioFormat base) {
        // Preserve the original mod's behavior: if stereo is enabled, force 1 channel, else 2 channels.
        // This looks inverted but matches upstream logic and avoids subtle regressions.
        int sampleBits = base.getSampleSizeInBits() > 0 ? base.getSampleSizeInBits() : 16;
        if (com.github.tartaricacid.netmusic.config.GeneralConfig.ENABLE_STEREO) {
            return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), sampleBits, 1, Math.max(1, sampleBits / 8), base.getSampleRate(), false);
        }
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), sampleBits, 2, 2 * Math.max(1, sampleBits / 8), base.getSampleRate(), false);
    }

    private static void applyPcmVolume(byte[] buffer, int length, float volume, int sampleBits, boolean bigEndian) {
        if (buffer == null || length <= 1) {
            return;
        }
        float clampedVolume = clampVolume(volume);
        if (Math.abs(clampedVolume - 1.0F) < 1.0e-4F) {
            return;
        }
        if (sampleBits == 16) {
            applyVolume16(buffer, length, clampedVolume, bigEndian);
            return;
        }
        if (sampleBits == 24) {
            applyVolume24(buffer, length, clampedVolume, bigEndian);
        }
    }

    private static void applyVolume16(byte[] buffer, int length, float volume, boolean bigEndian) {
        for (int i = 0; i + 1 < length; i += 2) {
            int sample;
            if (bigEndian) {
                int hi = buffer[i];
                int lo = buffer[i + 1] & 0xFF;
                sample = (short) ((hi << 8) | lo);
            } else {
                int lo = buffer[i] & 0xFF;
                int hi = buffer[i + 1];
                sample = (short) ((hi << 8) | lo);
            }
            int scaled = Math.round(sample * volume);
            if (scaled > Short.MAX_VALUE) {
                scaled = Short.MAX_VALUE;
            } else if (scaled < Short.MIN_VALUE) {
                scaled = Short.MIN_VALUE;
            }
            if (bigEndian) {
                buffer[i] = (byte) ((scaled >> 8) & 0xFF);
                buffer[i + 1] = (byte) (scaled & 0xFF);
            } else {
                buffer[i] = (byte) (scaled & 0xFF);
                buffer[i + 1] = (byte) ((scaled >> 8) & 0xFF);
            }
        }
    }

    private static void applyVolume24(byte[] buffer, int length, float volume, boolean bigEndian) {
        for (int i = 0; i + 2 < length; i += 3) {
            int sample;
            if (bigEndian) {
                sample = ((buffer[i] & 0xFF) << 16)
                        | ((buffer[i + 1] & 0xFF) << 8)
                        | (buffer[i + 2] & 0xFF);
            } else {
                sample = (buffer[i] & 0xFF)
                        | ((buffer[i + 1] & 0xFF) << 8)
                        | ((buffer[i + 2] & 0xFF) << 16);
            }
            if ((sample & 0x800000) != 0) {
                sample |= 0xFF000000;
            }

            int scaled = Math.round(sample * volume);
            if (scaled > 0x7FFFFF) {
                scaled = 0x7FFFFF;
            } else if (scaled < -0x800000) {
                scaled = -0x800000;
            }

            if (bigEndian) {
                buffer[i] = (byte) ((scaled >> 16) & 0xFF);
                buffer[i + 1] = (byte) ((scaled >> 8) & 0xFF);
                buffer[i + 2] = (byte) (scaled & 0xFF);
            } else {
                buffer[i] = (byte) (scaled & 0xFF);
                buffer[i + 1] = (byte) ((scaled >> 8) & 0xFF);
                buffer[i + 2] = (byte) ((scaled >> 16) & 0xFF);
            }
        }
    }

    private static float clampVolume(float volume) {
        if (volume < 0.0F) {
            return 0.0F;
        }
        if (volume > 2.0F) {
            return 2.0F;
        }
        return volume;
    }

    private static void skipID3(InputStream inputStream) throws java.io.IOException {
        if (!inputStream.markSupported()) {
            return;
        }
        inputStream.mark(10);
        byte[] header = new byte[10];
        int read = inputStream.read(header, 0, 10);
        if (read < 10) {
            inputStream.reset();
            return;
        }
        if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
            int size = ((header[6] & 0x7F) << 21)
                    | ((header[7] & 0x7F) << 14)
                    | ((header[8] & 0x7F) << 7)
                    | (header[9] & 0x7F);
            int skipped = 0;
            int skip;
            do {
                skip = (int) inputStream.skip(size - skipped);
                if (skip != 0) {
                    skipped += skip;
                }
            } while (skipped < size && skip != 0);
        } else {
            inputStream.reset();
        }
    }
}
