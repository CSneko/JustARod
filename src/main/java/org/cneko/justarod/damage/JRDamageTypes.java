package org.cneko.justarod.damage;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class JRDamageTypes {
    public static final ResourceKey<DamageType> SEXUAL_EXCITEMENT = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "sexual_excitement"));
    public static final ResourceKey<DamageType> GRASS = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "grass"));
    public static final ResourceKey<DamageType> ICED_TEA = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "iced_tea"));
    public static final ResourceKey<DamageType> PARONYCHIA = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "paronychia"));

    public static DamageSource of(Level world, ResourceKey<DamageType> key) {
        return new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    public static DamageSource sexualExcitement(Entity entity) {
        return new SexualExcitement(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SEXUAL_EXCITEMENT),entity);
    }
    public static DamageSource grass(Entity entity) {
        return new KilledByGrass(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GRASS),entity);
    }

    public static DamageSource icedTea(Entity entity) {
        return new KilledByIcedTea(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ICED_TEA), entity);
    }

    public static DamageSource paronychia(Entity entity) {
        return new KilledByParonychia(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PARONYCHIA), entity);
    }

}
