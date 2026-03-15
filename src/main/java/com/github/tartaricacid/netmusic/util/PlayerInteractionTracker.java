package com.github.tartaricacid.netmusic.util;

import net.minecraft.EntityPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerInteractionTracker {
    private static final Map<String, Context> LAST_CONTEXT = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL_TICKS = 20L * 60L;
    private static long lastCleanupTick;

    private PlayerInteractionTracker() {
    }

    public static void markCdBurner(EntityPlayer player, long worldTick) {
        mark(player, Kind.CD_BURNER, worldTick);
    }

    public static void markComputer(EntityPlayer player, long worldTick) {
        mark(player, Kind.COMPUTER, worldTick);
    }

    public static boolean hasRecentNetMusicInteraction(EntityPlayer player, long worldTick, long maxAgeTicks) {
        cleanup(worldTick, maxAgeTicks);
        if (player == null || maxAgeTicks < 0) {
            return false;
        }
        Context context = LAST_CONTEXT.get(player.getEntityName());
        if (context == null) {
            return false;
        }
        return worldTick - context.tick <= maxAgeTicks;
    }

    public static boolean hasRecentCdWriterInteraction(EntityPlayer player, long worldTick, long maxAgeTicks) {
        cleanup(worldTick, maxAgeTicks);
        if (player == null || maxAgeTicks < 0) {
            return false;
        }
        Context context = LAST_CONTEXT.get(player.getEntityName());
        if (context == null) {
            return false;
        }
        if (worldTick - context.tick > maxAgeTicks) {
            return false;
        }
        return context.kind == Kind.CD_BURNER || context.kind == Kind.COMPUTER;
    }

    private static void mark(EntityPlayer player, Kind kind, long worldTick) {
        if (player == null) {
            return;
        }
        LAST_CONTEXT.put(player.getEntityName(), new Context(kind, worldTick));
    }

    private static void cleanup(long worldTick, long maxAgeTicks) {
        if (worldTick - lastCleanupTick < CLEANUP_INTERVAL_TICKS) {
            return;
        }
        lastCleanupTick = worldTick;
        long keepTicks = Math.max(maxAgeTicks, CLEANUP_INTERVAL_TICKS);
        LAST_CONTEXT.entrySet().removeIf(e -> worldTick - e.getValue().tick > keepTicks);
    }

    private enum Kind {
        CD_BURNER,
        COMPUTER
    }

    private static final class Context {
        private final Kind kind;
        private final long tick;

        private Context(Kind kind, long tick) {
            this.kind = kind;
            this.tick = tick;
        }
    }
}
