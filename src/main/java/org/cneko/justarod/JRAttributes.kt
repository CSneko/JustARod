package org.cneko.justarod

import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.RangedAttribute
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.player.Player
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import org.cneko.justarod.Justarod.MODID

class JRAttributes {
    companion object{
        val PLAYER_LUBRICATING_ID:ResourceLocation = ResourceLocation.fromNamespaceAndPath(MODID, "player.lubricating")
        val PLAYER_LUBRICATING: Holder<Attribute> = register(
            PLAYER_LUBRICATING_ID,
            RangedAttribute(
                "attribute.name.player.lubricating",
                1.0, 0.0, 1000.0
            ).setSyncable(true)
        )
        val GENERIC_MAX_POWER_ID = ResourceLocation.fromNamespaceAndPath(MODID, "generic.max_power")
        val GENERIC_MAX_POWER: Holder<Attribute> = register(
            GENERIC_MAX_POWER_ID,
            RangedAttribute(
                "attribute.name.generic.max_power",
                100.0, 0.0, 1000.0
            ).setSyncable(true)
        )
        fun register(id: ResourceLocation?, attribute: Attribute?): Holder<Attribute> {
            return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, id, attribute)
        }
        fun init(){

        }
    }
}