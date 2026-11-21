package org.cneko.justarod.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.*;
import org.cneko.justarod.client.disassembly.DisassemblyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class DisassemblyModelMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("TAIL"))
    private void justarod$hideParts(HumanoidRenderState state,CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar) DisassemblyClient.pose((HumanoidModel<?>)(Object)this,avatar);
    }
}
