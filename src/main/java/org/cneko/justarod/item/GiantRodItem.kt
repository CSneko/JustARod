package org.cneko.justarod.item

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import org.cneko.justarod.effect.JREffects

class GiantRodItem: OtherUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.USED_TIME_MARK, 0)) {
    override fun canAcceptEntity(stack: ItemStack, entity: Entity): Boolean {
        return entity.type.equals(EntityType.ENDER_DRAGON)
    }

    override fun useOnOther(stack: ItemStack, world: World?, user: PlayerEntity, target: LivingEntity): ActionResult {
        if (super.useOnOther(stack, world, user, target) == ActionResult.FAIL) {
            user.sendMessage(Text.translatable("item.justarod.giant_rod.too_big"))
            return ActionResult.FAIL
        }
        // 给目标实体高潮效果
        JREffects.TATER_EFFECT?.let {
            val orgasm = StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(it), 100, 0)
            target.addStatusEffect(orgasm)
        }
        // 掉一颗龙蛋
        target.dropItem(Items.DRAGON_EGG)
        return ActionResult.SUCCESS
    }

    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.USE_ON_OTHER_INSERT
    }


}