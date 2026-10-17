package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.cneko.justarod.client.renderer.EntitySquashAnimation;
import org.cneko.justarod.config.JRConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntitySquashRenderMixin {
    @Unique
    private float justarod$squashHeight = 1f;

    @Unique
    private float justarod$squashRotation = 0f;

    @Unique
    private final EntitySquashAnimation.Clock justarod$animationClock = new EntitySquashAnimation.Clock();

    @Unique
    private ClientLevel justarod$animationLevel;

    @Inject(method = "prepare", at = @At("HEAD"))
    private void justarod$prepareSquash(Camera camera, Entity crosshairEntity, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        justarod$squashHeight = 1f;
        justarod$squashRotation = 0f;
        if (!JRConfig.isEntitySquashEnabled() || minecraft.level == null) {
            justarod$animationClock.reset();
            justarod$animationLevel = null;
            return;
        }
        if (justarod$animationLevel != minecraft.level) {
            justarod$animationClock.reset();
            justarod$animationLevel = minecraft.level;
        }

        // Sample once per frame so even entities submitted across a beat boundary stay in sync.
        float bpm = JRConfig.getEntitySquashBpm();
        double phase = justarod$animationClock.sample(minecraft.level.getGameTime(),
                minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), System.nanoTime(), bpm,
                minecraft.isPaused() || minecraft.level.tickRateManager().isFrozen());
        justarod$squashHeight = EntitySquashAnimation.heightScaleForPhase(phase,
                JRConfig.getEntitySquashCompressionRatio());
        justarod$squashRotation = EntitySquashAnimation.rotationDegreesForPhase(phase);
    }

    @Inject(method = "submit", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void justarod$beginSquash(EntityRenderState state, CameraRenderState camera,
                                      double x, double y, double z, PoseStack pose,
                                      SubmitNodeCollector collector, CallbackInfo ci) {
        if (justarod$squashHeight == 1f && justarod$squashRotation == 0f) return;
        // Dispatcher has already translated to the entity's feet; keep its world position fixed.
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(justarod$squashRotation));
        float width = 1f / justarod$squashHeight;
        pose.scale(width, justarod$squashHeight, width);
    }

    @Inject(method = "submit", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            shift = At.Shift.AFTER))
    private void justarod$endSquash(EntityRenderState state, CameraRenderState camera,
                                    double x, double y, double z, PoseStack pose,
                                    SubmitNodeCollector collector, CallbackInfo ci) {
        if (justarod$squashHeight != 1f || justarod$squashRotation != 0f) pose.popPose();
    }
}
