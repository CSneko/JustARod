package org.cneko.justarod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.cneko.justarod.JRAttributes.Companion;

@SuppressWarnings({"AddedMixinMembersNamePattern", "ConstantValue", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements Powerable {

    @Unique
    private double power = 0;
    @Unique
    private short slowTick = 10;

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
        power = this.readPowerFromNbt(nbt);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        this.writePowerToNbt(nbt);
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        Powerable.tickPower(player);
        if (slowTick++ >= 10){
            if (player instanceof ServerPlayerEntity sp) {
                // 同步power
                ServerPlayNetworking.send(sp, new PowerSyncPayload(getPower()));
            }
        }
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.getReturnValue()
                .add(Companion.getPLAYER_LUBRICATING())
                .add(Companion.getGENERIC_MAX_POWER());
    }

}
