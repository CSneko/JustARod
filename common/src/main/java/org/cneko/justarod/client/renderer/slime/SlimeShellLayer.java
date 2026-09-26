package org.cneko.justarod.client.renderer.slime;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;

/**
 * 外壳图层：给体内内容罩上那层半透明胶质。
 *
 * <p><b>为什么用「自定义几何」提交，而不是 {@code submitModel}</b>（第十一轮修正）：
 * 26.x 的提交是按阶段画出来的，{@code FeatureRenderDispatcher} 的顺序是
 *
 * <pre>
 * renderSolidFeatures():      Shadow → ModelFeatureRenderer(实体模型，含体内内容) → … → CustomFeatureRenderer
 * renderTranslucentFeatures(): Shadow → ModelFeatureRenderer(半透明模型) → ModelPart → NameTag → Text
 *                              → Item → Block → CustomFeatureRenderer(← 这里)
 * </pre>
 *
 * <p>于是有两个不同的批次，谁先谁后是**引擎定死的**，不是我们提交的先后：
 * <ul>
 *   <li>普通生物的模型走**实体渲染类型**（cutout，进 renderSolidFeatures）→ 永远排在外壳前面，
 *       所以体内普通实体一直看得见（第九/十轮之后的表现）；</li>
 *   <li>**玩家的模型是半透明的**（皮肤要支持半透明像素，{@code AvatarRenderer} 用
 *       {@code entityTranslucent}）→ 和外壳挤在同一个 {@code renderTranslucent} 批次里，
 *       而那个批次按「到相机的距离」排序：外壳的提交点是史莱姆脚下、玩家的提交点在体内偏上，
 *       结果外壳经常先画 → 外壳写下的深度把体内的自己整块挡掉
 *       —— 这正是「第三人称看不见自己，除非把镜头转进壳里」的原因。</li>
 * </ul>
 *
 * <p>改成 {@code submitCustomGeometry} 后，外壳落在 <b>CustomFeatureRenderer</b>：它排在
 * 半透明模型之后，也就是**必定后画**。于是：内容永远先画、外壳永远只负责罩色，
 * 既不会被挡住，又保留「泡在胶质里」的观感（贴图 alpha 180/255 ≈ 0.71，再乘
 * {@link #SHELL_TINT} 的 0.5 → 内容约 64% 可见）。
 *
 * <p>自定义几何的回调给的是 {@code PoseStack.Pose} + 已经绑好渲染类型的
 * {@code VertexConsumer}，所以这里把 pose 拷进一个临时 {@code PoseStack}，
 * 直接让外壳模型往那个 consumer 里画（同一个渲染类型、同样的光照与 overlay）。
 */
public class SlimeShellLayer extends RenderLayer<SlimeRenderState, JRTamedSlimeModel> {

    /** 外壳整体透明度系数（乘在贴图 alpha 上） */
    private static final int SHELL_TINT = ARGB.color(128, 255, 255, 255);

    private final JRTamedSlimeShellModel shellModel;

    public SlimeShellLayer(RenderLayerParent<SlimeRenderState, JRTamedSlimeModel> parent,
                           JRTamedSlimeShellModel shellModel) {
        super(parent);
        this.shellModel = shellModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       SlimeRenderState state, float limbSwing, float limbSwingAmount) {
        if (state.isInvisible) return;
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        collector.submitCustomGeometry(poseStack,
                RenderTypes.entityTranslucent(JRTamedSlimeRenderer.TEXTURE),
                (pose, consumer) -> {
                    // 回调是延迟执行的（真正绘制时），所以只捕获数值、不捕获状态
                    PoseStack local = new PoseStack();
                    local.last().set(pose);
                    this.shellModel.root().render(local, consumer, lightCoords, overlayCoords, SHELL_TINT);
                });
    }
}
