package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.util.MenuSongWriter;
import com.github.tartaricacid.netmusic.util.MusicCdWriteHelper;
import com.github.tartaricacid.netmusic.util.PendingSongTracker;
import com.github.tartaricacid.netmusic.util.PlayerInteractionTracker;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import com.github.tartaricacid.netmusic.util.SongInfoHelper;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

public class SetMusicIDMessage implements Message {
    public final static ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "set_music_id");
    private static final int MAX_ARTIST_COUNT = 32;
    private static final long INTERACTION_WINDOW_TICKS = 20L * 30L;
    private final Source source;
    private final ItemMusicCD.SongInfo song;

    public enum Source {
        CD_BURNER,
        COMPUTER;

        public static Source fromOrdinal(int ordinal) {
            Source[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return CD_BURNER;
            }
            return values[ordinal];
        }
    }

    public SetMusicIDMessage(PacketByteBuf packetByteBuf) {
        this(Source.fromOrdinal(packetByteBuf.readInt()), readSongInfo(packetByteBuf));
    }

    public SetMusicIDMessage(Source source, ItemMusicCD.SongInfo song) {
        this.source = source == null ? Source.CD_BURNER : source;
        this.song = sanitizeSong(song);
    }

    public ResourceLocation getChannel() {
        return ID;
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeInt(this.source.ordinal());
        writeSongInfo(packetByteBuf, this.song == null ? new ItemMusicCD.SongInfo() : this.song);
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        if (entityPlayer == null || this.song == null) {
            return;
        }
        PendingSongTracker.Source trackerSource = this.source == Source.COMPUTER
                ? PendingSongTracker.Source.COMPUTER
                : PendingSongTracker.Source.CD_BURNER;

        ItemMusicCD.SongInfo copiedSong = SongInfoHelper.copy(this.song);
        MenuSongWriter.WriteResult result = MenuSongWriter.tryWriteToSourceMenu(entityPlayer, trackerSource, copiedSong);
        if (result.isSuccess()) {
            PendingSongTracker.clear(entityPlayer);
            return;
        }
        if (result.isFailure()) {
            entityPlayer.addChatMessage(ScreenSubmitResult.resolveFeedbackKey(result.failureKey, "message.netmusic.music_cd.need_writable_cd"));
            return;
        }

        long now = entityPlayer.worldObj == null ? 0L : entityPlayer.worldObj.getTotalWorldTime();
        boolean hasInteraction = trackerSource == PendingSongTracker.Source.CD_BURNER
                ? PlayerInteractionTracker.hasRecentCDBurnerInteraction(entityPlayer, now, INTERACTION_WINDOW_TICKS)
                : PlayerInteractionTracker.hasRecentComputerInteraction(entityPlayer, now, INTERACTION_WINDOW_TICKS);
        if (!hasInteraction) {
            entityPlayer.addChatMessage(trackerSource == PendingSongTracker.Source.CD_BURNER
                    ? "command.netmusic.music_cd.need_cd_burner"
                    : "command.netmusic.music_cd.need_computer");
            return;
        }

        if (!MusicCdWriteHelper.writeSongToPlayerCd(entityPlayer, copiedSong)) {
            entityPlayer.addChatMessage("message.netmusic.music_cd.need_writable_cd");
            return;
        }

        PendingSongTracker.clear(entityPlayer);
        entityPlayer.addChatMessage(trackerSource == PendingSongTracker.Source.CD_BURNER
                ? "message.netmusic.cd_burner.applied"
                : "message.netmusic.computer.applied");
    }

    private static void writeSongInfo(PacketByteBuf buf, ItemMusicCD.SongInfo song) {
        buf.writeString(song.songUrl == null ? "" : song.songUrl);
        buf.writeString(song.songName == null ? "" : song.songName);
        buf.writeInt(song.songTime);
        buf.writeString(song.transName == null ? "" : song.transName);
        buf.writeBoolean(song.vip);
        buf.writeBoolean(song.readOnly);
        int size = song.artists == null ? 0 : song.artists.size();
        size = Math.min(size, MAX_ARTIST_COUNT);
        buf.writeInt(size);
        for (int i = 0; i < size; i++) {
            String artist = song.artists.get(i);
            buf.writeString(artist == null ? "" : artist);
        }
    }

    private static ItemMusicCD.SongInfo readSongInfo(PacketByteBuf buf) {
        ItemMusicCD.SongInfo info = new ItemMusicCD.SongInfo();
        info.songUrl = buf.readString();
        info.songName = buf.readString();
        info.songTime = buf.readInt();
        info.transName = buf.readString();
        info.vip = buf.readBoolean();
        info.readOnly = buf.readBoolean();
        int size = Math.max(0, Math.min(MAX_ARTIST_COUNT, buf.readInt()));
        info.artists.clear();
        for (int i = 0; i < size; i++) {
            info.artists.add(buf.readString());
        }
        return sanitizeSong(info);
    }

    private static ItemMusicCD.SongInfo sanitizeSong(ItemMusicCD.SongInfo input) {
        return SongInfoHelper.sanitize(input);
    }
}