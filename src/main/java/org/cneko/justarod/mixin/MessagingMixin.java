package org.cneko.justarod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import org.cneko.justarod.effect.JREffects;
import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
@Mixin(Messaging.class)
public class MessagingMixin {
    @Inject(at = @At("RETURN"), method = "getChatPrefixes",remap = false)
    private static void getChatPrefixes(String playerName, CallbackInfoReturnable<List<String>> cir){
        PlayerEntity player = PlayerUtil.getPlayerByName(playerName);
        if (player!=null){
            if (player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getORGASM_EFFECT()))){
                cir.getReturnValue().add("高潮");
            }
            if (player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getESTRUS_EFFECT()))){
                cir.getReturnValue().add("发情");
            }
        }
    }
}
