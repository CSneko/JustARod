package org.cneko.justarod.item.armor

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.FireworkExplosionComponent
import net.minecraft.component.type.FireworksComponent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/*
我嘞个豆~~~~~~~~~~~~~
 */
class FireworksRodItem : RodArmorItem<FireworksRodItem>(JRArmorMaterials.FIREWORKS_ROD_MATERIAL, Settings().maxCount(1).maxDamage(1000)) {
    companion object{
        const val ID = "fireworks_rod"
    }
    override fun getId(): String {
        return ID
    }

    override fun inventoryTick(stack: ItemStack?, world: Level?, entity: Entity?, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
    }

    override fun onUse(stack: ItemStack?, world: Level?, entity: Entity?, slot: Int, selected: Boolean) {
        super.onUse(stack, world, entity, slot, selected)
        if (entity is Player){
            // 1/5的概率放一个烟花
            if (entity.random.nextInt(5) == 0){
                val stack = Items.FIREWORK_ROCKET.getDefaultInstance
                stack.set(DataComponentTypes.FIREWORKS, FireworksComponent(60, listOf(FireworkExplosionComponent(FireworkExplosionComponent.Type.STAR,
                    IntArrayList(listOf(0xDC143C, 0xFFD700, 0xFFE4E1)),
                    IntArrayList(listOf(0xDB7093, 0xFFF8DC, 0xC0C0C0)),
                    true,
                    true
                ))))
                entity.level().addFreshEntity(FireworkRocketEntity(entity.level(),stack, entity))
            }
        }
    }

}