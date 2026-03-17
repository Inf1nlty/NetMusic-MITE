package com.github.tartaricacid.netmusic.client.gui;

import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.config.MusicProviderType;
import com.github.tartaricacid.netmusic.config.NetMusicConfigs;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import com.github.tartaricacid.netmusic.util.CDBurnerInputParser;
import com.github.tartaricacid.netmusic.util.ScreenSubmitResult;
import net.minecraft.GuiButton;
import net.minecraft.GuiContainer;
import net.minecraft.GuiTextField;
import net.minecraft.ResourceLocation;
import net.minecraft.StatCollector;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;

public class GuiCDBurnerScreen extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation("netmusic", "textures/gui/cd_burner.png");
    private final CDBurnerMenu menu;

    private GuiTextField idField;
    private boolean readOnly;
    private String tipsKey = "";

    public GuiCDBurnerScreen(CDBurnerMenu menu) {
        super(menu);
        this.menu = menu;
        this.xSize = 176;
        this.ySize = 176;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        String prevText = this.idField != null ? this.idField.getText() : "";
        boolean focused = this.idField != null && this.idField.isFocused();

        this.idField = new GuiTextField(this.fontRenderer, this.guiLeft + 12, this.guiTop + 18, 132, 16) {
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

        this.buttonList.add(new GuiButton(0, this.guiLeft + 7, this.guiTop + 35, 55, 18,
                StatCollector.translateToLocal("gui.netmusic.cd_burner.craft")));
        this.buttonList.add(new GuiButton(1, this.guiLeft + 66, this.guiTop + 34, 80, 20, getReadOnlyText()));
        this.buttonList.add(new GuiButton(2, this.guiLeft + 7, this.guiTop + 56, 139, 18, getProviderText()));
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
            return;
        }
        if (button.id == 2) {
            toggleProvider();
            button.displayString = getProviderText();
        }
    }

    private String getReadOnlyText() {
        return StatCollector.translateToLocal("gui.netmusic.cd_burner.read_only") + ": " + (this.readOnly ? "ON" : "OFF");
    }

    private String getProviderText() {
        MusicProviderType provider = GeneralConfig.CD_PROVIDER;
        String providerKey = "config.enum.netmusic.general.cd_provider." + provider.name();
        String providerText = StatCollector.translateToLocal(providerKey);
        if (providerText.equals(providerKey)) {
            providerText = provider.getShortLabel();
        }
        return StatCollector.translateToLocal("gui.netmusic.cd_burner.provider") + ": " + providerText;
    }

    private void toggleProvider() {
        MusicProviderType next = GeneralConfig.CD_PROVIDER.next();
        NetMusicConfigs.CD_PROVIDER.setEnumValue(next);
        NetMusicConfigs.getInstance().save();
    }

    private void submit() {
        String writeFailure = this.menu.getWriteFailureKey();
        if (writeFailure != null && !"gui.netmusic.cd_burner.get_info_error".equals(writeFailure)) {
            this.tipsKey = writeFailure;
            return;
        }

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
        this.tipsKey = "";
        ClientNetWorkHandler.sendToServer(new SetMusicIDMessage(SetMusicIDMessage.Source.CD_BURNER, songInfo));
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == this.mc.gameSettings.keyBindInventory.keyCode && this.idField.isFocused()) {
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
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(BG);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        this.idField.drawTextBox();
        if (this.idField.getText().trim().isEmpty() && !this.idField.isFocused()) {
            String tipsKey = GeneralConfig.CD_PROVIDER == MusicProviderType.QQ
                    ? "gui.netmusic.cd_burner.id.tips.qq"
                    : "gui.netmusic.cd_burner.id.tips";
            this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal(tipsKey), this.guiLeft + 12, this.guiTop + 18, 0xA0A0A0);
        }

        if (this.tipsKey != null && !this.tipsKey.isEmpty()) {
            this.fontRenderer.drawSplitString(StatCollector.translateToLocal(this.tipsKey), this.guiLeft + 8, this.guiTop + 77, 138, 0xCF0000);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

