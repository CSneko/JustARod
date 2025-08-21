package org.cneko.justarod.mixin;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    // 禁止攻击方块（左键挖掘）
    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void disableBlockBreaking(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止攻击实体（左键攻击）
    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    private void disableEntityAttack(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止右键使用方块
    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void disableBlockUse(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止右键使用物品
    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
    private void disableItemUse(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean isDisabled() {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        return handler.player.getHandcuffed() > 0;
    }
}
