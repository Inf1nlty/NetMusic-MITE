package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.github.tartaricacid.netmusic.util.ComputerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import org.apache.commons.lang3.StringUtils;

public class ComputerMenuScreen {
    private final ComputerMenu menu;
    private String urlInput = "";
    private String nameInput = "";
    private String timeInput = "";
    private boolean readOnly;
    private String tipsKey = "";

    public ComputerMenuScreen(ComputerMenu menu) {
        this.menu = menu;
    }

    public ComputerMenu getMenu() {
        return this.menu;
    }

    public void setInputs(String url, String name, String time) {
        this.urlInput = url == null ? "" : url.trim();
        this.nameInput = name == null ? "" : name.trim();
        this.timeInput = time == null ? "" : time.trim();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getTipsKey() {
        return this.tipsKey;
    }

    public boolean submitCraft() {
        String writeFailure = this.menu.getWriteFailureKey();
        if (writeFailure != null && !"gui.netmusic.computer.url.error".equals(writeFailure)) {
            this.tipsKey = writeFailure;
            return false;
        }

        ScreenSubmitResult result = ComputerInputParser.parseSongInfo(this.urlInput, this.nameInput, this.timeInput, this.readOnly);
        if (!result.isSuccess()) {
            this.tipsKey = result.getMessageKey() == null ? "gui.netmusic.computer.url.error" : result.getMessageKey();
            return false;
        }

        ItemMusicCD.SongInfo songInfo = result.getSongInfo();
        if (songInfo == null || StringUtils.isBlank(songInfo.songUrl) || StringUtils.isBlank(songInfo.songName)) {
            this.tipsKey = "gui.netmusic.computer.url.error";
            return false;
        }

        this.tipsKey = "";
        ClientNetWorkHandler.sendToServer(new SetMusicIDMessage(SetMusicIDMessage.Source.COMPUTER, songInfo));
        return true;
    }
}
