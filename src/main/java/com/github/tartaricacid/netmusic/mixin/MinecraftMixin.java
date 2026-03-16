package com.github.tartaricacid.netmusic.mixin;

import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayer;
import net.minecraft.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "runTick", at = @At("TAIL"))
    private void netmusic$clientTick(CallbackInfo ci) {
        ClientMusicPlayer.clientTick();
    }
}
