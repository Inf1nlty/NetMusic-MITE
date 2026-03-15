package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.github.tartaricacid.netmusic.util.CDBurnerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import org.apache.commons.lang3.StringUtils;

public class CDBurnerMenuScreen {
    private final CDBurnerMenu menu;
    private String musicIdInput = "";
    private boolean readOnly;
    private String tipsKey = "";

    public CDBurnerMenuScreen(CDBurnerMenu menu) {
        this.menu = menu;
    }

    public CDBurnerMenu getMenu() {
        return this.menu;
    }

    public void setMusicIdInput(String text) {
        this.musicIdInput = CDBurnerInputParser.normalizeInput(text);
    }

    public String getMusicIdInput() {
        return this.musicIdInput;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public String getTipsKey() {
        return this.tipsKey;
    }

    public boolean submitCraft() {
        String writeFailure = this.menu.getWriteFailureKey();
        if (writeFailure != null && !"gui.netmusic.cd_burner.get_info_error".equals(writeFailure)) {
            this.tipsKey = writeFailure;
            return false;
        }

        ScreenSubmitResult result = CDBurnerInputParser.parseSongInfo(this.musicIdInput, this.readOnly);
        if (!result.isSuccess()) {
            this.tipsKey = result.getMessageKey();
            return false;
        }

        ItemMusicCD.SongInfo songInfo = result.getSongInfo();
        if (songInfo == null || StringUtils.isBlank(songInfo.songUrl) || StringUtils.isBlank(songInfo.songName)) {
            this.tipsKey = "gui.netmusic.cd_burner.get_info_error";
            return false;
        }

        this.tipsKey = "";
        ClientNetWorkHandler.sendToServer(new SetMusicIDMessage(SetMusicIDMessage.Source.CD_BURNER, songInfo));
        return true;
    }
}
