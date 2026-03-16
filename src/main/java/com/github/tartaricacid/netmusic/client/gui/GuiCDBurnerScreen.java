package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.github.tartaricacid.netmusic.util.CDBurnerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;
import net.minecraft.GuiTextField;
import net.minecraft.StatCollector;
import org.lwjgl.input.Keyboard;

public class GuiCDBurnerScreen extends GuiScreen {
    private GuiTextField idField;
    private boolean readOnly;
    private String tipsKey = "";

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int left = this.width / 2 - 90;
        int top = this.height / 2 - 45;
        this.idField = new GuiTextField(this.fontRenderer, left + 8, top + 20, 164, 18);
        this.idField.setMaxStringLength(64);
        this.idField.setFocused(true);

        this.buttonList.add(new GuiButton(0, left + 8, top + 50, 78, 20,
                StatCollector.translateToLocal("gui.netmusic.cd_burner.craft")));
        this.buttonList.add(new GuiButton(1, left + 94, top + 50, 78, 20, getReadOnlyText()));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }
        if (button.id == 1) {
            this.readOnly = !this.readOnly;
            button.displayString = getReadOnlyText();
            return;
        }
        if (button.id == 0) {
            submit();
        }
    }

    private String getReadOnlyText() {
        return StatCollector.translateToLocal("gui.netmusic.cd_burner.read_only") + ": " + (this.readOnly ? "ON" : "OFF");
    }

    private void submit() {
        ScreenSubmitResult result = CDBurnerInputParser.parseSongInfo(this.idField.getText(), this.readOnly);
        if (!result.isSuccess()) {
            this.tipsKey = result.getMessageKey();
            return;
        }
        ItemMusicCD.SongInfo songInfo = result.getSongInfo();
        if (songInfo == null) {
            this.tipsKey = "gui.netmusic.cd_burner.get_info_error";
            return;
        }
        ClientNetWorkHandler.sendToServer(new SetMusicIDMessage(SetMusicIDMessage.Source.CD_BURNER, songInfo));
        this.tipsKey = "message.netmusic.cd_burner.applied";
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || (keyCode == this.mc.gameSettings.keyBindInventory.keyCode && !this.idField.isFocused())) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (this.idField.textboxKeyTyped(c, keyCode)) {
            return;
        }
        super.keyTyped(c, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        this.idField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.idField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int left = this.width / 2 - 90;
        int top = this.height / 2 - 45;
        drawRect(left, top, left + 180, top + 95, 0xCC000000);
        this.drawCenteredString(this.fontRenderer, StatCollector.translateToLocal("tile.netmusic:cd_burner.name"), this.width / 2, top + 8, 0xFFFFFF);

        this.idField.drawTextBox();
        if (this.idField.getText().trim().isEmpty() && !this.idField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.cd_burner.id.tips"), left + 10, top + 25, 0xA0A0A0);
        }

        if (this.tipsKey != null && !this.tipsKey.isEmpty()) {
            this.fontRenderer.drawSplitString(StatCollector.translateToLocal(this.tipsKey), left + 8, top + 74, 164, 0xFF5555);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

