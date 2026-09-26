package org.cneko.justarod.client.renderer.slime;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

/**
 * 驯服史莱姆渲染器。
 *
 * <p>26.x 已移除原版 SlimeRenderer，渲染统一走
 * {@code MobRenderer + EntityModel + RenderLayer}，模型在模型空间里是 1 格见方，
 * 因此这里按等级做统一缩放：{@code scale = blockSize}（level0=1 格、level5=5.5 格）。
 * 体内内容由 {@link SlimeContentsLayer} 负责。
 */
public class JRTamedSlimeRenderer extends MobRenderer<TamedSlimeEntity, SlimeRenderState, JRTamedSlimeModel> {

    /** 直接复用原版史莱姆贴图，不额外提供本模组贴图 */
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/slime/slime.png");

    public JRTamedSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new JRTamedSlimeModel(context.bakeLayer(JRTamedSlimeModel.LAYER)), 0.5F);
        // 顺序很重要：内容是「先画进去」，外壳最后叠加颜色（见 JRTamedSlimeShellModel 的说明）
        this.addLayer(new SlimeContentsLayer(this, context.getItemModelResolver()));
        this.addLayer(new SlimeShellLayer(this, new JRTamedSlimeShellModel(
                context.bakeLayer(JRTamedSlimeShellModel.LAYER))));
    }

    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    @Override
    public void extractRenderState(TamedSlimeEntity entity, SlimeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.blockSize = entity.tier().blockSize();
        state.tierLevel = entity.getTierLevel();
        state.decomposeState = entity.getDecomposeState();
        state.decomposeProgress = entity.getDecomposeProgress();
        // 吞噬技能的放大动画：用 partialTick 插值，30 tick 的动画才不会一卡一卡
        state.devourScale = entity.devourScale(partialTick);
        // 体内内容渲染层需要实体本身（乘客实体 + 体内物品列表）
        state.slime = entity;
    }

    @Override
    protected void scale(SlimeRenderState state, PoseStack poseStack) {
        // 模型空间里 16 像素 = 1 格，而外壳用的是原版那张贴图要求的 8 像素立方体
        // （每面 8x8 贴图像素，见 §9.2）—— 也就是说**外壳在模型空间里只有 0.5 格**。
        // 要让外观边长真的等于 blockSize 格（设计表里的「实际边长」：level0=1 格、level5=5.5 格），
        // 缩放必须是 blockSize * 2。
        //
        // 之前只乘了 blockSize，外观只有设计值的一半（T3 = 1.43 格而不是 2.85 格）：
        //   * 乘客挂载点按 blockSize 算（0.45 × 2.85 = 1.28 格），直接落到壳顶之上
        //     → 看起来像「玩家站在史莱姆头上」，也就是这次截图里的问题；
        //   * 粒子/空腔中心（blockSize * 0.5）也随之偏到壳外。
        //
        // 再乘 devourScale 就是手动「吞噬」技能的「先放大到 4 倍」：
        // 纯渲染倍数，碰撞箱/挂载点/容量都不动（需求确认「仅视觉效果」）。
        float size = Math.max(0.2F, state.blockSize) * 2.0F * Math.max(1.0F, state.devourScale);
        poseStack.scale(size, size, size);
    }

    @Override
    public Identifier getTextureLocation(SlimeRenderState state) {
        return TEXTURE;
    }

}
