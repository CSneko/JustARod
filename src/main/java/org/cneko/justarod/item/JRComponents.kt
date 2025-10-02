package org.cneko.justarod.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.component.ComponentType
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
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
        val SECRETIONS_APPEARANCE: ComponentType<String> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "secretions_appearance"),
            ComponentType.builder<String>().codec(Codec.STRING).build()
        )

        val ENTITY_TYPE: ComponentType<EntityType<*>> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "entity_type"),
            ComponentType.builder<EntityType<*>>()
                .codec(Registries.ENTITY_TYPE.codec)
                .build()
        )
        val COLLECTED_TIME: ComponentType<Int> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MODID, "collected_time"),
            ComponentType.builder<Int>().codec(Codec.INT).build()
        )


        // 实体NBT (用于复制生成幼崽的属性)
        val CLONER_ENTITY_NBT: ComponentType<NbtComponent> =
            register("cloner_entity_nbt", ComponentType.builder<NbtComponent>().codec(NbtComponent.CODEC).build())

        // 是否完成细胞核转移
        val CLONER_TRANSFERRED: ComponentType<Boolean> =
            register("cloner_transferred", ComponentType.builder<Boolean>().codec(Codec.BOOL).build())

        val CLONER_STATE: ComponentType<String> =
            register("cloner_state", ComponentType.builder<String>().codec(Codec.STRING).build())

        private fun <T> register(id: String, type: ComponentType<T>): ComponentType<T> {
            return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(MODID, id), type)
        }

    }
}