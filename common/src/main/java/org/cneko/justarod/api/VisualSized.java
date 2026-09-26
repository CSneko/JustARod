package org.cneko.justarod.api;

/**
 * 「渲染尺寸」契约：**模型与碰撞箱差得远**的实体实现它。
 *
 * <p>为什么需要它：体内内容渲染（{@code SlimeContentsLayer}）要把实体缩进史莱姆壳里，
 * 就得知道「这个实体画出来到底多大」。默认做法是量生物模型的包围盒
 * （见 {@code SlimeContentSizing}），但下面这些实体量不出来 / 量不准，必须自己报：
 *
 * <ul>
 *   <li>{@code TamedSlimeEntity}：主模型只剩一张脸，真正的体积在
 *       {@code SlimeShellLayer} 的独立模型里，而且随等级放大（T0 一格、T5 五点五格）；</li>
 *   <li>{@code RodEntity}：GeckoLib 模型（渲染器不是 {@code LivingEntityRenderer}，
 *       量不到模型），而且遗传学还会按基因把长度/宽度继续放大。</li>
 * </ul>
 *
 * <p>本接口**不依赖任何客户端类**（只有一个返回 float 的方法），因此可以让服务端也会加载的
 * 实体类实现它——专用服务器上这些实体同样会被实例化。
 */
public interface VisualSized {

    /**
     * 该实体渲染时**最长的那条边**，单位是格（世界单位）。
     *
     * <p>必须包含渲染器自己做的缩放（等级、遗传学、幼年 0.5 倍…），但**不要**再乘
     * 实体自身的 {@code minecraft:scale} 属性——碰撞箱已经含了它，渲染管线也会再乘一次。
     */
    float visualSize();
}
