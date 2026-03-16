package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.github.tartaricacid.netmusic.util.CDBurnerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import net.minecraft.GuiButton;
import net.minecraft.GuiScreen;
import net.minecraft.GuiTextField;
import net.minecraft.ResourceLocation;
import net.minecraft.StatCollector;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;

public class GuiCDBurnerScreen extends GuiScreen {
    private static final ResourceLocation BG = new ResourceLocation("netmusic", "textures/gui/cd_burner.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    private GuiTextField idField;
    private boolean readOnly;
    private String tipsKey = "";
    private int left;
    private int top;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.left = (this.width - GUI_WIDTH) / 2;
        this.top = (this.height - GUI_HEIGHT) / 2;

        String prevText = this.idField != null ? this.idField.getText() : "";
        boolean focused = this.idField != null && this.idField.isFocused();

        this.idField = new GuiTextField(this.fontRenderer, left + 12, top + 18, 132, 16) {
            @Override
            public void writeText(String text) {
                super.writeText(CDBurnerInputParser.normalizeInput(text));
            }
        };
        this.idField.setMaxStringLength(19);
        this.idField.setText(prevText);
        this.idField.setEnableBackgroundDrawing(false);
        this.idField.setFocused(true);
        this.idField.setFocused(focused || prevText.isEmpty());

        this.buttonList.add(new GuiButton(0, left + 7, top + 35, 55, 18,
                StatCollector.translateToLocal("gui.netmusic.cd_burner.craft")));
        this.buttonList.add(new GuiButton(1, left + 66, top + 34, 80, 20, getReadOnlyText()));
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
        this.mc.getTextureManager().bindTexture(BG);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(left, top, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        this.idField.drawTextBox();
        if (this.idField.getText().trim().isEmpty() && !this.idField.isFocused()) {
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("gui.netmusic.cd_burner.id.tips"), left + 12, top + 18, 0xA0A0A0);
        }

        if (this.tipsKey != null && !this.tipsKey.isEmpty()) {
            this.fontRenderer.drawSplitString(StatCollector.translateToLocal(this.tipsKey), left + 8, top + 57, 135, 0xCF0000);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

