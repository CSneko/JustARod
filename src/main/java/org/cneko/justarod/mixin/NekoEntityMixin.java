package org.cneko.justarod.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.JRAttributes;
import org.cneko.justarod.entity.Insertable;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NekoEntity.class)
public abstract class NekoEntityMixin implements Insertable{
    @Shadow public abstract LivingEntity getEntity();

    @Unique
    private ItemStack rodInside = ItemStack.EMPTY;
    @Override
    public ItemStack getRodInside() {
        return rodInside;
    }

    @Override
    public void setRodInside(@NotNull ItemStack rodInside) {
        this.rodInside = rodInside;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void tick(CallbackInfo ci) {
        if (this.rodInside != null) {
            this.tickInside((NekoEntity)(Object)this);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("rodInside")) {
            var rod = ItemStack.parse(this.getEntity().registryAccess(),nbt.getCompound("rodInside"));
            rod.ifPresent(this::setRodInside);
        }
        if (this.getEntity() instanceof Pregnant pregnant){
            pregnant.readPregnantFromNbt(nbt);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void addAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        if (!getRodInside().isEmpty()) {
            nbt.put("rodInside", getRodInside().save(
                    this.getEntity().registryAccess()
            ));
        }
        if (this.getEntity() instanceof Pregnant pregnant){
            pregnant.writePregnantToNbt(nbt);
        }
    }
    @Inject(method = "createNekoAttributes",at = @At("RETURN"))
    private static void createNekoAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(JRAttributes.Companion.getPLAYER_LUBRICATING(), 1);
    }
}
