package org.cneko.justarod.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.component.ComponentType
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID
import java.util.*

class JRComponents{
    companion object{
        val USED_TIME_MARK: ComponentType<Int> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "used_time_mark"),
            ComponentType.builder<Int>().codec(Codec.INT).build()
        )
        val OWNER: ComponentType<String> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "owner"),
            ComponentType.builder<String>().codec(Codec.STRING).build()
        )
        val SPEED: ComponentType<Int> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "speed"),
            ComponentType.builder<Int>().codec(Codec.INT).build()
        )
        val MODE: ComponentType<String> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "mode"),
            ComponentType.builder<String>().codec(Codec.STRING).build()
        )
        val ROD_INSIDE: ComponentType<ItemStack> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "rod_inside"),
            ComponentType.builder<ItemStack>().codec(ItemStack.CODEC).build()
        )
    }
}