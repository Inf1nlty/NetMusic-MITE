package com.github.tartaricacid.netmusic.network;

import com.github.tartaricacid.netmusic.config.GeneralConfig;
import moddedmite.rustedironcore.network.Network;
import moddedmite.rustedironcore.network.Packet;
import net.minecraft.EntityPlayer;
import net.minecraft.ServerPlayer;
import net.minecraft.World;

public class NetworkHandler {

    public static void sendToNearBy(World world, int x, int y, int z, Packet packet) {
        if (world == null || packet == null || world.playerEntities == null) {
            return;
        }
        for (Object obj : world.playerEntities) {
            if (!(obj instanceof ServerPlayer player)) {
                continue;
            }
            double radius = Math.max(1.0D, GeneralConfig.MUSIC_PLAYER_HEAR_DISTANCE);
            if (player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= radius * radius) {
                Network.sendToClient(player, packet);
            }
        }
    }

    public static void sendToNearBy(World world, EntityPlayer center, Packet packet) {
        if (center != null) {
            sendToNearBy(world, center.getBlockPosX(), center.getBlockPosY(), center.getBlockPosZ(), packet);
        }
    }

    public static void sendToClientPlayer(Packet packet, ServerPlayer player) {
        if (packet != null && player != null) {
            Network.sendToClient(player, packet);
        }
    }
}
