package org.cneko.justarod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.justarod.JRAttributes;
import org.cneko.justarod.entity.Insertable;
import org.cneko.justarod.entity.Powerable;
import org.cneko.justarod.packet.PowerSyncPayload;
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
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (nbt.contains("rodInside")) {
            var rod = ItemStack.fromNbt(self.getRegistryManager(),nbt.getCompound("rodInside"));
            rod.ifPresent(this::setRodInside);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!getRodInside().isEmpty()) {
            nbt.put("rodInside", getRodInside().encode(
                    self.getRegistryManager()
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
    private static void createLivingAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        DefaultAttributeContainer.Builder builder = cir.getReturnValue();
        builder.add(JRAttributes.Companion.getPLAYER_LUBRICATING());
        builder.add(JRAttributes.Companion.getGENERIC_MAX_POWER());
    }
}
