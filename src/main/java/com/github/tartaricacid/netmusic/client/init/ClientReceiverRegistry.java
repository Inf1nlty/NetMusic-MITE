package com.github.tartaricacid.netmusic.client.init;

import com.github.tartaricacid.netmusic.network.message.GetMusicListMessage;
import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;
import com.github.tartaricacid.netmusic.network.message.MusicPlayerStateMessage;
import com.github.tartaricacid.netmusic.network.message.OpenMenuMessage;
import moddedmite.rustedironcore.network.PacketReader;
import moddedmite.rustedironcore.network.PacketSupplier;
import net.minecraft.ResourceLocation;

public class ClientReceiverRegistry {
    public static void register() {
        registerReceiver(MusicToClientMessage.ID, MusicToClientMessage::new);
        registerReceiver(MusicPlayerStateMessage.ID, MusicPlayerStateMessage::new);
        registerReceiver(GetMusicListMessage.ID, GetMusicListMessage::new);
        registerReceiver(OpenMenuMessage.ID, OpenMenuMessage::new);
    }

    public static void registerReceiver(ResourceLocation channel, PacketSupplier packetSupplier) {
        PacketReader.registerClientPacketReader(channel, packetSupplier);
    }
}
