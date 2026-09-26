package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 把 {@code LivingEntityRenderer#scale(S, PoseStack)} 暴露成可调用方法。
 *
 * <p>为什么需要：体内内容要按「实体实际画出来多大」缩放（见
 * {@code SlimeContentSizing}）。模型包围盒能量出来，但**渲染器自己加的缩放量不出来**——
 * 史莱姆/岩浆怪按体积、幼年 0.5 倍、幻翼按体型、驯服史莱姆按等级，
 * 全都写在 {@code scale(...)} 里（protected，跨包调不到）。
 *
 * <p>因为 {@code @Invoker} 生成的是虚调用，子类覆写的 {@code scale} 会正常生效
 * （例如 {@code JRTamedSlimeRenderer} 的 {@code blockSize * 2}）。
 * 调用方给的是一个临时 {@link PoseStack}，不会影响正常渲染。
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererScaleInvoker {

    @Invoker("scale")
    void justarod$applyScale(LivingEntityRenderState state, PoseStack poseStack);
}
