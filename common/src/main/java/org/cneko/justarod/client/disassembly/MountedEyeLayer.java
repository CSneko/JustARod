package org.cneko.justarod.client.disassembly;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import org.cneko.justarod.disassembly.*;

/** Mounted eyes sit on the chest, so they stay attached even when the host removes their own head. */
public final class MountedEyeLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    private final DetachedPartModel left=new DetachedPartModel(BodyPart.LEFT_EYE,false),right=new DetachedPartModel(BodyPart.RIGHT_EYE,false);
    public MountedEyeLayer(RenderLayerParent<AvatarRenderState,PlayerModel> parent) { super(parent); }
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float a,float b) {
        if (state.isInvisible) return;
        for (var eye:DisassemblyClient.state(state.id).eyes()) {
            if (!(eye.getItem() instanceof DetachedPartItem item) || !item.part.eye()) continue;
            var profile=eye.get(DataComponents.PROFILE);
            var skin=profile==null?DefaultPlayerSkin.getDefaultSkin():Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).playerSkin();
            poses.pushPose(); getParentModel().body.translateAndRotate(poses);
            poses.translate(item.part==BodyPart.LEFT_EYE?0.14:-0.14,0.2,-0.17);
            collector.submitModelPart((item.part==BodyPart.LEFT_EYE?left:right).root,poses,RenderTypes.entityTranslucent(skin.body().texturePath()),
                    light,OverlayTexture.NO_OVERLAY,null,false,false,-1,null,state.outlineColor);
            poses.popPose();
        }
    }
}
