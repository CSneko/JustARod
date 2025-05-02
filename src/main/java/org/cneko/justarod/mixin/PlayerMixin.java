package org.cneko.justarod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.cneko.justarod.entity.Insertable;
import org.cneko.justarod.entity.Powerable;
import org.cneko.justarod.packet.PowerSyncPayload;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"AddedMixinMembersNamePattern", "ConstantValue", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements Insertable, Powerable {

    @Unique
    private ItemStack rodInside = ItemStack.EMPTY;
    @Unique
    private double power = 0;
    @Unique
    private short slowTick = 10;

    @Override
    public ItemStack getRodInside() {
        return rodInside;
    }

    @Override
    public void setRodInside(@NotNull ItemStack rodInside) {
        this.rodInside = rodInside;
    }

    @Override
    public double getPower() {
        return power;
    }

    @Override
    public void setPower(double power) {
        this.power = power;
    }

    @Override
    public boolean canPowerUp() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        return player.getHungerManager().getFoodLevel() >= 3;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        if (nbt.contains("rodInside")) {
            var rod = ItemStack.fromNbt(player.getServerWorld().getRegistryManager(),nbt.getCompound("rodInside"));
            rod.ifPresent(this::setRodInside);
        }
        power = this.readPowerFromNbt(nbt);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        if (!getRodInside().isEmpty()) {
            nbt.put("rodInside", getRodInside().encode(
                    player.getRegistryManager()
            ));
        }
        this.writePowerToNbt(nbt);
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!getRodInside().isEmpty()) {
            this.tickInside(player);
        }
        Powerable.tickPower(player);
        if (slowTick++ >= 10){
            if (player instanceof ServerPlayerEntity sp) {
                // 同步power
                ServerPlayNetworking.send(sp, new PowerSyncPayload(getPower()));
            }
        }
    }

}
