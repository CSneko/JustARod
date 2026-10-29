package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.cneko.justarod.client.renderer.empathy.EmpathyAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class EmpathyHandMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void justarod$empathyHands(float partialTick, PoseStack poses, SubmitNodeCollector collector,
                                      LocalPlayer player, int light, CallbackInfo ci) {
        poses.pushPose();
        var reaction = EmpathyAnimation.reaction(player, partialTick);
        if (reaction != null) EmpathyAnimation.firstPerson(poses, reaction, partialTick);
    }
    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void justarod$restoreHands(float partialTick, PoseStack poses, SubmitNodeCollector collector,
                                      LocalPlayer player, int light, CallbackInfo ci) { poses.popPose(); }
}
