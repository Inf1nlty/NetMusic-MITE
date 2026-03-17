package com.github.tartaricacid.netmusic.client.config;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.network.ClientNetWorkHandler;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.config.MusicProviderType;
import com.github.tartaricacid.netmusic.config.PlayerVipCookieStore;
import com.github.tartaricacid.netmusic.network.message.SyncVipCookieMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.EntityPlayer;
import net.minecraft.Minecraft;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class ClientVipCookieManager {
    private static String activePlayerKey;
    private static String lastSyncedNetease = "";
    private static String lastSyncedQq = "";

    private ClientVipCookieManager() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
            activePlayerKey = null;
            return;
        }
        String key = resolveClientPlayerKey(mc.thePlayer);
        if (StringUtils.isBlank(key)) {
            return;
        }
        if (!key.equals(activePlayerKey)) {
            activePlayerKey = key;
            PlayerVipCookieStore.CookiePair pair = PlayerVipCookieStore.getCookies(key);
            applyRuntimeCookies(pair.neteaseCookie, pair.qqCookie);
            syncToServer(pair.neteaseCookie, pair.qqCookie, true);
        }
    }

    public static void updateCookieForCurrentPlayer(MusicProviderType provider, String cookie) {
        MusicProviderType actualProvider = provider == null ? MusicProviderType.NETEASE : provider;
        String sanitized = cookie == null ? "" : cookie.trim();
        String netease = GeneralConfig.NETEASE_VIP_COOKIE;
        String qq = GeneralConfig.QQ_VIP_COOKIE;
        if (actualProvider == MusicProviderType.QQ) {
            qq = sanitized;
        } else {
            netease = sanitized;
        }
        applyRuntimeCookies(netease, qq);

        String key = getCurrentPlayerKey();
        if (StringUtils.isNotBlank(key)) {
            activePlayerKey = key;
            PlayerVipCookieStore.setCookies(key, netease, qq);
        }
        syncToServer(netease, qq, false);
    }

    private static String getCurrentPlayerKey() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return null;
        }
        EntityPlayer player = mc.thePlayer;
        return player == null ? null : resolveClientPlayerKey(player);
    }

    private static String resolveClientPlayerKey(EntityPlayer player) {
        if (player == null) {
            return null;
        }
        UUID uuid = player.getUniqueIDSilent();
        if (uuid != null) {
            return uuid.toString();
        }
        String name = player.getEntityName();
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return "name:" + name.trim();
    }

    private static void applyRuntimeCookies(String neteaseCookie, String qqCookie) {
        GeneralConfig.setVipCookies(neteaseCookie, qqCookie);
        NetMusic.refreshNetEaseApi();
    }

    private static void syncToServer(String neteaseCookie, String qqCookie, boolean force) {
        String netease = neteaseCookie == null ? "" : neteaseCookie.trim();
        String qq = qqCookie == null ? "" : qqCookie.trim();
        if (!force && lastSyncedNetease.equals(netease) && lastSyncedQq.equals(qq)) {
            return;
        }
        ClientNetWorkHandler.sendToServer(new SyncVipCookieMessage(netease, qq));
        lastSyncedNetease = netease;
        lastSyncedQq = qq;
    }
}
