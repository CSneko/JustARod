package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.client.disassembly.DisassemblyClient;
import org.cneko.justarod.disassembly.BodyPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class DisassemblyHeldItemMixin {
    @Inject(method="submitArmWithItem",at=@At("HEAD"),cancellable=true)
    private void justarod$hideHeldItem(ArmedEntityRenderState state,ItemStackRenderState item,ItemStack stack,HumanoidArm arm,
            PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar && DisassemblyClient.missing(avatar.id,
                arm==HumanoidArm.LEFT?BodyPart.LEFT_ARM:BodyPart.RIGHT_ARM)) ci.cancel();
    }
}
