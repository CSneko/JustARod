package org.cneko.justarod.client.renderer.slime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.entity.slime.SlimeAbility;

/** A floating, full-bright ability core; cosmetic and independent of storage slots. */
public final class SlimeAbilityLayer extends RenderLayer<SlimeRenderState, JRTamedSlimeModel> {
    private final ItemModelResolver resolver;

    public SlimeAbilityLayer(RenderLayerParent<SlimeRenderState, JRTamedSlimeModel> parent, ItemModelResolver resolver) {
        super(parent);
        this.resolver = resolver;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SlimeRenderState state,
                       float limbSwing, float limbSwingAmount) {
        if (state.isInvisible || state.slime == null || state.ability == SlimeAbility.NONE) return;
        ItemStackRenderState item = new ItemStackRenderState();
        resolver.updateForNonLiving(item, new ItemStack(state.ability.icon()), ItemDisplayContext.FIXED, state.slime);
        // Model Y points down. Put the core behind the face and above the contained passengers.
        pose.pushPose();
        pose.translate(0, 1.12 + Math.sin(state.ageInTicks * 0.09) * 0.015, 0.07);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * (state.ability == SlimeAbility.SHULKER ? 3 : 1.5f)));
        float gained = state.absorptionTicks > 0 ? (float) Math.sin(Math.PI * state.absorptionTicks / 60f) : 0;
        float scale = 0.12f + 0.10f * gained + 0.04f * state.chargeProgress;
        if (state.ability == SlimeAbility.SHULKER && state.pulseAge < 20) scale += 0.07f * (1 - state.pulseAge / 20);
        pose.scale(scale, scale, scale);
        item.submit(pose, collector, 0xF000F0, 0, 0);
        pose.popPose();
    }
}
