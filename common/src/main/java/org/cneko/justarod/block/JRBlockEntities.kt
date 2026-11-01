package org.cneko.justarod.block

import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntityType
import org.cneko.justarod.Justarod.MODID

/**
 * 方块实体类型注册。
 * 必须在 [JRBlocks] 之后调用（方块实体要引用已经注册好的方块）。
 */
object JRBlockEntities {
    lateinit var END_ROD_TRAP: BlockEntityType<EndRodTrapBlockEntity>
        private set

    fun init() {
        END_ROD_TRAP = FabricBlockEntityTypeBuilder.create(
            { pos, state -> EndRodTrapBlockEntity(END_ROD_TRAP, pos, state) },
            JRBlocks.END_ROD_TRAP
        ).build()
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "end_rod_trap"),
            END_ROD_TRAP
        )
    }
}
