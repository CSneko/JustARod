package org.cneko.justarod.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemDisplayContext;
import org.cneko.justarod.client.feature.*;
import org.cneko.justarod.entity.BDSMable;
import org.cneko.justarod.item.JRItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.x：PlayerRenderer 改名为 AvatarRenderer（构造参数 (Context, boolean) 不变）。
 * 在 extract 阶段捕获口球等饰品状态，供提交式渲染层在 submit 阶段使用。
 */
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        AvatarRenderer self = (AvatarRenderer) (Object) this;
        self.addLayer(new RashFeatureRenderer(self));
        self.addLayer(new BallMouthFeatureRenderer(self));
        self.addLayer(new ElectricShockFeatureRenderer(self));
        self.addLayer(new BundledFeatureRenderer(self));
        self.addLayer(new EyePatchFeatureRenderer(self));
        self.addLayer(new EarplugFeatureRenderer(self));
        self.addLayer(new HandcuffFeatureRenderer(self));
        self.addLayer(new ShacklesFeatureRenderer(self));
        self.addLayer(new DarkCirclesFeatureRenderer(self));
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void justarod$captureBallMouth(Avatar player, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        BallMouthRenderStateCache.remove(state);
        if (player instanceof BDSMable bdsm && bdsm.getBallMouth() > 0) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForNonLiving(
                    itemState,
                    JRItems.Companion.getBALL_MOUTH().getDefaultInstance(),
                    ItemDisplayContext.FIXED,
                    player
            );
            BallMouthRenderStateCache.put(state, itemState);
        }
    }
}
