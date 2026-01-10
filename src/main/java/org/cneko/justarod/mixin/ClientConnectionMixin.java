package org.cneko.justarod.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(
            method = "exceptionCaught",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onExceptionCaught(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
        // 立刻关闭连接，防止异常包反复消耗性能
        context.close();

        // 阻止原版继续处理（writeAndFlush / 日志等）
        ci.cancel();
    }
}