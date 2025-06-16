package org.cneko.justarod.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import static org.cneko.justarod.Justarod.MODID;

public class JRDamageTypes {
    public static final RegistryKey<DamageType> SEXUAL_EXCITEMENT = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MODID, "sexual_excitement"));
    public static final RegistryKey<DamageType> GRASS = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MODID, "grass"));
    public static final RegistryKey<DamageType> ICED_TEA = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MODID, "iced_tea"));

    public static DamageSource of(World world, RegistryKey<DamageType> key) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key));
    }

    public static DamageSource sexualExcitement(Entity entity) {
        return new SexualExcitement(entity.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(SEXUAL_EXCITEMENT),entity);
    }
    public static DamageSource grass(Entity entity) {
        return new KilledByGrass(entity.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(GRASS),entity);
    }

    public static DamageSource icedTea(Entity entity) {
        return new KilledByIcedTea(entity.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(ICED_TEA), entity);
    }

}
