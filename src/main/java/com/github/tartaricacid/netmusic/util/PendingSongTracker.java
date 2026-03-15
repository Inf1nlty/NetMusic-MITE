package com.github.tartaricacid.netmusic.util;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.EntityPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingSongTracker {
    private static final Map<String, PendingSong> PENDING = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL_TICKS = 20L * 60L;
    private static long lastCleanupTick;

    private PendingSongTracker() {
    }

    public static void setPending(EntityPlayer player, Source source, ItemMusicCD.SongInfo songInfo, long worldTick) {
        if (player == null || source == null || songInfo == null) {
            return;
        }
        PENDING.put(player.getEntityName(), new PendingSong(source, cloneSongInfo(songInfo), worldTick));
    }

    public static PendingSong getPending(EntityPlayer player, long worldTick, long maxAgeTicks) {
        cleanup(worldTick, maxAgeTicks);
        if (player == null || maxAgeTicks < 0) {
            return null;
        }
        PendingSong pending = PENDING.get(player.getEntityName());
        if (pending == null) {
            return null;
        }
        if (worldTick - pending.tick > maxAgeTicks) {
            PENDING.remove(player.getEntityName());
            return null;
        }
        return pending;
    }

    public static PendingSong getPendingForSource(EntityPlayer player, Source source, long worldTick, long maxAgeTicks) {
        PendingSong pending = getPending(player, worldTick, maxAgeTicks);
        if (pending == null || pending.source != source) {
            return null;
        }
        return pending;
    }

    public static void clear(EntityPlayer player) {
        if (player != null) {
            PENDING.remove(player.getEntityName());
        }
    }

    public static String getPendingText(EntityPlayer player, long worldTick, long maxAgeTicks) {
        PendingSong pending = getPending(player, worldTick, maxAgeTicks);
        if (pending == null) {
            return "none";
        }
        long age = Math.max(0L, worldTick - pending.tick);
        return pending.source.name().toLowerCase() + ":" + safeName(pending.songInfo.songName) + " age=" + age + "t";
    }

    private static String safeName(String songName) {
        if (songName == null || songName.trim().isEmpty()) {
            return "unknown";
        }
        return songName.trim();
    }

    private static ItemMusicCD.SongInfo cloneSongInfo(ItemMusicCD.SongInfo songInfo) {
        ItemMusicCD.SongInfo copy = new ItemMusicCD.SongInfo();
        copy.songUrl = songInfo.songUrl;
        copy.songName = songInfo.songName;
        copy.songTime = songInfo.songTime;
        copy.transName = songInfo.transName;
        copy.vip = songInfo.vip;
        copy.readOnly = songInfo.readOnly;
        copy.artists.clear();
        if (songInfo.artists != null) {
            copy.artists.addAll(songInfo.artists);
        }
        return copy;
    }

    private static void cleanup(long worldTick, long maxAgeTicks) {
        if (worldTick - lastCleanupTick < CLEANUP_INTERVAL_TICKS) {
            return;
        }
        lastCleanupTick = worldTick;
        long keepTicks = Math.max(maxAgeTicks, CLEANUP_INTERVAL_TICKS);
        PENDING.entrySet().removeIf(e -> worldTick - e.getValue().tick > keepTicks);
    }

    public enum Source {
        CD_BURNER,
        COMPUTER
    }

    public static final class PendingSong {
        public final Source source;
        public final ItemMusicCD.SongInfo songInfo;
        public final long tick;

        private PendingSong(Source source, ItemMusicCD.SongInfo songInfo, long tick) {
            this.source = source;
            this.songInfo = songInfo;
            this.tick = tick;
        }
    }
}
