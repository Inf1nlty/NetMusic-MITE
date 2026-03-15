package com.github.tartaricacid.netmusic.init;

import com.github.tartaricacid.netmusic.network.message.SetMusicIDMessage;
import moddedmite.rustedironcore.network.PacketReader;
import moddedmite.rustedironcore.network.PacketSupplier;
import net.minecraft.ResourceLocation;

public class ServerReceiverRegistry {
    public static void register() {
        registerReceiver(SetMusicIDMessage.ID, SetMusicIDMessage::new);
    }

    public static void registerReceiver(ResourceLocation channelName, PacketSupplier packetSupplier) {
        PacketReader.registerServerPacketReader(channelName, packetSupplier);
    }
}