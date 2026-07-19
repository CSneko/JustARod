package org.cneko.justarod.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.component.DataComponentType
import net.minecraft.component.type.NbtComponent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.StringRepresentable
import org.cneko.justarod.Justarod.MODID
import java.util.*

class JRComponents{
    companion object{
        val USED_TIME_MARK: ComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "used_time_mark"),
            ComponentType.builder<Int>().codec(Codec.INT).build()
        )
        val OWNER: ComponentType<String> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "owner"),
            ComponentType.builder<String>().codec(Codec.STRING).build()
        )
        val SPEED: ComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "speed"),
            ComponentType.builder<Int>().codec(Codec.INT).build()
        )
        val MODE: ComponentType<String> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "mode"),
            ComponentType.builder<String>().codec(Codec.STRING).build()
        )
        val ROD_INSIDE: ComponentType<ItemStack> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "rod_inside"),
            ComponentType.builder<ItemStack>().codec(ItemStack.CODEC).build()
        )
        val SECRETIONS_APPEARANCE: ComponentType<String> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "secretions_appearance"),
            ComponentType.builder<String>().codec(Codec.STRING).build()
        )

        val ENTITY_TYPE: ComponentType<EntityType<*>> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "entity_type"),
            ComponentType.builder<EntityType<*>>()
                .codec(BuiltInRegistries.ENTITY_TYPE.codec)
                .build()
        )
        val COLLECTED_TIME: ComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "collected_time"),
            ComponentType.builder<Int>().codec(Codec.INT).build()
        )
        val PANTSU_STATE: ComponentType<PantsuState> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "pantsu_state"),
            ComponentType.builder<PantsuState>().codec(PantsuState.CODEC).build()
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
            return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, id), type)
        }



    }

    enum class PantsuState(private val id: String, val translationKey: String) : StringRepresentable {
        CLEAN("clean", "tooltip.justarod.pantsu.clean"),
        WET("wet", "tooltip.justarod.pantsu.wet"),          // 尿湿
        SOILED("soiled", "tooltip.justarod.pantsu.soiled"), // 弄脏(大号)
        BLOODY("bloody", "tooltip.justarod.pantsu.bloody"); // 血染(经期/其他)

        override fun asString(): String = id

        companion object {
            val CODEC: Codec<PantsuState> = StringRepresentable.createCodec { PantsuState.entries.toTypedArray() }
        }
    }
}