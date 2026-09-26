package org.cneko.justarod.client.renderer.slime;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

/**
 * 驯服史莱姆的渲染状态：除原版生物状态外，额外带上等级、体积与体内内容信息。
 */
public class SlimeRenderState extends LivingEntityRenderState {

    /** 碰撞箱边长（格），渲染器按此统一缩放模型 */
    public float blockSize = 1.0F;
    public int tierLevel = 0;
    public int decomposeState = 0;
    public float decomposeProgress = 0.0F;
    /**
     * 手动「吞噬」技能的放大倍数（1.0 = 原始大小，峰值 4.0）。
     *
     * <p>这个值同时被三方使用，缺一不可：
     * <ul>
     *   <li>渲染器把它乘进模型缩放（外观变大）；</li>
     *   <li>{@link SlimeContentsLayer} 要把它**除掉**——那一层原本用
     *       {@code 1/(2*blockSize)} 抵消渲染器缩放来还原「原尺寸」，
     *       现在渲染器多乘了一个放大倍数，所以那里也要跟着除，
     *       否则壳张开了、体内的东西还是原来那么大，看着像壳在漏气；</li>
     *   <li>外壳图层不用管：它复用同一个 poseStack，自动跟着放大。</li>
     * </ul>
     */
    public float devourScale = 1.0F;
    /** 仅渲染期使用：当前正在渲染的史莱姆（体内内容层需要它取乘客与物品） */
    public TamedSlimeEntity slime;
}
