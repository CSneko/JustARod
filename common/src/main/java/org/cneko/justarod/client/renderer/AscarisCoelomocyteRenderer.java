package org.cneko.justarod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.body.AscarisCoelomocyteEntity;

/**
 * 粉白体腔免疫小细胞渲染器：
 * 半透明柔光外膜 + 内部缓慢旋转的珍珠发光细胞核 + 4 枚微摆伪足小突起，随呼吸节律柔弹起伏。
 */
public final class AscarisCoelomocyteRenderer extends EntityRenderer<AscarisCoelomocyteEntity, AscarisCoelomocyteRenderer.State> {

    private static final Identifier TEX_FRIENDLY = Identifier.fromNamespaceAndPath(Justarod.MODID, "textures/entity/ascaris_coelomocyte.png");
    private static final Identifier TEX_ALERT = Identifier.fromNamespaceAndPath(Justarod.MODID, "textures/entity/ascaris_coelomocyte_alert.png");

    private final ModelPart outerShell;
    private final ModelPart nucleus;

    public static final class State extends EntityRenderState {
        float age;
        float phase;
        boolean alert;
        boolean happy;
    }

    public AscarisCoelomocyteRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.22f;

        // 半透明果冻外膜 + 4 枚可爱星状伪足小突起（UV 0..32, 0..16）
        MeshDefinition shellMesh = new MeshDefinition();
        var root = shellMesh.getRoot();
        root.addOrReplaceChild("membrane", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.ZERO);
        root.addOrReplaceChild("nub_n", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.5f, -1.5f, -5.5f, 3.0f, 3.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild("nub_s", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.5f, -1.5f, 3.5f, 3.0f, 3.0f, 2.0f), PartPose.ZERO);
        root.addOrReplaceChild("nub_e", CubeListBuilder.create().texOffs(0, 0)
                .addBox(3.5f, -1.5f, -1.5f, 2.0f, 3.0f, 3.0f), PartPose.ZERO);
        root.addOrReplaceChild("nub_w", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.5f, -1.5f, -1.5f, 2.0f, 3.0f, 3.0f), PartPose.ZERO);
        this.outerShell = LayerDefinition.create(shellMesh, 32, 32).bakeRoot();

        // 内部温暖发亮的珍珠细胞核（UV 0..32, 16..32）
        MeshDefinition coreMesh = new MeshDefinition();
        coreMesh.getRoot().addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2.0f, -2.0f, -2.0f, 4.0f, 4.0f, 4.0f), PartPose.ZERO);
        this.nucleus = LayerDefinition.create(coreMesh, 32, 32).bakeRoot();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AscarisCoelomocyteEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.phase = entity.getPhase();
        state.alert = entity.isAlert();
        state.happy = entity.isHappy();
    }

    @Override
    public void submit(State s, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        Identifier tex = s.alert ? TEX_ALERT : TEX_FRIENDLY;
        RenderType type = RenderTypes.entityTranslucentEmissive(tex);
        float t = s.age * (s.alert ? 0.24f : (s.happy ? 0.18f : 0.10f)) + s.phase;
        float bob = Mth.sin(t) * 0.08f;
        float squish = 1.0f + Mth.sin(t * 1.35f) * (s.happy ? 0.12f : 0.065f);
        float invSquish = 1.0f / Mth.sqrt(squish);

        // 1. 内部缓慢旋转的珍珠发光核
        int fullBright = 0x00F000F0;
        poses.pushPose();
        poses.translate(0.0, 0.30 + bob, 0.0);
        poses.mulPose(Axis.YP.rotationDegrees(s.age * (s.alert ? 5.0f : 2.0f)));
        poses.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.7f) * 20f));
        collector.submitModelPart(this.nucleus, poses, type, fullBright,
                OverlayTexture.NO_OVERLAY, null, false, false, -1, null, s.outlineColor);
        poses.popPose();

        // 2. 外部半透果冻膜与星状伪足（呼吸柔弹）
        poses.pushPose();
        poses.translate(0.0, 0.30 + bob, 0.0);
        poses.mulPose(Axis.YP.rotationDegrees(-s.age * (s.alert ? 3.2f : 1.1f)));
        poses.scale(invSquish, squish, invSquish);
        collector.submitModelPart(this.outerShell, poses, type, fullBright,
                OverlayTexture.NO_OVERLAY, null, false, false, -1, null, s.outlineColor);
        poses.popPose();

        super.submit(s, poses, collector, camera);
    }
}
