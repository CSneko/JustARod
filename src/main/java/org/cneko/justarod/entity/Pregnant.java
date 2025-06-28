package org.cneko.justarod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.cneko.justarod.effect.JREffects;

public interface Pregnant{

    /*
    插的太深了...
     */

    default void setPregnant(int time) {
    }

    default int getPregnant() {
        return 0;
    }

    default boolean isPregnant() {
        return getPregnant() > 0;
    }

    default void updatePregnant() {
        if (getPregnant() > 0) {
            setPregnant(getPregnant() - 1);
            if (getPregnant() == 0) {
                if (this instanceof LivingEntity liv){
                    liv.sendMessage(Text.of("§a分娩完成！"));
                    liv.damage(liv.getDamageSources().generic(),2.0f);
                }
                makeBaby();
            }
        }
    }

    default void miscarry() {
        // 流产
        if (this instanceof LivingEntity pregnantEntity) {
            pregnantEntity.setHealth(pregnantEntity.getHealth()-10);
            pregnantEntity.sendMessage(Text.of("§c你流产了！"));
        }
        setPregnant(0);
    }

    default void makeBaby() {
        Entity baby = getBaby();
        if (baby != null) {
            // 产仔
            baby.getWorld().spawnEntity(baby);
            // 受伤
            if (this instanceof LivingEntity pregnantEntity) {
                pregnantEntity.damage(pregnantEntity.getDamageSources().generic(), 2.0F);
            }
        }
    }

    default void writePregnantToNbt(NbtCompound nbt) {
        nbt.putInt("Pregnant", getPregnant());
    }
    default void readPregnantFromNbt(NbtCompound nbt) {
        if (nbt.contains("Pregnant")) {
            setPregnant(nbt.getInt("Pregnant"));
        }
    }

    default Entity getBaby() {
        throw new RuntimeException("getBaby() is not implemented in " + this.getClass().getName());
    }

    static <T extends LivingEntity&Pregnant> void pregnantTick(T pregnant) {
        pregnant.updatePregnant();
        if (!pregnant.isPregnant()) {
            // 清除怀孕效果（如果有的话）
            pregnant.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPREGNANT_EFFECT()));
        }else {
            // 设置怀孕效果
            if (!pregnant.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPREGNANT_EFFECT()))) {
                pregnant.addStatusEffect(new StatusEffectInstance(
                        Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPREGNANT_EFFECT()),
                        pregnant.getPregnant(),
                        0,
                        false,
                        false,
                        true
                ));
            }
        }
    }


}
