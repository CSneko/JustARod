package org.cneko.justarod.mixin;

import net.minecraft.world.entity.Entity;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 被吞进史莱姆体内的实体**不再走原版渲染**（第二道闸，放在公共段）。
 *
 * <p>为什么要两道：内容渲染有两条路——
 * <ol>
 *   <li>{@code SlimeContentsLayer}（图层阶段）：把体内乘客缩小后画进壳里，这是**唯一该被看见**的那份；</li>
 *   <li>原版乘客渲染：实体是真实乘客，原版会把它按 {@code getPassengerAttachmentPoint}
 *       摆在体内、但**按原尺寸**画一遍——于是壳里外各有一份，看起来就是「体内的实体被异常放大」
 *       （美西螈那张截图里身体捅出壳外的，就是这一份）。</li>
 * </ol>
 *
 * <p>{@code SlimePassengerRenderMixin} 已经在 {@code EntityRenderDispatcher#shouldRender}
 * 上拦了一次，但那道闸在 mixin 配置的 {@code "client"} 段里：只要构建产物里的配置是旧的
 * （比如只重编了代码、没重跑 {@code processResources}），这道闸就静默失效，症状原样复现。
 * 所以这里在**公共段**再拦一次——{@code LivingEntity} 的渲染器（{@code EntityRenderer#shouldRender}）
 * 第一步就是问 {@code entity.shouldRender(camX, camY, camZ)}，在这里返回 false 就彻底不会被提取。
 *
 * <p>放在公共段是安全的：{@code Entity.shouldRender} 是公共代码，服务端不调用它
 * （全仓扫描：只有客户端 {@code EntityRenderer} 与烟花/三叉戟这两个子类引用它），
 * 而且本类不引用任何客户端类，专用服务器加载它没有问题。
 */
@Mixin(Entity.class)
public abstract class SlimePassengerCullMixin {

    @Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void justarod$hideSlimeContents(double camX, double camY, double camZ,
                                            CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.getVehicle() instanceof TamedSlimeEntity) {
            cir.setReturnValue(false);
        }
    }
}
