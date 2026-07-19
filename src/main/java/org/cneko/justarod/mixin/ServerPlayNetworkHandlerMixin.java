package org.cneko.justarod.mixin;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {
    // 禁止攻击方块（左键挖掘）
    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void disableBlockBreaking(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止攻击实体（左键攻击）
    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    private void disableEntityAttack(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止右键使用方块
    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void disableBlockUse(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止右键使用物品
    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
    private void disableItemUse(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean isDisabled() {
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        return handler.player.getHandcuffed() > 0;
    }
}
