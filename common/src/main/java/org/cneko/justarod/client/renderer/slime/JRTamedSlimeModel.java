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
 * 驯服史莱姆模型：**直接复用原版史莱姆贴图**
 * （{@code minecraft:textures/entity/slime/slime.png}，无需本模组自带贴图）。
 *
 * <p>尺寸规则（由原版 bake 结果反推，并用 {@code :fabric:dumpSlimeModel} 实测确认）：
 * <ul>
 *   <li>模型空间里 <b>16 像素 = 1 格</b>，Y=16 对应地面（原版外壳 cube 为 y=16..24，
 *       即 0.5 格高、对应 size-1 史莱姆的 0.5 格碰撞箱）</li>
 *   <li>贴图密度是 <b>1 模型像素 = 1 贴图像素</b>：外壳边长必须用 8 像素
 *       （{@code texOffs(0,0)} → 每面正好 8x8），这里取 {@code 8 × blockSize}，
 *       渲染器再按 blockSize 缩放 → 外壳边长恰好等于碰撞箱边长</li>
 *   <li>眼睛/嘴是<b>单独的立方体</b>（原版放在内层模型里），不是把外壳贴图整体拉伸</li>
 * </ul>
 *
 * <p>眼睛与嘴的贴图坐标完全照抄原版内层：右眼 (32,0)、左眼 (32,4)、嘴 (32,8)
 * （这三块是原版贴图里真正的深色眼睛/嘴巴像素；写成别的坐标会落在透明区域、脸上什么都没有）。
 * 体内内容靠外壳半透明透出来，与原版史莱姆「一层胶质壳」的观感一致。
 */
public class JRTamedSlimeModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart inner;

    public JRTamedSlimeModel(ModelPart root) {
        // 脸用**不透明**渲染类型（cutout）：眼睛/嘴是贴图里的实心像素，效果与半透明一致，
        // 但 cutout 会丢弃完全透明的像素 → 不会在脸的位置写下一片「看不见的深度」
        // （半透明批次里那些深度会把体内内容抠出几个小洞）。
        // 顺带：cutout 走的是实体渲染类型的「实心」批次，永远排在外壳（自定义几何）之前。
        super(root, RenderTypes::entityCutout);
        // 注意：外壳已经搬去 JRTamedSlimeShellModel，这里**不能**再去 getChild("outer")，
        // 否则烘焙出来的根节点里没有这个名字会直接抛异常
        this.inner = root.getChild("inner");
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // 刻意不做「呼吸式挤压」：外壳缩放会让贴在壳上的眼睛/嘴陷进去或凸出来。
        // 原版史莱姆的 squish 是作用在整套模型 + 渲染状态上的，单体模型下做不安全。
    }

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Justarod.MODID, "tamed_slime"), "main");

    /**
     * 主模型：**只有脸**（眼睛 + 嘴巴）。
     *
     * <p>两个刻意的取舍（都是为了让体内内容真的看得见，见文档 §9.17）：
     * <ul>
     *   <li>**外壳搬去 {@link JRTamedSlimeShellModel}**：它在图层里、内容之后提交，
     *       这样它只是给内容罩一层颜色；留在主模型里的话它会先写深度，
     *       之后提交的内容全部被深度测试挡掉（第三人称什么都看不见的根因）；</li>
     *   <li>**删掉内核 cube**：内核（6 像素 = 0.375 格）比体内内容还大，
     *       会把内容的**整个轮廓**罩住 → 同样用深度把内容挡掉；
     *       少了它以后脸也能透出来了（以前它一直被外壳的深度挡着，游戏里根本看不到脸）。</li>
     * </ul>
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();

        // ===== 脸（挂在 inner 下，方便统一缩放）=====
        var inner = root.addOrReplaceChild("inner", CubeListBuilder.create(), PartPose.ZERO);

        // 右眼 / 左眼 / 嘴：与原版内层逐位一致（贴图坐标 (32,0) / (32,4) / (32,8)）
        inner.addOrReplaceChild("right_eye",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-3.25F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        inner.addOrReplaceChild("left_eye",
                CubeListBuilder.create().texOffs(32, 4)
                        .addBox(1.25F, 18.0F, -3.5F, 2.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        inner.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-1.0F, 21.0F, -3.5F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }
}
