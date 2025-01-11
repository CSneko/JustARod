package org.cneko.justarod.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.cneko.justarod.entity.Insertable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerMixin implements Insertable {
    @Shadow public abstract ServerWorld getServerWorld();

    @Unique
    private ItemStack rodInside = ItemStack.EMPTY;
    @Override
    public ItemStack getRodInside() {
        return rodInside;
    }

    @Override
    public void setRodInside(ItemStack rodInside) {
        this.rodInside = rodInside;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("rodInside")) {
            var rod = ItemStack.fromNbt(this.getServerWorld().getRegistryManager(),nbt.getCompound("rodInside"));
            rod.ifPresent(this::setRodInside);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!getRodInside().isEmpty()) {
            nbt.put("rodInside", getRodInside().encode(
                    this.getServerWorld().getRegistryManager()
            ));
        }
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (!getRodInside().isEmpty()) {
            this.tickInside((ServerPlayerEntity) (Object) this);
        }
    }

}
