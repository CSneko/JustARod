package org.cneko.justarod.client.renderer.slime;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.cneko.justarod.api.VisualSized;
import org.cneko.justarod.mixin.client.LivingEntityRendererScaleInvoker;
import org.jetbrains.annotations.Nullable;

/**
 * 「这个实体在缩放 1 倍时画出来多大」——{@link SlimeContentsLayer} 缩放体内内容时用的尺寸测量。
 *
 * <p>为什么要量模型而不是用碰撞箱：旧实现按<b>碰撞箱高度</b>算缩放
 * （{@code scale = 0.6 / (2 × getBbHeight())}），只有「模型高 ≈ 碰撞箱高」的生物才成立。
 * 美西螈就是反例：碰撞箱 0.75×0.42，模型却有 0.5×0.31×1.9 格（又长又扁），
 * 于是它会按 0.42 去缩，画出来足足有外壳的两倍多——「体内实体被异常放大」的根因。
 *
 * <p>测量的来源（按优先级）：
 * <ol>
 *   <li>{@link VisualSized}：实体自己报（模型与碰撞箱差得远的自家实体，例如驯服史莱姆的壳、Rod 的遗传学长度）；</li>
 *   <li>{@link LivingEntityRenderer#getModel()}：量<b>模型包围盒</b>（{@code ModelPart#getExtentsForGui}
 *       会遍历所有立方体的顶点），并先乘上实体自己的 {@code scale} 属性与渲染器自己的
 *       {@code scale(...)}（史莱姆/岩浆怪按体积、幼年 0.5 倍、幻翼按体型…）；</li>
 *   <li>兜底：碰撞箱的较长边（含 {@code scale} 属性）。GeckoLib 的渲染器不是
 *       {@code LivingEntityRenderer}，走这条。</li>
 * </ol>
 *
 * <p>量出来的值与碰撞箱取**较大者**：模型量废了（空模型、怪变换）也不会把内容缩得比碰撞箱还小，
 * 也就是「最差退回旧行为」。{@code @Invoker} 那个入口还额外做了 {@code instanceof} 守卫，
 * 万一哪天 mixin 没注册上，只是量得保守一点，而不是让游戏崩掉。
 */
public final class SlimeContentSizing {

    /** 原版模型空间的约定：+Y 朝下、脚底（地面）在 24 像素 = 1.5 格处 */
    private static final double GROUND_IN_MODEL_SPACE = 1.501D;
    /** 尺寸下限（格）：避免除零与「零尺寸模型」把内容放到天上去 */
    private static final double MIN_DIMENSION = 0.05D;
    /** 底部偏移的绝对值上限（格）：个别渲染器有奇怪的 translate，夹住它免得内容整体偏出空腔 */
    private static final double MAX_BOTTOM_OFFSET = 0.35D;

    private SlimeContentSizing() {
    }

    /**
     * 实体的渲染尺寸（图层空间 = 模型空间，1 单位 = 16 像素 = 1 格）。
     *
     * @param height        竖直方向的高度
     * @param maxHorizontal 水平方向较长的那条边（宽与长取大）
     * @param bottomOffset  模型底部相对实体原点的偏移：正数表示底部在原点上方（正常生物 ≈ 0），
     *                      负数表示模型向下探出原点之外（美西螈那种扁模型）
     */
    public record RenderedSize(double height, double maxHorizontal, double bottomOffset) {

        /** 最长的那条边——缩放时按它来，内容才不会从壳里捅出去 */
        public double maxDimension() {
            return Math.max(this.height, this.maxHorizontal);
        }
    }

    /** 量一个体内实体的渲染尺寸；永远返回可用的值（最差也是碰撞箱） */
    public static RenderedSize measure(EntityRenderer<?, ?> renderer, EntityRenderState state, Entity entity) {
        // 碰撞箱（已含 scale 属性）作为**下限**：模型量不准时也不会把内容画得比碰撞箱还小
        double hitbox = Math.max(MIN_DIMENSION, Math.max(entity.getBbWidth(), entity.getBbHeight()));

        // 1. 实体自己知道（壳在图层里、GeckoLib 模型量不到…）
        if (entity instanceof VisualSized sized) {
            // 注意：实体自己的 scale 属性不算在 visualSize() 里（契约如此），这里补上；
            // 渲染管线（LivingEntityRenderer#submit）同样会乘它。
            double attributeScale = state instanceof LivingEntityRenderState living ? Math.max(0.0F, living.scale) : 1.0D;
            double size = Math.max(MIN_DIMENSION, sized.visualSize() * attributeScale);
            return new RenderedSize(size, size, 0.0D);
        }

        // 2. 生物渲染器：直接量模型包围盒
        if (renderer instanceof LivingEntityRenderer<?, ?, ?> living && state instanceof LivingEntityRenderState livingState) {
            RenderedSize measured = measureModel(living, livingState, hitbox);
            if (measured != null) return measured;
        }

        // 3. 兜底：碰撞箱
        return new RenderedSize(hitbox, hitbox, 0.0D);
    }

    /** 量模型包围盒（含实体 scale 属性 + 渲染器自己的 scale）；量不到返回 null */
    @Nullable
    private static RenderedSize measureModel(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState state, double hitbox) {
        EntityModel<?> model = renderer.getModel();
        if (model == null) return null;
        ModelPart root = model.root();
        // 空模型（GeckoLib 之类借用 LivingEntityRenderer 壳子的情况）没有可量的东西
        if (root == null || root.isEmpty()) return null;

        PoseStack scratch = new PoseStack();
        float attributeScale = state.scale;
        if (attributeScale != 1.0F && attributeScale > 0.0F) {
            scratch.scale(attributeScale, attributeScale, attributeScale);
        }
        // 渲染器自己的缩放：史莱姆/岩浆怪按体积、幼年 0.5、幻翼按体型…
        if (renderer instanceof LivingEntityRendererScaleInvoker invoker) {
            invoker.justarod$applyScale(state, scratch);
        }

        // 包围盒：minX/minY/minZ/maxX/maxY/maxZ
        double[] box = {
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE
        };
        root.getExtentsForGui(scratch, vertex -> {
            box[0] = Math.min(box[0], vertex.x());
            box[1] = Math.min(box[1], vertex.y());
            box[2] = Math.min(box[2], vertex.z());
            box[3] = Math.max(box[3], vertex.x());
            box[4] = Math.max(box[4], vertex.y());
            box[5] = Math.max(box[5], vertex.z());
        });
        if (box[3] < box[0] || box[4] < box[1] || box[5] < box[2]) return null;   // 一个顶点都没有

        double height = Math.max(box[4] - box[1], hitbox);
        double maxHorizontal = Math.max(Math.max(box[3] - box[0], box[5] - box[2]), hitbox);
        // 模型空间 +Y 朝下、脚底在 1.5 格：模型「最低点」是 y 最大的那个顶点，
        // 于是底部相对实体原点 = 1.501 - maxY（正常生物 ≈ 0.001，美西螈那种扁模型是负数）
        double bottomOffset = Mth.clamp(GROUND_IN_MODEL_SPACE - box[4], -MAX_BOTTOM_OFFSET, MAX_BOTTOM_OFFSET);
        return new RenderedSize(Math.max(MIN_DIMENSION, height), Math.max(MIN_DIMENSION, maxHorizontal), bottomOffset);
    }
}
