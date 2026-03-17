package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.config.PlayerVipCookieStore;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

import java.util.UUID;

public class SyncVipCookieMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "sync_vip_cookie");

    private final String neteaseCookie;
    private final String qqCookie;

    public SyncVipCookieMessage(PacketByteBuf buf) {
        this(buf.readString(), buf.readString());
    }

    public SyncVipCookieMessage(String neteaseCookie, String qqCookie) {
        this.neteaseCookie = sanitize(neteaseCookie);
        this.qqCookie = sanitize(qqCookie);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeString(this.neteaseCookie);
        buf.writeString(this.qqCookie);
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        if (entityPlayer == null || entityPlayer.worldObj == null || entityPlayer.worldObj.isRemote) {
            return;
        }
        UUID uuid = entityPlayer.getUniqueIDSilent();
        if (uuid == null) {
            return;
        }
        PlayerVipCookieStore.setCookies(uuid, this.neteaseCookie, this.qqCookie);
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }

    private static String sanitize(String cookie) {
        return cookie == null ? "" : cookie.trim();
    }
}
