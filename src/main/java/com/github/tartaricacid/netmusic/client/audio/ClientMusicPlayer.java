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
import java.net.URLConnection;
import java.util.Locale;
import java.util.Random;

public final class ClientMusicPlayer {
    private static final Object LOCK = new Object();

    private static NetMusicSound currentSound;
    private static Thread playThread;
    private static volatile boolean stopRequested;
    private static volatile int playSession;
    private static volatile float dynamicVolume = 1.0F;
    private static volatile boolean gamePaused;
    private static volatile boolean streamStarted;
    private static int missingTileTicks;
    private static int currentTick;
    private static int currentPlaybackSessionId;
    private static String currentSourceId = "";
    private static String pendingSourceId = "";
    private static int pendingX;
    private static int pendingY;
    private static int pendingZ;
    private static long pendingUntilMs;
    private static final Random RANDOM = new Random();

    private ClientMusicPlayer() {}

    public static void play(NetMusicSound sound) {
        play(sound, null, 0);
    }

    public static void play(NetMusicSound sound, String sourceId) {
        play(sound, sourceId, 0);
    }

    public static void play(NetMusicSound sound, String sourceId, int playbackSessionId) {
        if (sound == null) {
            return;
        }
        synchronized (LOCK) {
            stopInternal();
            currentSound = sound;
            currentTick = Math.max(0, sound.getStartTick());
            streamStarted = false;
            dynamicVolume = (float) GeneralConfig.MUSIC_PLAYER_VOLUME;
            gamePaused = false;
            missingTileTicks = 0;
            currentPlaybackSessionId = Math.max(0, playbackSessionId);
            currentSourceId = normalizeSourceId(sourceId, sound);
            stopRequested = false;
            int session = ++playSession;
            clearPendingLocked();
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

    public static boolean isPlaying() {
        synchronized (LOCK) {
            return currentSound != null;
        }
    }

    public static boolean isPlayingAt(int x, int y, int z) {
        synchronized (LOCK) {
            return currentSound != null
                    && currentSound.getX() == x
                    && currentSound.getY() == y
                    && currentSound.getZ() == z;
        }
    }

    public static boolean isPlayingAtSource(int x, int y, int z, String sourceId) {
        String normalized = normalizeSourceId(sourceId, null);
        synchronized (LOCK) {
            if (currentSound == null) {
                return false;
            }
            if (currentSound.getX() != x || currentSound.getY() != y || currentSound.getZ() != z) {
                return false;
            }
            return normalized.equals(currentSourceId);
        }
    }

    public static boolean isPlayingAtSession(int x, int y, int z, int playbackSessionId) {
        int safeSession = Math.max(0, playbackSessionId);
        if (safeSession == 0) {
            return false;
        }
        synchronized (LOCK) {
            if (currentSound == null) {
                return false;
            }
            if (currentSound.getX() != x || currentSound.getY() != y || currentSound.getZ() != z) {
                return false;
            }
            return safeSession == currentPlaybackSessionId;
        }
    }

    public static boolean isPendingAtSource(int x, int y, int z, String sourceId) {
        String normalized = normalizeSourceId(sourceId, null);
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            if (pendingSourceId.isEmpty() || now >= pendingUntilMs) {
                return false;
            }
            if (pendingX != x || pendingY != y || pendingZ != z) {
                return false;
            }
            return normalized.equals(pendingSourceId);
        }
    }

    public static boolean isPlayingOrPendingAtSource(int x, int y, int z, String sourceId) {
        return isPlayingAtSource(x, y, z, sourceId) || isPendingAtSource(x, y, z, sourceId);
    }

    public static void markPendingPlayback(int x, int y, int z, String sourceId, long ttlMs) {
        String normalized = normalizeSourceId(sourceId, null);
        long ttl = Math.max(500L, ttlMs);
        synchronized (LOCK) {
            pendingX = x;
            pendingY = y;
            pendingZ = z;
            pendingSourceId = normalized;
            pendingUntilMs = System.currentTimeMillis() + ttl;
        }
    }

    public static int getCurrentTickAt(int x, int y, int z) {
        synchronized (LOCK) {
            if (currentSound == null) {
                return -1;
            }
            if (currentSound.getX() != x || currentSound.getY() != y || currentSound.getZ() != z) {
                return -1;
            }
            return currentTick;
        }
    }

    public static boolean syncTickAtSource(int x, int y, int z, String sourceId, int targetTick) {
        return syncServerTickAtSource(x, y, z, sourceId, targetTick);
    }

    public static boolean syncServerTickAt(int x, int y, int z, int targetTick) {
        int safeTick = Math.max(0, targetTick);
        synchronized (LOCK) {
            if (currentSound == null) {
                return false;
            }
            if (currentSound.getX() != x || currentSound.getY() != y || currentSound.getZ() != z) {
                return false;
            }
            currentTick = safeTick;
            return true;
        }
    }

    public static boolean syncServerTickAtSource(int x, int y, int z, String sourceId, int targetTick) {
        String normalized = normalizeSourceId(sourceId, null);
        int safeTick = Math.max(0, targetTick);
        synchronized (LOCK) {
            if (currentSound == null) {
                return false;
            }
            if (currentSound.getX() != x || currentSound.getY() != y || currentSound.getZ() != z) {
                return false;
            }
            if (!normalized.equals(currentSourceId)) {
                return false;
            }
            currentTick = safeTick;
            return true;
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
        streamStarted = false;
        missingTileTicks = 0;
        dynamicVolume = 0.0F;
        gamePaused = false;
        currentPlaybackSessionId = 0;
        currentSourceId = "";
        clearPendingLocked();
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
        float maxHearDistance = (float) Math.max(1.0D, GeneralConfig.MUSIC_PLAYER_HEAR_DISTANCE);
        float attenuation = Math.max(0.0F, 1.0F - distance / maxHearDistance);
        dynamicVolume = clampVolume((float) GeneralConfig.MUSIC_PLAYER_VOLUME * attenuation);

        int syncedTick;
        synchronized (LOCK) {
            if (streamStarted) {
                currentTick++;
            }
            syncedTick = currentTick;
        }

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

        LyricRecord lyricRecord = sound.getLyricRecord();

        // Stop when the tile is no longer playing or missing. Prefer server TE time as lyric tick source.
        TileEntity te = mc.theWorld.getBlockTileEntity(sound.getX(), sound.getY(), sound.getZ());
        if (te instanceof TileEntityMusicPlayer musicPlayer) {
            missingTileTicks = 0;
            if (!musicPlayer.isPlay()) {
                stopAndClearTile(mc, sound);
                return;
            }
            syncedTick = TileEntityMusicPlayer.computeStartTick(sound.getTimeSecond(), musicPlayer.getCurrentTime());
            synchronized (LOCK) {
                currentTick = syncedTick;
            }
            if (lyricRecord != null) {
                lyricRecord.updateCurrentLine(syncedTick);
            }
            musicPlayer.lyricRecord = lyricRecord;
        } else {
            // In multiplayer join/teleport transitions, the sync packet can arrive before the chunk TE is client-ready.
            // Keep audio alive for a short grace window and wait for explicit server stop updates.
            missingTileTicks++;
            if (missingTileTicks > 100) {
                stop();
                return;
            }
            if (lyricRecord != null) {
                lyricRecord.updateCurrentLine(syncedTick);
            }
        }

        // Fallback stop: avoid lingering playback if the stream stalls or server missed a stop update.
        int maxTick = Math.max(sound.getTimeSecond(), 1) * 20 + 50;
        if (syncedTick > maxTick) {
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
                        int targetTick = Math.max(0, sound.getStartTick());
                        skipToStartTick(finalPcm, playbackFormat, targetTick);
                        synchronized (LOCK) {
                            if (currentSound == sound && session == playSession) {
                                currentTick = targetTick;
                            }
                        }
                        line.open(playbackFormat);
                        line.start();
                        synchronized (LOCK) {
                            if (currentSound == sound && session == playSession) {
                                streamStarted = true;
                            }
                        }
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
                    streamStarted = false;
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
            if (shouldUseDirectHttpStream(songUrl)) {
                return openDirectHttpStream(songUrl);
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

    private static boolean shouldUseDirectHttpStream(URL songUrl) {
        String host = songUrl.getHost();
        if (host == null) {
            return false;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        return lower.contains("qq.com");
    }

    private static InputStream openDirectHttpStream(URL songUrl) throws Exception {
        URLConnection connection = songUrl.openConnection(NetWorker.getProxyFromConfig());
        connection.setUseCaches(false);
        connection.setConnectTimeout(12000);
        // Keep long-lived VIP streams stable while still allowing timeout-based recovery.
        connection.setReadTimeout(45000);
        applyDirectRequestHeaders(connection, songUrl);
        if (GeneralConfig.ENABLE_DEBUG_MODE) {
            NetMusic.LOGGER.info("[NetMusic Debug] Use direct stream for host={} url={}", songUrl.getHost(), songUrl);
        }
        return connection.getInputStream();
    }

    private static void applyDirectRequestHeaders(URLConnection connection, URL songUrl) {
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "keep-alive");
        String host = songUrl.getHost() == null ? "" : songUrl.getHost().toLowerCase(Locale.ROOT);
        if (host.contains("qq.com")) {
            connection.setRequestProperty("Referer", "https://y.qq.com/");
            connection.setRequestProperty("Origin", "https://y.qq.com");
            if (GeneralConfig.hasQqVipCookie()) {
                connection.setRequestProperty("Cookie", GeneralConfig.QQ_VIP_COOKIE);
            }
            return;
        }
        if (host.contains("music.163.com") && GeneralConfig.hasNeteaseVipCookie()) {
            connection.setRequestProperty("Cookie", GeneralConfig.NETEASE_VIP_COOKIE);
        }
    }

    private static AudioFormat chooseDecodedPcmFormat(AudioFormat source) {
        int channels = Math.max(1, source.getChannels());
        float sampleRate = source.getSampleRate() > 0 ? source.getSampleRate() : 44100.0F;
        float[] sampleRates = new float[]{sampleRate, 48000.0F, 44100.0F};
        int[] candidateBits = new int[]{16, 24, 32};
        for (float rate : sampleRates) {
            if (rate <= 0 || isAlmostDuplicateRate(rate, sampleRate) && rate != sampleRate) {
                continue;
            }
            for (int bits : candidateBits) {
                if (bits <= 0 || bits % 8 != 0) {
                    continue;
                }
                AudioFormat candidate = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, bits,
                        channels, channels * (bits / 8), rate, false);
                if (AudioSystem.isConversionSupported(candidate, source)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isAlmostDuplicateRate(float rateA, float rateB) {
        return Math.abs(rateA - rateB) < 1.0F;
    }

    private static AudioFormat applyStereoConfig(AudioFormat base) {
        // Respect config semantics directly: stereo=true outputs 2 channels, else mono.
        int sampleBits = base.getSampleSizeInBits() > 0 ? base.getSampleSizeInBits() : 16;
        if (com.github.tartaricacid.netmusic.config.GeneralConfig.ENABLE_STEREO) {
            return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), sampleBits, 2, 2 * Math.max(1, sampleBits / 8), base.getSampleRate(), false);
        }
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), sampleBits, 1, Math.max(1, sampleBits / 8), base.getSampleRate(), false);
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

    private static String normalizeSourceId(String sourceId, NetMusicSound sound) {
        String value = sourceId;
        if ((value == null || value.trim().isEmpty()) && sound != null && sound.getSongUrl() != null) {
            value = sound.getSongUrl().toString();
        }
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static void clearPendingLocked() {
        pendingSourceId = "";
        pendingUntilMs = 0L;
        pendingX = 0;
        pendingY = 0;
        pendingZ = 0;
    }

    private static void skipToStartTick(AudioInputStream stream, AudioFormat format, int startTick) throws java.io.IOException {
        if (stream == null || format == null || startTick <= 0) {
            return;
        }
        int frameSize = Math.max(1, format.getFrameSize());
        float frameRate = format.getFrameRate() > 0 ? format.getFrameRate() : format.getSampleRate();
        if (frameRate <= 0) {
            return;
        }
        double seconds = startTick / 20.0D;
        long targetBytes = (long) Math.floor(seconds * frameRate * frameSize);
        if (targetBytes <= 0L) {
            return;
        }
        try {
            skipFully(stream, targetBytes);
        } catch (RuntimeException runtimeException) {
            // Some decoders (notably certain FLAC SPI chains) don't support stable skip semantics.
            // Don't abort playback; just start from the beginning instead of crashing stream setup.
            NetMusic.LOGGER.warn("Failed to seek audio stream to start tick {}, fallback to start-from-beginning", startTick, runtimeException);
        }
    }

    private static void skipFully(InputStream input, long bytes) throws java.io.IOException {
        long remaining = bytes;
        byte[] discard = new byte[4096];
        while (remaining > 0) {
            long skipped;
            try {
                skipped = input.skip(remaining);
            } catch (RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            int read;
            try {
                read = input.read(discard, 0, (int) Math.min(discard.length, remaining));
            } catch (RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (read <= 0) {
                break;
            }
            remaining -= read;
        }
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
