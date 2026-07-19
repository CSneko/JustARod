package org.cneko.justarod.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.JRAttributes;
import org.cneko.justarod.entity.BDSMable;
import org.cneko.justarod.entity.Insertable;
import org.cneko.justarod.entity.Pregnant;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements Insertable {

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


    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (nbt.contains("rodInside")) {
            var rod = ItemStack.parse(self.registryAccess(),nbt.getCompound("rodInside"));
            rod.ifPresent(this::setRodInside);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void addAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!getRodInside().isEmpty()) {
            nbt.put("rodInside", getRodInside().save(
                    self.registryAccess()
            ));
        }
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!getRodInside().isEmpty()) {
            this.tickInside(self);
        }
    }

    @Inject(method = "createLivingAttributes",at = @At("RETURN"))
    private static void createLivingAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        AttributeSupplier.Builder builder = cir.getReturnValue();
        builder.add(JRAttributes.Companion.getPLAYER_LUBRICATING());
        builder.add(JRAttributes.Companion.getGENERIC_MAX_POWER());
    }

    @Inject(method = "damage", at = @At("HEAD"))
    public void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Pregnant pregnant && amount >=7 && pregnant.isPregnant()){
            // 流产
            pregnant.miscarry();
        }
        // 甲沟炎"磕到"触发
        if (self instanceof Pregnant pregnant && pregnant.getParonychia() > 0 && amount >= 1.0f) {
            int bumpChance = pregnant.getParonychiaBumpChance();
            if (bumpChance > 0 && self.getRandom().nextInt(bumpChance) == 0) {
                pregnant.triggerParonychiaBump("受到" + Math.round(amount) + "点伤害");
            }
        }
    }
}
