package com.github.tartaricacid.netmusic.command;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.config.MusicListManage;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.GetMusicListMessage;
import com.github.tartaricacid.netmusic.util.MusicCdWriteHelper;
import com.github.tartaricacid.netmusic.util.PendingSongTracker;
import com.github.tartaricacid.netmusic.util.PlayerInteractionTracker;
import com.github.tartaricacid.netmusic.util.ComputerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import net.minecraft.ChatMessageComponent;
import net.minecraft.CommandBase;
import net.minecraft.ICommandSender;
import net.minecraft.ServerPlayer;
import net.minecraft.StatCollector;
import net.minecraft.WrongUsageException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetMusicCommand extends CommandBase {
    private static final long INTERACTION_WINDOW_TICKS = 20L * 30L;
    private static final long PENDING_WINDOW_TICKS = INTERACTION_WINDOW_TICKS * 4L;

    @Override
    public String getCommandName() {
        return "netmusic";
    }

    @Override
    public List getCommandAliases() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("nm");
        return aliases;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/netmusic <reload|get163|add163cd|get163cd|adddjcd|getdjcd|addurlcd|status|clearpending> ...";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1 || "help".equalsIgnoreCase(args[0])) {
            sendUsage(sender);
            return;
        }

        ServerPlayer player = sender instanceof ServerPlayer ? (ServerPlayer) sender : null;
        if (player == null) {
            sender.sendChatToPlayer(ChatMessageComponent.createFromText("This command can only be used by players."));
            return;
        }

        String sub = args[0];
        if ("reload".equalsIgnoreCase(sub)) {
            NetworkHandler.sendToClientPlayer(new GetMusicListMessage(GetMusicListMessage.RELOAD_MESSAGE), player);
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocal("command.netmusic.music_cd.reload.success")));
            return;
        }

        if ("status".equalsIgnoreCase(sub)) {
            long now = player.worldObj == null ? 0L : player.worldObj.getTotalWorldTime();
            String status = PlayerInteractionTracker.getRecentInteractionText(player, now, INTERACTION_WINDOW_TICKS);
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocalFormatted("command.netmusic.music_cd.status", status)));
            String pending = PendingSongTracker.getPendingText(player, now, PENDING_WINDOW_TICKS);
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocalFormatted("command.netmusic.music_cd.pending.status", pending)));
            return;
        }

        if ("clearpending".equalsIgnoreCase(sub)) {
            PendingSongTracker.clear(player);
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocal("command.netmusic.music_cd.pending.clear")));
            return;
        }

        if (isAnyOf(sub, "add163", "get163")) {
            if (args.length < 2) {
                throw new WrongUsageException("/netmusic add163 <playlistId>");
            }
            try {
                long playlistId = Long.parseLong(args[1]);
                if (playlistId <= 0) {
                    throw new NumberFormatException();
                }
                NetworkHandler.sendToClientPlayer(new GetMusicListMessage(playlistId), player);
            } catch (NumberFormatException e) {
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.add163.fail")));
            }
            return;
        }

        if (isAnyOf(sub, "add163cd", "get163cd")) {
            if (args.length < 2) {
                throw new WrongUsageException("/netmusic add163cd <musicId>");
            }
            long musicId;
            try {
                musicId = Long.parseLong(args[1]);
                if (musicId <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.add163cd.fail")));
                return;
            }
            try {
                ItemMusicCD.SongInfo songInfo = MusicListManage.get163Song(musicId);
                if (isDirectGiveMode(sub)) {
                    giveSongCdDirect(player, sender, songInfo, "command.netmusic.music_cd.add163cd.success");
                } else {
                    applySongForSource(player, sender, songInfo, PendingSongTracker.Source.CD_BURNER,
                            "command.netmusic.music_cd.need_cd_burner", "command.netmusic.music_cd.add163cd.success");
                }
            } catch (Exception e) {
                NetMusic.LOGGER.error("Failed to get NetEase song by id: {}", musicId, e);
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.add163cd.fail")));
            }
            return;
        }

        if (isAnyOf(sub, "adddjcd", "getdjcd")) {
            if (args.length < 2) {
                throw new WrongUsageException("/netmusic adddjcd <djMusicId>");
            }
            long musicId;
            try {
                musicId = Long.parseLong(args[1]);
                if (musicId <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.addDJcd.fail")));
                return;
            }
            try {
                ItemMusicCD.SongInfo songInfo = MusicListManage.getDjSong(musicId);
                if (isDirectGiveMode(sub)) {
                    giveSongCdDirect(player, sender, songInfo, "command.netmusic.music_cd.addDJcd.success");
                } else {
                    applySongForSource(player, sender, songInfo, PendingSongTracker.Source.CD_BURNER,
                            "command.netmusic.music_cd.need_cd_burner", "command.netmusic.music_cd.addDJcd.success");
                }
            } catch (Exception e) {
                NetMusic.LOGGER.error("Failed to get NetEase DJ song by id: {}", musicId, e);
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.addDJcd.fail")));
            }
            return;
        }

        if ("addurlcd".equalsIgnoreCase(sub)) {
            if (args.length < 4) {
                throw new WrongUsageException("/netmusic addurlcd <url_or_path> <timeSecond> <name>");
            }
            ScreenSubmitResult result = ComputerInputParser.parseSongInfo(args[1], joinName(args, 3), args[2], false);
            if (!result.isSuccess()) {
                String key = result.getMessageKey();
                if (key == null) {
                    key = "command.netmusic.music_cd.addurlcd.fail";
                }
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(StatCollector.translateToLocal(key)));
                return;
            }

            ItemMusicCD.SongInfo songInfo = result.getSongInfo();
            applySongForSource(player, sender, songInfo, PendingSongTracker.Source.COMPUTER,
                    "command.netmusic.music_cd.need_computer", "command.netmusic.music_cd.addurlcd.success");
            return;
        }

        sendUsage(sender);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    "reload", "add163", "get163", "add163cd", "get163cd", "adddjcd", "getdjcd",
                    "addurlcd", "status", "clearpending", "help");
        }
        if (args.length == 2 && isAnyOf(args[0], "add163", "get163", "add163cd", "get163cd", "adddjcd", "getdjcd")) {
            return Collections.singletonList("<id>");
        }
        if (args.length == 2 && "addurlcd".equalsIgnoreCase(args[0])) {
            return Collections.singletonList("<url_or_path>");
        }
        if (args.length == 3 && "addurlcd".equalsIgnoreCase(args[0])) {
            return Collections.singletonList("<timeSecond>");
        }
        return null;
    }

    private static void sendUsage(ICommandSender sender) {
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic reload"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic add163|get163 <playlistId>"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic add163cd <musicId>    (writer flow)"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic get163cd <musicId>    (direct CD)"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic adddjcd <djMusicId>   (writer flow)"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic getdjcd <djMusicId>   (direct CD)"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic addurlcd <url_or_path> <timeSecond> <name>"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic status"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic clearpending"));
    }

    private static void giveSongCdDirect(ServerPlayer player, ICommandSender sender, ItemMusicCD.SongInfo songInfo, String successKey) {
        if (songInfo == null) {
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocal("command.netmusic.music_cd.no_song_info")));
            return;
        }
        if (!MusicCdWriteHelper.giveSongCdToPlayer(player, songInfo)) {
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocal("command.netmusic.music_cd.direct_give_fail")));
            return;
        }
        PendingSongTracker.clear(player);
        sender.sendChatToPlayer(ChatMessageComponent.createFromText(StatCollector.translateToLocal(successKey)));
    }

    private static void applySongForSource(ServerPlayer player, ICommandSender sender, ItemMusicCD.SongInfo songInfo,
                                           PendingSongTracker.Source source, String needInteractionKey, String successKey) {
        if (songInfo == null) {
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocal("command.netmusic.music_cd.no_song_info")));
            return;
        }

        if (setSongToOpenMenu(player, songInfo) || writeSongWithSourceInteractionCheck(player, songInfo, source)) {
            PendingSongTracker.clear(player);
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(StatCollector.translateToLocal(successKey)));
            return;
        }


        long now = player.worldObj == null ? 0L : player.worldObj.getTotalWorldTime();
        PendingSongTracker.setPending(player, source, songInfo, now);
        sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                StatCollector.translateToLocalFormatted("command.netmusic.music_cd.pending.saved",
                        StatCollector.translateToLocal(source == PendingSongTracker.Source.CD_BURNER
                                ? "command.netmusic.source.cd_burner"
                                : "command.netmusic.source.computer"),
                        songInfo.songName == null ? "unknown" : songInfo.songName)));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText(StatCollector.translateToLocal(needInteractionKey)));
    }

    private static boolean isAnyOf(String input, String... options) {
        for (String option : options) {
            if (option.equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDirectGiveMode(String subCommand) {
        return subCommand != null && subCommand.toLowerCase().startsWith("get");
    }

    private static String joinName(String[] args, int start) {
        if (start >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }


    private static boolean setSongToOpenMenu(ServerPlayer player, ItemMusicCD.SongInfo songInfo) {
        if (player.openContainer instanceof CDBurnerMenu cdBurnerMenu) {
            cdBurnerMenu.setSongInfo(songInfo);
            return true;
        }
        if (player.openContainer instanceof ComputerMenu computerMenu) {
            computerMenu.setSongInfo(songInfo);
            return true;
        }
        return false;
    }

    private static boolean writeSongToInventoryCd(ServerPlayer player, ItemMusicCD.SongInfo songInfo) {
        if (player == null || songInfo == null) {
            return false;
        }
        return MusicCdWriteHelper.writeSongToPlayerCd(player, songInfo);
    }

    private static boolean writeSongWithSourceInteractionCheck(ServerPlayer player, ItemMusicCD.SongInfo songInfo,
                                                               PendingSongTracker.Source source) {
        long now = player.worldObj == null ? 0L : player.worldObj.getTotalWorldTime();
        boolean hasInteraction = source == PendingSongTracker.Source.CD_BURNER
                ? PlayerInteractionTracker.hasRecentCDBurnerInteraction(player, now, INTERACTION_WINDOW_TICKS)
                : PlayerInteractionTracker.hasRecentComputerInteraction(player, now, INTERACTION_WINDOW_TICKS);
        if (!hasInteraction) {
            return false;
        }
        return writeSongToInventoryCd(player, songInfo);
    }
}
