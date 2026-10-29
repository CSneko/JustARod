package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.cneko.justarod.client.renderer.empathy.EmpathyAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EmpathyPlayerRenderMixin {
    @Inject(method = "submit", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void justarod$beginReaction(EntityRenderState state, CameraRenderState camera, double x, double y,
                                       double z, PoseStack poses, SubmitNodeCollector collector, CallbackInfo ci) {
        // Always balance pushes, including when the reaction expires at a frame boundary.
        poses.pushPose();
        if (state instanceof AvatarRenderState avatar) {
            var reaction = EmpathyAnimation.reaction(avatar);
            if (reaction != null) EmpathyAnimation.transform(poses, reaction, EmpathyAnimation.partialTick());
        }
    }

    @Inject(method = "submit", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", shift = At.Shift.AFTER))
    private void justarod$endReaction(EntityRenderState state, CameraRenderState camera, double x, double y,
                                     double z, PoseStack poses, SubmitNodeCollector collector, CallbackInfo ci) {
        poses.popPose();
    }
}
