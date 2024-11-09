package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import org.cneko.justarod.effect.OrgasmEffect

class RedstoneEndRod:SelfUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.USED_TIME_MARK, 0).component(JRComponents.SPEED, 2)) {
    override fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean): ActionResult {
        return super.useOnSelf(stack, world, entity, slot, selected)
    }
}