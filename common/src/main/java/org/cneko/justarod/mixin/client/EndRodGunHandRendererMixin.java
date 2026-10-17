package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.client.renderer.EndRodGunAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class EndRodGunHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void justarod$holdGun(LivingEntity player, ItemStack stack, ItemDisplayContext display,
                                  PoseStack poses, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!EndRodGunAnimation.isGunView(player, stack, display)) return;
        poses.pushPose();
        EndRodGunAnimation.poseFirstPerson(poses, player, display,
                Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void justarod$restoreHandPose(LivingEntity player, ItemStack stack, ItemDisplayContext display,
                                         PoseStack poses, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (EndRodGunAnimation.isGunView(player, stack, display)) poses.popPose();
    }
}
