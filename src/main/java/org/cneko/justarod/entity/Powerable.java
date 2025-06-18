package org.cneko.justarod.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import org.cneko.justarod.JRAttributes;
import org.cneko.justarod.effect.JREffects;
import org.jetbrains.annotations.NotNull;

/*
那种事情超废体力的好吗
 */
public interface Powerable{
    default double getPower(){
        return 0;
    }
    default void setPower(double power){
        throw new UnsupportedOperationException("This entity is not Powerable");
    }

    default boolean canPowerUp(){
        return true;
    }

    default double readPowerFromNbt(@NotNull NbtCompound nbt){
        if (nbt.contains("power")) {
            return nbt.getDouble("power");
        }else return 100;
    }
    default void writePowerToNbt(@NotNull NbtCompound nbt){
        nbt.putDouble("power", getPower());
    }

    static <E extends LivingEntity & Powerable> void tickPower(@NotNull E entity) {
        if (entity.canPowerUp()) {
            powerUp(entity, 0.01);
        }
        entity.setPower(Math.min(entity.getPower(), getMaxPower(entity)));
        // 如果能量低于0了，晕倒
        if (entity.getPower() <= 0) {
            entity.setPower(0);
            entity.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getFAINT_EFFECT()), 1000, 1));
        }
    }

    static <E extends LivingEntity & Powerable> double getMaxPower(@NotNull E entity) {
        return entity.getAttributeValue(JRAttributes.Companion.getGENERIC_MAX_POWER());
    }

    static <E extends LivingEntity & Powerable> void powerUp(E e,double amount) {
        double power = e.getPower();
        e.setPower(power + amount);
    }
}
