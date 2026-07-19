package org.cneko.justarod.item.rod

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.JRComponents

/*
事实上没人可以把这个塞进去，哪怕扩张有多厉害
 */
class GiantRodItem: OtherUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.Companion.USED_TIME_MARK, 0)) {
    override fun canAcceptEntity(stack: ItemStack, entity: Entity): Boolean {
        return entity.type.equals(EntityType.ENDER_DRAGON)
    }

    override fun useOnOther(stack: ItemStack, world: Level?, user: Player, target: LivingEntity): InteractionResult {
        if (super.useOnOther(stack, world, user, target) == InteractionResult.FAIL) {
            user.sendSystemMessage(Component.translatable("item.justarod.giant_rod.too_big"))
            return InteractionResult.FAIL
        }
        // 给目标实体高潮效果
        JREffects.ORGASM_EFFECT?.let {
            val orgasm = MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getOrThrow(it), 100, 0)
            target.addEffect(orgasm)
        }
        // 掉一颗龙蛋
        target.dropItem(Items.DRAGON_EGG)
        return InteractionResult.SUCCESS
    }

    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.USE_ON_OTHER_INSERT
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        tooltip?.add(Component.translatable("item.justarod.giant_rod.tooltip"))
    }


}