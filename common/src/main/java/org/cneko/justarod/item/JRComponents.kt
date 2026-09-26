package org.cneko.justarod.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.component.DataComponentType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.StringRepresentable
import org.cneko.justarod.Justarod.MODID
import java.util.*

class JRComponents{
    companion object{
        val USED_TIME_MARK: DataComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "used_time_mark"),
            DataComponentType.builder<Int>().persistent(Codec.INT).build()
        )
        val OWNER: DataComponentType<String> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "owner"),
            DataComponentType.builder<String>().persistent(Codec.STRING).build()
        )
        val SPEED: DataComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "speed"),
            DataComponentType.builder<Int>().persistent(Codec.INT).build()
        )
        val MODE: DataComponentType<String> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "mode"),
            DataComponentType.builder<String>().persistent(Codec.STRING).build()
        )
        val ROD_INSIDE: DataComponentType<ItemStack> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "rod_inside"),
            DataComponentType.builder<ItemStack>().persistent(ItemStack.CODEC).build()
        )
        val SECRETIONS_APPEARANCE: DataComponentType<String> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "secretions_appearance"),
            DataComponentType.builder<String>().persistent(Codec.STRING).build()
        )

        val ENTITY_TYPE: DataComponentType<EntityType<*>> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "entity_type"),
            DataComponentType.builder<EntityType<*>>()
                .persistent(BuiltInRegistries.ENTITY_TYPE.byNameCodec())
                .build()
        )
        val COLLECTED_TIME: DataComponentType<Int> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "collected_time"),
            DataComponentType.builder<Int>().persistent(Codec.INT).build()
        )
        val PANTSU_STATE: DataComponentType<PantsuState> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "pantsu_state"),
            DataComponentType.builder<PantsuState>().persistent(PantsuState.CODEC).build()
        )


        // 实体NBT (用于复制生成幼崽的属性)
        val CLONER_ENTITY_NBT: DataComponentType<CustomData> =
            register("cloner_entity_nbt", DataComponentType.builder<CustomData>().persistent(CustomData.CODEC).build())

        // 是否完成细胞核转移
        val CLONER_TRANSFERRED: DataComponentType<Boolean> =
            register("cloner_transferred", DataComponentType.builder<Boolean>().persistent(Codec.BOOL).build())

        val CLONER_STATE: DataComponentType<String> =
            register("cloner_state", DataComponentType.builder<String>().persistent(Codec.STRING).build())

        private fun <T : Any> register(id: String, type: DataComponentType<T>): DataComponentType<T> {
            return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(MODID, id), type)
        }



    }

    enum class PantsuState(private val id: String, val translationKey: String) : StringRepresentable {
        CLEAN("clean", "tooltip.justarod.pantsu.clean"),
        WET("wet", "tooltip.justarod.pantsu.wet"),          // 尿湿
        SOILED("soiled", "tooltip.justarod.pantsu.soiled"), // 弄脏(大号)
        BLOODY("bloody", "tooltip.justarod.pantsu.bloody"); // 血染(经期/其他)

        override fun getSerializedName(): String = id

        companion object {
            val CODEC: Codec<PantsuState> = StringRepresentable.fromEnum { PantsuState.values() }
        }
    }
}