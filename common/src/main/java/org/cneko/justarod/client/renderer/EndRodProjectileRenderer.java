package org.cneko.justarod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.cneko.justarod.entity.EndRodProjectile;

/** Render the vanilla 3D end rod along its flight direction, rather than as a camera-facing sprite. */
public final class EndRodProjectileRenderer extends EntityRenderer<EndRodProjectile, EndRodProjectileRenderer.State> {
    private final ItemModelResolver resolver;

    public static final class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yaw;
        public float pitch;
    }

    public EndRodProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        resolver = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EndRodProjectile rod, State state, float partialTick) {
        super.extractRenderState(rod, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, rod.yRotO, rod.getYRot());
        state.pitch = Mth.lerp(partialTick, rod.xRotO, rod.getXRot());
        resolver.updateForNonLiving(state.item, rod.getItem(), ItemDisplayContext.NONE, rod);
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poses.mulPose(Axis.XP.rotationDegrees(90 - state.pitch));
        state.item.submit(poses, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poses.popPose();
        super.submit(state, poses, collector, camera);
    }
}
