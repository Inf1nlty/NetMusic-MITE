package com.github.tartaricacid.netmusic.item;

import com.github.tartaricacid.netmusic.api.pojo.NetEaseMusicList;
import com.github.tartaricacid.netmusic.api.pojo.NetEaseMusicSong;
import com.github.tartaricacid.netmusic.creativetab.NetMusicCreativeTab;
import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import net.minecraft.EntityPlayer;
import net.minecraft.EnumChatFormatting;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.NBTBase;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.NBTTagString;
import net.minecraft.Slot;
import net.minecraft.StatCollector;
import net.xiaoyu233.fml.reload.utils.IdUtil;
import org.apache.commons.lang3.StringUtils;
import com.github.tartaricacid.netmusic.util.SongInfoHelper;

import java.util.List;

public class ItemMusicCD extends Item {
    public static final String SONG_INFO_TAG = "NetMusicSongInfo";

    public ItemMusicCD() {
        super(IdUtil.getNextItemID(), "music_cd");
        this.setCreativeTab(NetMusicCreativeTab.TAB);
    }

    public static SongInfo getSongInfo(ItemStack stack) {
        if (stack != null) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null && tag.hasKey(SONG_INFO_TAG)) {
                NBTTagCompound infoTag = tag.getCompoundTag(SONG_INFO_TAG);
                return SongInfo.deserializeNBT(infoTag);
            }
        }
        return null;
    }

    public static ItemStack setSongInfo(SongInfo info, ItemStack stack) {
        SongInfo sanitized = SongInfoHelper.sanitize(info);
        if (stack != null && sanitized != null) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
            }
            NBTTagCompound songInfoTag = new NBTTagCompound();
            SongInfo.serializeNBT(sanitized, songInfoTag);
            tag.setCompoundTag(SONG_INFO_TAG, songInfoTag);
            stack.setTagCompound(tag);
        }
        return stack;
    }

    @Override
    public String getItemDisplayName(ItemStack stack) {
        SongInfo info = getSongInfo(stack);
        if (info != null) {
            String name = info.songName;
            if (info.vip) {
                name = name + " " + EnumChatFormatting.DARK_RED + "[VIP]";
            }
            if (info.readOnly) {
                return name + " " + EnumChatFormatting.YELLOW + StatCollector.translateToLocal("tooltips.netmusic.cd.read_only");
            }
            return name;
        }
        return super.getItemDisplayName(stack);
    }

    private String getSongTime(int songTime) {
        int min = songTime / 60;
        int sec = songTime % 60;
        String minStr = min <= 9 ? ("0" + min) : ("" + min);
        String secStr = sec <= 9 ? ("0" + sec) : ("" + sec);
        return StatCollector.translateToLocalFormatted("tooltips.netmusic.cd.time.format", minStr, secStr);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean extendedInfo, Slot slot) {
        super.addInformation(stack, player, tooltip, extendedInfo, slot);
        SongInfo songInfo = getSongInfo(stack);
        final String prefix = "§a▍ §7";
        final String delimiter = ": ";
        if (songInfo != null) {
            if (StringUtils.isNoneBlank(songInfo.transName)) {
                String text = prefix + StatCollector.translateToLocal("tooltips.netmusic.cd.trans_name") + delimiter + "§6" + songInfo.transName;
                tooltip.add(text);
            }
            if (songInfo.artists != null && !songInfo.artists.isEmpty()) {
                String artistNames = StringUtils.join(songInfo.artists, " | ");
                String text = prefix + StatCollector.translateToLocal("tooltips.netmusic.cd.artists") + delimiter + "§3" + artistNames;
                tooltip.add(text);
            }
            String text = prefix + StatCollector.translateToLocal("tooltips.netmusic.cd.time") + delimiter + "§5" + getSongTime(songInfo.songTime);
            tooltip.add(text);
        } else {
            tooltip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("tooltips.netmusic.cd.empty"));
        }
    }

    public static class SongInfo {
        @SerializedName("url")
        public String songUrl;
        @SerializedName("name")
        public String songName;
        @SerializedName("time_second")
        public int songTime;
        @SerializedName("trans_name")
        public String transName = StringUtils.EMPTY;
        @SerializedName("vip")
        public boolean vip = false;
        @SerializedName("read_only")
        public boolean readOnly = false;
        @SerializedName("artists")
        public List<String> artists = Lists.newArrayList();

        public SongInfo() {}

        public SongInfo(String songUrl, String songName, int songTime, boolean readOnly) {
            this.songUrl = songUrl;
            this.songName = songName;
            this.songTime = songTime;
            this.readOnly = readOnly;
        }

        public SongInfo(NetEaseMusicSong pojo) {
            NetEaseMusicSong.Song song = pojo.getSong();
            if (song != null) {
                this.songUrl = String.format("https://music.163.com/song/media/outer/url?id=%d.mp3", song.getId());
                this.songName = song.getName();
                this.songTime = song.getDuration() / 1000;
                this.transName = song.getTransName();
                this.vip = song.needVip();
                this.artists = song.getArtists();
            }
        }

        public SongInfo(NetEaseMusicSong.Song song) {
            this.songUrl = String.format("https://music.163.com/song/media/outer/url?id=%d.mp3", song.getId());
            this.songName = song.getName();
            this.songTime = song.getDuration() / 1000;
            this.transName = song.getTransName();
            this.vip = song.needVip();
            this.artists = song.getArtists();
        }

        public SongInfo(NetEaseMusicList.Track track) {
            this.songUrl = String.format("https://music.163.com/song/media/outer/url?id=%d.mp3", track.getId());
            this.songName = track.getName();
            this.songTime = track.getDuration() / 1000;
            this.transName = track.getTransName();
            this.vip = track.needVip();
            this.artists = track.getArtists();
        }

        public SongInfo(NBTTagCompound tag) {
            this.songUrl = tag.getString("url");
            this.songName = tag.getString("name");
            this.songTime = tag.getInteger("time");
            if (tag.hasKey("trans_name")) {
                this.transName = tag.getString("trans_name");
            }
            if (tag.hasKey("vip")) {
                this.vip = tag.getBoolean("vip");
            }
            if (tag.hasKey("read_only")) {
                this.readOnly = tag.getBoolean("read_only");
            }
            if (tag.hasKey("artists")) {
                NBTTagList tagList = tag.getTagList("artists");
                this.artists = Lists.newArrayList();
                for (int i = 0; i < tagList.tagCount(); i++) {
                    NBTBase nbt = tagList.tagAt(i);
                    if (nbt instanceof NBTTagString stringTag) {
                        this.artists.add(stringTag.data);
                    }
                }
            }
        }

        public static SongInfo deserializeNBT(NBTTagCompound tag) {
            return new SongInfo(tag);
        }

        public static void serializeNBT(SongInfo info, NBTTagCompound tag) {
            if (info == null || tag == null) {
                return;
            }
            tag.setString("url", info.songUrl);
            tag.setString("name", info.songName);
            tag.setInteger("time", info.songTime);
            if (StringUtils.isNoneBlank(info.transName)) {
                tag.setString("trans_name", info.transName);
            }
            tag.setBoolean("vip", info.vip);
            tag.setBoolean("read_only", info.readOnly);
            if (info.artists != null && !info.artists.isEmpty()) {
                NBTTagList nbt = new NBTTagList();
                for (String name : info.artists) {
                    nbt.appendTag(new NBTTagString("", name));
                }
                tag.setTag("artists", nbt);
            }
        }
    }
}
