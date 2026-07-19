package org.cneko.justarod.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    // 禁止左键攻击方块
    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void disableAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (isDisabled()) {
            cir.setReturnValue(false);
        }
    }

    // 禁止左键攻击实体
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void disableAttackEntity(Player player, Entity target, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止右键使用方块
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void disableInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (isDisabled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    // 禁止右键使用物品
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void disableInteractItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (isDisabled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Unique
    private boolean isDisabled() {
        return Minecraft.getInstance().player.getHandcuffed() > 0;
    }
}