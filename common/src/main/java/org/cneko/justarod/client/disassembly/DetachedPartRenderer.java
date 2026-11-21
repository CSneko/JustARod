package org.cneko.justarod.client.disassembly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;
import org.cneko.justarod.disassembly.*;
import java.util.EnumMap;

public final class DetachedPartRenderer extends EntityRenderer<DetachedPartEntity, DetachedPartRenderer.State> {
    private final EnumMap<BodyPart,DetachedPartModel> wide = new EnumMap<>(BodyPart.class), slim = new EnumMap<>(BodyPart.class);
    private final ItemModelResolver items;
    public static final class State extends EntityRenderState {
        BodyPart part = BodyPart.HEAD;
        float yaw, age;
        int mode;
        boolean slim, talking;
        RenderType skin;
        final ItemStackRenderState tool = new ItemStackRenderState();
    }
    public DetachedPartRenderer(EntityRendererProvider.Context context) {
        super(context); items = context.getItemModelResolver();
        for (BodyPart part : BodyPart.values()) { wide.put(part,new DetachedPartModel(part,false)); slim.put(part,new DetachedPartModel(part,true)); }
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(DetachedPartEntity entity, State state, float partialTick) {
        super.extractRenderState(entity,state,partialTick);
        state.part = entity.part(); state.yaw = Mth.rotLerp(partialTick,entity.yRotO,entity.getYRot());
        state.age = entity.tickCount + partialTick; state.mode = entity.mode(); state.talking = entity.isCustomNameVisible();
        var profile = entity.stack().get(DataComponents.PROFILE);
        var skin = profile == null ? DefaultPlayerSkin.getDefaultSkin() : Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).playerSkin();
        state.slim = skin.model() == PlayerModelType.SLIM;
        state.skin = RenderTypes.entityTranslucent(skin.body().texturePath()); state.shadowRadius = state.part.eye()?0.08f:0.2f;
        items.updateForNonLiving(state.tool,entity.tool(),ItemDisplayContext.GROUND,entity);
    }
    @Override public void submit(State s, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!s.isInvisible) {
            poses.pushPose(); poses.mulPose(Axis.YP.rotationDegrees(180-s.yaw));
            if (s.mode == DetachedPartEntity.WORK) poses.mulPose(Axis.XP.rotationDegrees((float)Math.sin(s.age*0.8)*25));
            if (s.talking && s.part == BodyPart.HEAD) poses.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(s.age*0.6)*5));
            poses.scale(-1,-1,1);
            var model = (s.slim?slim:wide).get(s.part);
            if (s.part == BodyPart.HEAD) {
                boolean blink = (s.age % 85) < 3;
                model.root.getChild("left_lid").visible = blink;
                model.root.getChild("right_lid").visible = blink;
            }
            collector.submitModelPart(model.root,poses,s.skin,s.lightCoords,OverlayTexture.NO_OVERLAY,null,false,false,-1,null,s.outlineColor);
            poses.popPose();
            if (!s.tool.isEmpty()) {
                poses.pushPose(); poses.translate(0,0.25,0); poses.mulPose(Axis.YP.rotationDegrees(-s.yaw));
                if (s.mode == DetachedPartEntity.WORK) poses.mulPose(Axis.XP.rotationDegrees((float)Math.sin(s.age*0.8)*40));
                s.tool.submit(poses,collector,s.lightCoords,OverlayTexture.NO_OVERLAY,s.outlineColor); poses.popPose();
            }
        }
        super.submit(s,poses,collector,camera);
    }
}
