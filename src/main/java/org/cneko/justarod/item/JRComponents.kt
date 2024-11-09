package org.cneko.justarod.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.component.ComponentType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
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
        val SPEED: ComponentType<Int> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "speed"),
            ComponentType.builder<Int>().codec(Codec.INT).build()
        )
    }
}