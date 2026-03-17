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

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GuiCDBurnerScreen extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation("netmusic", "textures/gui/cd_burner.png");
    private static final String NETEASE_LOGIN_URL = "https://music.163.com/#/login";
    private static final String QQ_LOGIN_URL = "https://y.qq.com/";
    private static final Set<String> COOKIE_ATTRIBUTES = Set.of(
            "path", "domain", "expires", "max-age", "httponly", "secure", "samesite", "priority"
    );
    private static final String[] NETEASE_COOKIE_KEYS = new String[]{
            "MUSIC_U", "MUSIC_A", "__csrf", "NMTID", "MUSIC_R_T"
    };
    private static final String[] QQ_COOKIE_KEYS = new String[]{
            "uin", "p_uin", "qqmusic_uin",
            "qm_keyst", "qqmusic_key",
            "skey", "p_skey"
    };
    private final CDBurnerMenu menu;

    private GuiTextField idField;
    private boolean readOnly;
    private String tipsKey = "";
    private boolean waitingCookieImport;
    private int clipboardPollTicks;

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
        this.buttonList.add(new GuiButton(2, this.guiLeft + 7, this.guiTop + 56, 68, 18, getProviderText()));
        this.buttonList.add(new GuiButton(3, this.guiLeft + 78, this.guiTop + 56, 68, 18,
                StatCollector.translateToLocal("gui.netmusic.cd_burner.cookie_button")));
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
            return;
        }
        if (button.id == 3) {
            handleCookieButton();
        }
    }

    private String getReadOnlyText() {
        return StatCollector.translateToLocal("gui.netmusic.cd_burner.read_only") + ": " + (this.readOnly ? "ON" : "OFF");
    }

    private String getProviderText() {
        MusicProviderType provider = GeneralConfig.CD_PROVIDER;
        return StatCollector.translateToLocal("gui.netmusic.cd_burner.provider.short") + ":" + provider.getShortLabel();
    }

    private void toggleProvider() {
        MusicProviderType next = GeneralConfig.CD_PROVIDER.next();
        NetMusicConfigs.CD_PROVIDER.setEnumValue(next);
        NetMusicConfigs.getInstance().save();
    }

    private void handleCookieButton() {
        String importedCookie = tryReadCookieFromClipboard(GeneralConfig.CD_PROVIDER);
        if (!importedCookie.isEmpty()) {
            if (GeneralConfig.CD_PROVIDER == MusicProviderType.QQ) {
                NetMusicConfigs.QQ_VIP_COOKIE.setValueFromString(importedCookie);
            } else {
                NetMusicConfigs.NETEASE_VIP_COOKIE.setValueFromString(importedCookie);
            }
            NetMusicConfigs.getInstance().save();
            this.waitingCookieImport = false;
            this.tipsKey = "gui.netmusic.cd_burner.cookie_imported";
            return;
        }

        if (openProviderLoginPage()) {
            this.waitingCookieImport = true;
            this.clipboardPollTicks = 0;
            this.tipsKey = "gui.netmusic.cd_burner.cookie_opened";
        } else {
            this.tipsKey = "gui.netmusic.cd_burner.cookie_open_failed";
        }
    }

    private boolean openProviderLoginPage() {
        String url = GeneralConfig.CD_PROVIDER == MusicProviderType.QQ ? QQ_LOGIN_URL : NETEASE_LOGIN_URL;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception ignored) {
            // Fallback below.
        }
        try {
            org.lwjgl.Sys.openURL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String tryReadCookieFromClipboard(MusicProviderType provider) {
        try {
            Object value = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (!(value instanceof String text)) {
                return "";
            }
            return extractEssentialCookie(text, provider);
        } catch (Exception e) {
            return "";
        }
    }

    private static String extractEssentialCookie(String raw, MusicProviderType provider) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (text.regionMatches(true, 0, "cookie:", 0, 7)) {
            text = text.substring(7).trim();
        }
        text = text.replace('\r', ' ').replace('\n', ' ').trim();
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1).trim();
        }
        if (!text.contains("=")) {
            return "";
        }

        Map<String, String> parsed = parseCookiePairs(text);
        if (parsed.isEmpty()) {
            return "";
        }
        return provider == MusicProviderType.QQ
                ? buildQqCookie(parsed)
                : buildNeteaseCookie(parsed);
    }

    private static Map<String, String> parseCookiePairs(String raw) {
        Map<String, String> pairs = new LinkedHashMap<String, String>();
        String[] segments = raw.split("[;\\n]");
        for (String segment : segments) {
            String item = segment == null ? "" : segment.trim();
            if (item.isEmpty()) {
                continue;
            }
            int eq = item.indexOf('=');
            if (eq <= 0 || eq >= item.length() - 1) {
                continue;
            }
            String key = item.substring(0, eq).trim();
            String value = item.substring(eq + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if (COOKIE_ATTRIBUTES.contains(lower)) {
                continue;
            }
            pairs.put(key, value);
        }
        return pairs;
    }

    private static String buildNeteaseCookie(Map<String, String> pairs) {
        // Minimal usable keys: MUSIC_U or MUSIC_A.
        if (!containsKey(pairs, "MUSIC_U") && !containsKey(pairs, "MUSIC_A")) {
            return "";
        }
        return joinCookiePairs(pickCookiePairs(pairs, NETEASE_COOKIE_KEYS));
    }

    private static String buildQqCookie(Map<String, String> pairs) {
        // Minimal usable keys: (uin or p_uin) + (qm_keyst or qqmusic_key).
        boolean hasUin = containsKey(pairs, "uin") || containsKey(pairs, "p_uin");
        boolean hasKey = containsKey(pairs, "qm_keyst") || containsKey(pairs, "qqmusic_key");
        if (!hasUin || !hasKey) {
            return "";
        }
        return joinCookiePairs(pickCookiePairs(pairs, QQ_COOKIE_KEYS));
    }

    private static Map<String, String> pickCookiePairs(Map<String, String> source, String[] orderedKeys) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String wantedKey : orderedKeys) {
            String actualKey = findActualKey(source, wantedKey);
            if (actualKey == null) {
                continue;
            }
            String value = source.get(actualKey);
            if (value == null || value.isEmpty()) {
                continue;
            }
            result.put(actualKey, value);
        }
        return result;
    }

    private static String findActualKey(Map<String, String> pairs, String key) {
        for (String existingKey : pairs.keySet()) {
            if (existingKey.equalsIgnoreCase(key)) {
                return existingKey;
            }
        }
        return null;
    }

    private static String joinCookiePairs(Map<String, String> pairs) {
        StringBuilder cookie = new StringBuilder();
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            if (cookie.length() > 0) {
                cookie.append("; ");
            }
            cookie.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return cookie.toString();
    }

    private static boolean containsKey(Map<String, String> pairs, String key) {
        for (String k : pairs.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
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

        if (this.waitingCookieImport) {
            this.clipboardPollTicks++;
            if (this.clipboardPollTicks % 20 == 0) {
                String importedCookie = tryReadCookieFromClipboard(GeneralConfig.CD_PROVIDER);
                if (!importedCookie.isEmpty()) {
                    if (GeneralConfig.CD_PROVIDER == MusicProviderType.QQ) {
                        NetMusicConfigs.QQ_VIP_COOKIE.setValueFromString(importedCookie);
                    } else {
                        NetMusicConfigs.NETEASE_VIP_COOKIE.setValueFromString(importedCookie);
                    }
                    NetMusicConfigs.getInstance().save();
                    this.waitingCookieImport = false;
                    this.tipsKey = "gui.netmusic.cd_burner.cookie_imported";
                }
            }
        }
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

