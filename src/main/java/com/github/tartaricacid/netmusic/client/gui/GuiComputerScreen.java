package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.github.tartaricacid.netmusic.util.ComputerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import net.minecraft.GuiButton;
import net.minecraft.GuiContainer;
import net.minecraft.GuiTextField;
import net.minecraft.ResourceLocation;
import net.minecraft.StatCollector;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;

public class GuiComputerScreen extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation("netmusic", "textures/gui/computer.png");
    private final ComputerMenu menu;

    private GuiTextField urlField;
    private GuiTextField nameField;
    private GuiTextField timeField;
    private boolean readOnly;
    private String tipsKey = "";

    public GuiComputerScreen(ComputerMenu menu) {
        super(menu);
        this.menu = menu;
        this.xSize = 176;
        this.ySize = 216;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        String prevUrl = this.urlField != null ? this.urlField.getText() : "";
        String prevName = this.nameField != null ? this.nameField.getText() : "";
        String prevTime = this.timeField != null ? this.timeField.getText() : "";
        boolean prevUrlFocused = this.urlField != null && this.urlField.isFocused();
        boolean prevNameFocused = this.nameField != null && this.nameField.isFocused();
        boolean prevTimeFocused = this.timeField != null && this.timeField.isFocused();

        this.urlField = new GuiTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 18, 120, 16);
        this.urlField.setMaxStringLength(32500);
        this.urlField.setEnableBackgroundDrawing(false);
        this.urlField.setText(prevUrl);
        this.urlField.setFocused(prevUrlFocused || prevUrl.isEmpty());

        this.nameField = new GuiTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 39, 120, 16);
        this.nameField.setMaxStringLength(256);
        this.nameField.setEnableBackgroundDrawing(false);
        this.nameField.setText(prevName);
        this.nameField.setFocused(prevNameFocused);

        this.timeField = new GuiTextField(this.fontRenderer, this.guiLeft + 10, this.guiTop + 61, 40, 16);
        this.timeField.setMaxStringLength(5);
        this.timeField.setEnableBackgroundDrawing(false);
        this.timeField.setText(prevTime);
        this.timeField.setFocused(prevTimeFocused);

        this.buttonList.add(new GuiButton(0, this.guiLeft + 7, this.guiTop + 78, 135, 18,
                StatCollector.translateToLocal("gui.netmusic.cd_burner.craft")));
        this.buttonList.add(new GuiButton(1, this.guiLeft + 58, this.guiTop + 55, 86, 20, getReadOnlyText()));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
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
        String writeFailure = this.menu.getWriteFailureKey();
        if (writeFailure != null && !"gui.netmusic.computer.url.error".equals(writeFailure)) {
            this.tipsKey = writeFailure;
            return;
        }

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
        this.tipsKey = "";
        ClientNetWorkHandler.sendToServer(new SetMusicIDMessage(SetMusicIDMessage.Source.COMPUTER, songInfo));
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == this.mc.gameSettings.keyBindInventory.keyCode
                && (this.urlField.isFocused() || this.nameField.isFocused() || this.timeField.isFocused())) {
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
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(BG);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        this.urlField.drawTextBox();
        this.nameField.drawTextBox();
        this.timeField.drawTextBox();

        if (this.urlField.getText().trim().isEmpty() && !this.urlField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.computer.url.tips"), this.guiLeft + 12, this.guiTop + 18, 0xA0A0A0);
        }
        if (this.nameField.getText().trim().isEmpty() && !this.nameField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.computer.name.tips"), this.guiLeft + 12, this.guiTop + 39, 0xA0A0A0);
        }
        if (this.timeField.getText().trim().isEmpty() && !this.timeField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.computer.time.tips"), this.guiLeft + 11, this.guiTop + 61, 0xA0A0A0);
        }

        if (this.tipsKey != null && !this.tipsKey.isEmpty()) {
            this.fontRenderer.drawSplitString(StatCollector.translateToLocal(this.tipsKey), this.guiLeft + 8, this.guiTop + 100, 162, 0xCF0000);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

