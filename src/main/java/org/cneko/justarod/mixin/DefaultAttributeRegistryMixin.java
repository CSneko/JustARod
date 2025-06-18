package org.cneko.justarod.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import org.cneko.justarod.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 你不会闲到连mixin的注释都要看吧
@Mixin(DefaultAttributeRegistry.class)
public class DefaultAttributeRegistryMixin {
    @Inject(method = "get",at = @At("HEAD"), cancellable = true)
    private static void onGet(EntityType<? extends LivingEntity> type, CallbackInfoReturnable<DefaultAttributeContainer> cir) {
        if (type == JREntities.LOLI_NEKO){
            cir.setReturnValue(LoliNekoEntity.createNekoAttributes().build());
        } else if (type == JREntities.SEEEEEX_NEKO) {
            cir.setReturnValue(SeeeeexNekoEntity.createNekoAttributes().build());
        } else if (type == JREntities.ROD) {
            cir.setReturnValue(RodEntity.createMobAttributes().build());
        }
    }
}
