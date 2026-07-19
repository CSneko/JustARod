package org.cneko.justarod.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonNetworkingHandlerMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        ServerCommonPacketListenerImpl handler = (ServerCommonPacketListenerImpl) (Object) this;
        if (handler instanceof ServerGamePacketListenerImpl playerHandler){
            if (playerHandler.player.getEarplug() > 0 && packet instanceof ClientboundSoundPacket) {
                ci.cancel();
            }
        }
    }
}
