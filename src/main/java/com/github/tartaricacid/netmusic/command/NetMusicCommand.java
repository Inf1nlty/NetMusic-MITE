package com.github.tartaricacid.netmusic.command;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.config.MusicListManage;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.GetMusicListMessage;
import com.github.tartaricacid.netmusic.util.PlayerInteractionTracker;
import net.minecraft.ChatMessageComponent;
import net.minecraft.CommandBase;
import net.minecraft.ICommandSender;
import net.minecraft.ItemStack;
import net.minecraft.ServerPlayer;
import net.minecraft.StatCollector;
import net.minecraft.WrongUsageException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetMusicCommand extends CommandBase {
    private static final long INTERACTION_WINDOW_TICKS = 20L * 30L;

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
        return "/netmusic <reload|add163|add163cd|adddjcd|addurlcd> ...";
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

        if ("add163".equalsIgnoreCase(sub)) {
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

        if ("add163cd".equalsIgnoreCase(sub)) {
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
                if (!setSongToOpenMenu(player, songInfo) && !writeSongWithInteractionCheck(player, songInfo)) {
                    sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                            StatCollector.translateToLocal("command.netmusic.music_cd.need_interaction")));
                    return;
                }
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.add163cd.success")));
            } catch (Exception e) {
                NetMusic.LOGGER.error("Failed to get NetEase song by id: {}", musicId, e);
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.add163cd.fail")));
            }
            return;
        }

        if ("adddjcd".equalsIgnoreCase(sub)) {
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
                if (!setSongToOpenMenu(player, songInfo) && !writeSongWithInteractionCheck(player, songInfo)) {
                    sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                            StatCollector.translateToLocal("command.netmusic.music_cd.need_interaction")));
                    return;
                }
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.addDJcd.success")));
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
            String url = normalizeUrl(args[1]);
            if (url == null) {
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.addurlcd.fail")));
                return;
            }

            int time;
            try {
                time = Integer.parseInt(args[2]);
                if (time <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.time.fail")));
                return;
            }

            String songName = joinName(args, 3);
            if (StringUtils.isBlank(songName)) {
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.name.fail")));
                return;
            }

            ItemMusicCD.SongInfo songInfo = new ItemMusicCD.SongInfo(url, songName, time, false);
            if (!setSongToOpenMenu(player, songInfo) && !writeSongWithInteractionCheck(player, songInfo)) {
                sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                        StatCollector.translateToLocal("command.netmusic.music_cd.need_interaction")));
                return;
            }
            sender.sendChatToPlayer(ChatMessageComponent.createFromText(
                    StatCollector.translateToLocal("command.netmusic.music_cd.addurlcd.success")));
            return;
        }

        sendUsage(sender);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "reload", "add163", "add163cd", "adddjcd", "addurlcd", "help");
        }
        if (args.length == 2 && ("add163".equalsIgnoreCase(args[0]) || "add163cd".equalsIgnoreCase(args[0]) || "adddjcd".equalsIgnoreCase(args[0]))) {
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
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic add163 <playlistId>"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic add163cd <musicId>"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic adddjcd <djMusicId>"));
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("/netmusic addurlcd <url_or_path> <timeSecond> <name>"));
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

    private static String normalizeUrl(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        String text = input.trim();
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("file:/")) {
            try {
                new URL(text);
                return text;
            } catch (MalformedURLException e) {
                return null;
            }
        }

        File local = new File(text);
        if (local.exists()) {
            return local.toURI().toString();
        }
        return null;
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
        if (player == null || songInfo == null || InitItems.MUSIC_CD == null) {
            return false;
        }

        int slot = findWritableMusicCdSlot(player);
        if (slot < 0) {
            return false;
        }

        ItemMusicCD.SongInfo existing = ItemMusicCD.getSongInfo(player.inventory.mainInventory[slot]);
        if (existing != null && existing.readOnly) {
            return false;
        }

        if (player.inventory.mainInventory[slot].stackSize <= 1) {
            ItemMusicCD.setSongInfo(songInfo, player.inventory.mainInventory[slot]);
            player.inventory.onInventoryChanged();
            return true;
        }

        ItemStack singleCd = player.inventory.mainInventory[slot].copy();
        singleCd.stackSize = 1;
        ItemMusicCD.setSongInfo(songInfo, singleCd);

        player.inventory.mainInventory[slot].stackSize -= 1;
        if (player.inventory.mainInventory[slot].stackSize <= 0) {
            player.inventory.mainInventory[slot] = null;
        }

        if (!player.inventory.addItemStackToInventory(singleCd)) {
            player.dropPlayerItem(singleCd);
        }
        player.inventory.onInventoryChanged();
        return true;
    }

    private static boolean writeSongWithInteractionCheck(ServerPlayer player, ItemMusicCD.SongInfo songInfo) {
        long now = player.worldObj == null ? 0L : player.worldObj.getTotalWorldTime();
        if (!PlayerInteractionTracker.hasRecentCdWriterInteraction(player, now, INTERACTION_WINDOW_TICKS)) {
            return false;
        }
        return writeSongToInventoryCd(player, songInfo);
    }

    private static int findWritableMusicCdSlot(ServerPlayer player) {
        int current = player.inventory.currentItem;
        if (current >= 0 && current < player.inventory.mainInventory.length) {
            ItemStack held = player.inventory.mainInventory[current];
            if (isWritableCd(held)) {
                return current;
            }
        }

        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (isWritableCd(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isWritableCd(ItemStack stack) {
        if (stack == null || InitItems.MUSIC_CD == null || stack.itemID != InitItems.MUSIC_CD.itemID) {
            return false;
        }
        ItemMusicCD.SongInfo existing = ItemMusicCD.getSongInfo(stack);
        return existing == null || !existing.readOnly;
    }
}
