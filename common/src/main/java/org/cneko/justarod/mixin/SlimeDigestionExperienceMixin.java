package org.cneko.justarod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.entity.slime.SlimeDigestionLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public class SlimeDigestionExperienceMixin {
    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private static void absorbDigestionReward(ServerLevel level, Vec3 position, int amount, CallbackInfo ci) {
        if (SlimeDigestionLoot.absorbExperience(level, amount)) ci.cancel();
    }
}
