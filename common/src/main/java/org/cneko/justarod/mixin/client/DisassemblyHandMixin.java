package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.client.disassembly.DisassemblyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class DisassemblyHandMixin {
    @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
    private void justarod$missingHand(AbstractClientPlayer player,float partialTick,float pitch,InteractionHand hand,float swing,
            ItemStack stack,float equip,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo ci) {
        HumanoidArm arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        if (DisassemblyClient.remoteFrame() || DisassemblyClient.missingArm(arm)) ci.cancel();
    }
    @Inject(method="renderPlayerArm",at=@At("HEAD"),cancellable=true)
    private void justarod$missingBareHand(PoseStack poses,SubmitNodeCollector collector,int light,float equip,float swing,HumanoidArm arm,CallbackInfo ci) {
        if (DisassemblyClient.remoteFrame() || DisassemblyClient.missingArm(arm)) ci.cancel();
    }
}
