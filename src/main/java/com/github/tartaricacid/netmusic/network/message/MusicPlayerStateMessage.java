package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayer;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ItemStack;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class MusicPlayerStateMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "music_player_state");
    private static final long RECOVERY_COOLDOWN_MS = 1500L;
    private static final Map<String, Long> RECOVERY_COOLDOWN = new HashMap<String, Long>();

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
            recoverPlaybackIfNeeded(entityPlayer);
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }

    private void recoverPlaybackIfNeeded(EntityPlayer entityPlayer) {
        if (!this.play) {
            return;
        }
        if (ClientMusicPlayer.isPlayingAt(this.x, this.y, this.z) || ClientMusicPlayer.isPlaying()) {
            return;
        }
        ItemMusicCD.SongInfo info = resolveSongInfoFromState();
        if (info == null || info.songTime <= 0 || StringUtils.isBlank(info.songUrl) || !allowRecovery(info.songUrl)) {
            return;
        }
        int totalTicks = Math.max(1, info.songTime) * 20;
        int remainingMusicTicks = Math.max(0, this.currentTime - 64);
        int startTick = Math.max(0, Math.min(totalTicks, totalTicks - remainingMusicTicks));
        new MusicToClientMessage(this.x, this.y, this.z, info.songUrl, info.songTime, info.songName, startTick).apply(entityPlayer);
    }

    private ItemMusicCD.SongInfo resolveSongInfoFromState() {
        if (StringUtils.isNotBlank(this.songUrl) && this.songTime > 0) {
            ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo();
            info.songUrl = this.songUrl;
            info.songTime = this.songTime;
            info.songName = StringUtils.isBlank(this.songName) ? this.songUrl : this.songName;
            return info;
        }
        if (this.stack == null) {
            return null;
        }
        return ItemMusicCD.getSongInfo(this.stack);
    }

    private boolean allowRecovery(String songUrl) {
        long now = System.currentTimeMillis();
        String key = this.x + "," + this.y + "," + this.z + "|" + (songUrl == null ? "" : songUrl);
        Long next = RECOVERY_COOLDOWN.get(key);
        if (next != null && now < next.longValue()) {
            return false;
        }
        RECOVERY_COOLDOWN.put(key, Long.valueOf(now + RECOVERY_COOLDOWN_MS));
        return true;
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
