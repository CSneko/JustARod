package org.cneko.justarod.client.renderer.slime;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/**
 * 外壳模型（那层半透明胶质），**单独成模型是为了提交顺序**。
 *
 * <p>{@code RenderPipelines.ENTITY_TRANSLUCENT} 用的是 {@code DepthStencilState.DEFAULT}
 * ——也就是**会写深度**。所以「谁先提交谁就挡住后面的东西」：
 * 外壳先画 → 之后画的体内内容全部被深度测试挡掉（第三人称看不见体内任何东西的根因）。
 *
 * <p>现在的提交顺序是：主模型（脸）→ {@link SlimeContentsLayer}（体内内容）→ 本模型（外壳）。
 * 外壳最后画，只负责给内容罩一层颜色，看起来就是「泡在胶质里」。
 */
public class JRTamedSlimeShellModel extends EntityModel<LivingEntityRenderState> {

    /** 外壳边长（像素）：8 像素 = 0.5 格，与原版贴图每面 8x8 对应 */
    private static final float SHELL = 8.0F;

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Justarod.MODID, "tamed_slime_shell"), "main");

    public JRTamedSlimeShellModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-SHELL / 2, 16.0F, -SHELL / 2, SHELL, SHELL, SHELL),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
