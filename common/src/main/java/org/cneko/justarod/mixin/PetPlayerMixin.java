package org.cneko.justarod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cneko.justarod.pet.PetState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PetPlayerMixin implements PetState.Holder {
    @Unique private final PetState justarod$pet = new PetState();
    @Override public PetState justarod$petState() { return justarod$pet; }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void justarod$savePet(ValueOutput output, CallbackInfo ci) { justarod$pet.save(output); }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void justarod$loadPet(ValueInput input, CallbackInfo ci) { justarod$pet.load(input); }
}
