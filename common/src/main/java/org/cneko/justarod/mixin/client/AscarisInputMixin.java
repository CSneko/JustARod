package org.cneko.justarod.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.cneko.justarod.client.ascaris.AscarisClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 被粉白蛔虫娘尾巴缠住时抑制水平移动输入，防止 WASD 与长尾牵引来回对抗产生抖动。
 */
@Mixin(KeyboardInput.class)
public abstract class AscarisInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void justarod$lockWrappedMove(CallbackInfo ci) {
        if (AscarisClientState.isWrapped()) {
            this.moveVector = Vec2.ZERO;
        }
    }
}
