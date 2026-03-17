package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayer;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MusicPlayerStateMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "music_player_state");
    private static final long RECOVERY_RETRY_INTERVAL_MS = 2500L;
    private static final long RECOVERY_CACHE_EXPIRE_MS = 30000L;
    private static final int RECOVERY_CACHE_MAX = 512;
    private static final Map<String, Long> RECOVERY_ATTEMPTS = new HashMap<String, Long>();

    private final int x;
    private final int y;
    private final int z;
    private final boolean play;
    private final int currentTime;
    private final boolean signal;
    private final ItemStack stack;
    private final String songUrl;
    private final int songTime;
    private final String songName;

    public MusicPlayerStateMessage(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readItemStack(),
                readOptionalString(buf), readOptionalInt(buf), readOptionalString(buf));
    }

    public MusicPlayerStateMessage(int x, int y, int z, boolean play, int currentTime, boolean signal, ItemStack stack) {
        this(x, y, z, play, currentTime, signal, stack, "", 0, "");
    }

    public MusicPlayerStateMessage(int x, int y, int z, boolean play, int currentTime, boolean signal, ItemStack stack,
                                   String songUrl, int songTime, String songName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.play = play;
        this.currentTime = currentTime;
        this.signal = signal;
        this.stack = stack == null ? null : stack.copy();
        this.songUrl = songUrl == null ? "" : songUrl;
        this.songTime = Math.max(0, songTime);
        this.songName = songName == null ? "" : songName;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeBoolean(this.play);
        buf.writeInt(this.currentTime);
        buf.writeBoolean(this.signal);
        buf.writeItemStack(this.stack);
        buf.writeString(this.songUrl);
        buf.writeInt(this.songTime);
        buf.writeString(this.songName);
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        if (entityPlayer == null || entityPlayer.worldObj == null || !entityPlayer.worldObj.isRemote) {
            return;
        }
        TileEntity tileEntity = entityPlayer.worldObj.getBlockTileEntity(this.x, this.y, this.z);
        if (tileEntity instanceof TileEntityMusicPlayer musicPlayer) {
            musicPlayer.applyClientSync(this.play, this.currentTime, this.signal, this.stack == null ? null : this.stack.copy());
        }

        if (!this.play || this.songTime <= 0 || StringUtils.isBlank(this.songUrl)) {
            if (ClientMusicPlayer.isPlayingAt(this.x, this.y, this.z)) {
                ClientMusicPlayer.stop();
            }
            return;
        }

        String sourceId = MusicToClientMessage.buildPlaybackSourceId(this.songUrl, this.songTime, this.songName);
        if (ClientMusicPlayer.isPlayingAtSource(this.x, this.y, this.z, sourceId)
                || ClientMusicPlayer.isPendingAtSource(this.x, this.y, this.z, sourceId)) {
            return;
        }

        String recoveryKey = this.x + "," + this.y + "," + this.z + "|" + sourceId;
        long now = System.currentTimeMillis();
        if (!shouldAttemptRecovery(recoveryKey, now)) {
            return;
        }

        int startTick = TileEntityMusicPlayer.computeStartTick(this.songTime, this.currentTime);
        MusicToClientMessage.applyClientPlayback(entityPlayer, this.x, this.y, this.z,
                this.songUrl, this.songTime, this.songName, startTick, false);
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }

    private static boolean shouldAttemptRecovery(String key, long now) {
        Long last = RECOVERY_ATTEMPTS.get(key);
        if (last != null && now - last.longValue() < RECOVERY_RETRY_INTERVAL_MS) {
            return false;
        }
        RECOVERY_ATTEMPTS.put(key, now);
        cleanupRecoveryCache(now);
        return true;
    }

    private static void cleanupRecoveryCache(long now) {
        if (RECOVERY_ATTEMPTS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Long>> iterator = RECOVERY_ATTEMPTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue().longValue() > RECOVERY_CACHE_EXPIRE_MS) {
                iterator.remove();
            }
        }
        if (RECOVERY_ATTEMPTS.size() <= RECOVERY_CACHE_MAX) {
            return;
        }
        Iterator<String> keyIterator = RECOVERY_ATTEMPTS.keySet().iterator();
        while (RECOVERY_ATTEMPTS.size() > RECOVERY_CACHE_MAX && keyIterator.hasNext()) {
            keyIterator.next();
            keyIterator.remove();
        }
    }

    private static String readOptionalString(PacketByteBuf buf) {
        if (!hasReadableBytes(buf, 4)) {
            return "";
        }
        try {
            return buf.readString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int readOptionalInt(PacketByteBuf buf) {
        if (!hasReadableBytes(buf, 4)) {
            return 0;
        }
        try {
            return buf.readInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean hasReadableBytes(PacketByteBuf buf, int bytes) {
        try {
            return buf.getInputStream() != null && buf.getInputStream().available() >= bytes;
        } catch (Exception ignored) {
            return false;
        }
    }
}
