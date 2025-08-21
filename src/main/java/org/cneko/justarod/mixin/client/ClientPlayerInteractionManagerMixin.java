package org.cneko.justarod.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
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
    private void disableAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (isDisabled()) {
            ci.cancel();
        }
    }

    // 禁止右键使用方块
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void disableInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (isDisabled()) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    // 禁止右键使用物品
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void disableInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (isDisabled()) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Unique
    private boolean isDisabled() {
        return MinecraftClient.getInstance().player.getHandcuffed() > 0;
    }
}