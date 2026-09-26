package org.cneko.justarod.item.armor
import org.cneko.justarod.JRIds

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.server.level.ServerLevel

/*
我嘞个豆~~~~~~~~~~~~~
 */
class FireworksRodItem : RodArmorItem<FireworksRodItem>(JRArmorMaterials.FIREWORKS_ROD_MATERIAL, JRIds.itemProps("fireworks_rod").stacksTo(1).durability(1000)) {
    companion object{
        const val ID = "fireworks_rod"
    }
    override fun getId(): String {
        return ID
    }

    override fun inventoryTick(
        stack: ItemStack,
        world: net.minecraft.server.level.ServerLevel,
        entity: net.minecraft.world.entity.Entity,
        slot: EquipmentSlot?
) {
        super.inventoryTick(stack, world, entity, slot)
    }

    override fun onUse(stack: ItemStack, world: ServerLevel, entity: Entity, slot: EquipmentSlot?) {
        super.onUse(stack, world, entity, slot)
        if (entity is Player){
            // 1/5的概率放一个烟花
            if (entity.random.nextInt(5) == 0){
                val rocket = Items.FIREWORK_ROCKET.defaultInstance
                rocket.set(DataComponents.FIREWORKS, Fireworks(60, listOf(FireworkExplosion(FireworkExplosion.Shape.STAR,
                    IntArrayList(listOf(0xDC143C, 0xFFD700, 0xFFE4E1)),
                    IntArrayList(listOf(0xDB7093, 0xFFF8DC, 0xC0C0C0)),
                    true,
                    true
                ))))
                world.addFreshEntity(FireworkRocketEntity(world, rocket, entity))
            }
        }
    }

}
