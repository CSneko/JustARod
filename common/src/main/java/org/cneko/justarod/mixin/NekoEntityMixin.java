package org.cneko.justarod.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    // 26.x：实体存档改为 ValueInput/ValueOutput，通过 NbtBridge 与现有 CompoundTag 逻辑桥接
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    public void readAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        CompoundTag nbt = org.cneko.justarod.JRNbtBridge.read(input);
        if (nbt.contains("rodInside")) {
            // 26.x：ItemStack.parse 移除，改用 codec + RegistryOps
            ItemStack.CODEC.parse(
                    this.getEntity().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                    nbt.getCompoundOrEmpty("rodInside")
            ).result().ifPresent(this::setRodInside);
        }
        if (this.getEntity() instanceof Pregnant pregnant){
            pregnant.readPregnantFromNbt(nbt);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    public void addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        CompoundTag nbt = new CompoundTag();
        if (!getRodInside().isEmpty()) {
            // 26.x：ItemStack.save 移除，改用 codec + RegistryOps
            ItemStack.OPTIONAL_CODEC.encodeStart(
                    this.getEntity().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                    getRodInside()
            ).result().ifPresent(tag -> nbt.put("rodInside", tag));
        }
        if (this.getEntity() instanceof Pregnant pregnant){
            pregnant.writePregnantToNbt(nbt);
        }
        org.cneko.justarod.JRNbtBridge.store(nbt, output);
    }
    @Inject(method = "createNekoAttributes",at = @At("RETURN"))
    private static void createNekoAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(JRAttributes.Companion.getPLAYER_LUBRICATING(), 1);
    }
}
