package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.github.tartaricacid.netmusic.util.ComputerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;
import net.minecraft.GuiTextField;
import net.minecraft.StatCollector;
import org.lwjgl.input.Keyboard;

public class GuiComputerScreen extends GuiScreen {
    private GuiTextField urlField;
    private GuiTextField nameField;
    private GuiTextField timeField;
    private boolean readOnly;
    private String tipsKey = "";

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int left = this.width / 2 - 105;
        int top = this.height / 2 - 72;

        this.urlField = new GuiTextField(this.fontRenderer, left + 10, top + 20, 190, 18);
        this.urlField.setMaxStringLength(1024);
        this.urlField.setFocused(true);
        this.nameField = new GuiTextField(this.fontRenderer, left + 10, top + 45, 190, 18);
        this.nameField.setMaxStringLength(128);
        this.timeField = new GuiTextField(this.fontRenderer, left + 10, top + 70, 80, 18);
        this.timeField.setMaxStringLength(6);

        this.buttonList.add(new GuiButton(0, left + 10, top + 100, 92, 20,
                StatCollector.translateToLocal("gui.netmusic.cd_burner.craft")));
        this.buttonList.add(new GuiButton(1, left + 108, top + 100, 92, 20, getReadOnlyText()));
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
        ScreenSubmitResult result = ComputerInputParser.parseSongInfo(
                this.urlField.getText(), this.nameField.getText(), this.timeField.getText(), this.readOnly);
        if (!result.isSuccess()) {
            this.tipsKey = result.getMessageKey() == null ? "gui.netmusic.computer.url.error" : result.getMessageKey();
            return;
        }
        ItemMusicCD.SongInfo songInfo = result.getSongInfo();
        if (songInfo == null) {
            this.tipsKey = "gui.netmusic.computer.url.error";
            return;
        }
        ClientNetWorkHandler.sendToServer(new SetMusicIDMessage(SetMusicIDMessage.Source.COMPUTER, songInfo));
        this.tipsKey = "message.netmusic.computer.applied";
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || (keyCode == this.mc.gameSettings.keyBindInventory.keyCode
                && !this.urlField.isFocused() && !this.nameField.isFocused() && !this.timeField.isFocused())) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (this.urlField.textboxKeyTyped(c, keyCode) || this.nameField.textboxKeyTyped(c, keyCode) || this.timeField.textboxKeyTyped(c, keyCode)) {
            return;
        }
        super.keyTyped(c, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        this.urlField.mouseClicked(mouseX, mouseY, button);
        this.nameField.mouseClicked(mouseX, mouseY, button);
        this.timeField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.urlField.updateCursorCounter();
        this.nameField.updateCursorCounter();
        this.timeField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int left = this.width / 2 - 105;
        int top = this.height / 2 - 72;
        drawRect(left, top, left + 210, top + 130, 0xCC000000);
        this.drawCenteredString(this.fontRenderer, StatCollector.translateToLocal("tile.netmusic:computer.name"), this.width / 2, top + 8, 0xFFFFFF);

        this.urlField.drawTextBox();
        this.nameField.drawTextBox();
        this.timeField.drawTextBox();

        if (this.urlField.getText().trim().isEmpty() && !this.urlField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.computer.url.tips"), left + 12, top + 25, 0xA0A0A0);
        }
        if (this.nameField.getText().trim().isEmpty() && !this.nameField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.computer.name.tips"), left + 12, top + 50, 0xA0A0A0);
        }
        if (this.timeField.getText().trim().isEmpty() && !this.timeField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.computer.time.tips"), left + 12, top + 75, 0xA0A0A0);
        }

        if (this.tipsKey != null && !this.tipsKey.isEmpty()) {
            this.fontRenderer.drawSplitString(StatCollector.translateToLocal(this.tipsKey), left + 10, top + 124, 190, 0xFF5555);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

