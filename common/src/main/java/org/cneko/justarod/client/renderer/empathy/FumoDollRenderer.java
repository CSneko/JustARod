package org.cneko.justarod.client.renderer.empathy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.cneko.justarod.entity.EmpathyDollEntity;

public final class FumoDollRenderer extends EntityRenderer<EmpathyDollEntity, FumoDollRenderer.State> {
    private final FumoModel wide = new FumoModel(false);
    private final FumoModel slim = new FumoModel(true);

    public static final class State extends EntityRenderState {
        public float yaw;
        public float height = 1;
        public boolean slim;
        public RenderType skin;
    }

    public FumoDollRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(EmpathyDollEntity doll, State state, float partialTick) {
        super.extractRenderState(doll, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, doll.yRotO, doll.getYRot());
        state.height = doll.squeezeHeight(partialTick);
        var profile = doll.getDollStack().get(DataComponents.PROFILE);
        PlayerSkin skin = profile == null ? DefaultPlayerSkin.getDefaultSkin()
                : Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).playerSkin();
        state.skin = RenderTypes.entityTranslucent(skin.body().texturePath());
        state.slim = skin.model() == PlayerModelType.SLIM;
        state.shadowRadius = 0.25f;
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.isInvisible) {
            poses.pushPose();
            poses.mulPose(Axis.YP.rotationDegrees(180 - state.yaw));
            float width = 1 / (float) Math.sqrt(state.height);
            poses.scale(-0.85f * width, -0.85f * state.height, 0.85f * width);
            collector.submitModelPart((state.slim ? slim : wide).root, poses, state.skin, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, null, false, false, -1, null, state.outlineColor);
            poses.popPose();
        }
        super.submit(state, poses, collector, camera);
    }
}
