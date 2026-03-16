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

    public static void markCdBurner(EntityPlayer player, long worldTick, int x, int y, int z) {
        mark(player, Kind.CD_BURNER, worldTick, x, y, z);
    }

    public static void markComputer(EntityPlayer player, long worldTick, int x, int y, int z) {
        mark(player, Kind.COMPUTER, worldTick, x, y, z);
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
        return hasRecentInteraction(player, worldTick, maxAgeTicks, Kind.CD_BURNER, Kind.COMPUTER);
    }

    public static boolean hasRecentCDBurnerInteraction(EntityPlayer player, long worldTick, long maxAgeTicks) {
        return hasRecentInteraction(player, worldTick, maxAgeTicks, Kind.CD_BURNER);
    }

    public static boolean hasRecentComputerInteraction(EntityPlayer player, long worldTick, long maxAgeTicks) {
        return hasRecentInteraction(player, worldTick, maxAgeTicks, Kind.COMPUTER);
    }

    public static String getRecentInteractionText(EntityPlayer player, long worldTick, long maxAgeTicks) {
        InteractionView view = getRecentInteractionView(player, worldTick, maxAgeTicks);
        if (view == null) {
            return "none";
        }
        return view.kind.name().toLowerCase() + " @(" + view.x + "," + view.y + "," + view.z + ") age=" + view.ageTicks + "t";
    }

    public static InteractionView getRecentInteractionView(EntityPlayer player, long worldTick, long maxAgeTicks) {
        cleanup(worldTick, maxAgeTicks);
        if (player == null || maxAgeTicks < 0) {
            return null;
        }
        Context context = LAST_CONTEXT.get(player.getEntityName());
        if (context == null || worldTick - context.tick > maxAgeTicks) {
            return null;
        }
        return new InteractionView(context.kind, context.x, context.y, context.z, Math.max(0L, worldTick - context.tick));
    }

    private static boolean hasRecentInteraction(EntityPlayer player, long worldTick, long maxAgeTicks, Kind... allowed) {
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
        for (Kind kind : allowed) {
            if (context.kind == kind) {
                return true;
            }
        }
        return false;
    }

    private static void mark(EntityPlayer player, Kind kind, long worldTick, int x, int y, int z) {
        if (player == null) {
            return;
        }
        LAST_CONTEXT.put(player.getEntityName(), new Context(kind, worldTick, x, y, z));
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
        private final int x;
        private final int y;
        private final int z;

        private Context(Kind kind, long tick, int x, int y, int z) {
            this.kind = kind;
            this.tick = tick;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final class InteractionView {
        public final String kindKey;
        public final int x;
        public final int y;
        public final int z;
        public final long ageTicks;
        private final Kind kind;

        private InteractionView(Kind kind, int x, int y, int z, long ageTicks) {
            this.kind = kind;
            this.kindKey = kind == Kind.CD_BURNER ? "command.netmusic.source.cd_burner" : "command.netmusic.source.computer";
            this.x = x;
            this.y = y;
            this.z = z;
            this.ageTicks = ageTicks;
        }
    }
}
