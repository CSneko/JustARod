package org.cneko.justarod.item.medical

import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.cneko.justarod.ascaris.AscarisInfectionController
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData
import java.util.Optional

/**
 * 宝塔糖（枸橼酸哌嗪）：童年回忆温和麻痹型驱虫药。
 * 真实药理：弛缓性麻痹蛔虫神经肌肉接头，在成虫期可活体软瘫排出粉白蛔虫娘（最佳收服路线）。
 */
class PagodaCandyItem(properties: Properties) : MedicalItem(properties) {

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        val stack = user.getItemInHand(hand)
        if (world.isClientSide) return InteractionResult.SUCCESS
        val ok = AscarisInfectionController.administerDewormer(user, AscarisInfectionController.DRUG_PAGODA, user)
        if (ok) {
            consumeItem(user, user, stack, hand)
            return InteractionResult.SUCCESS
        }
        return InteractionResult.FAIL
    }

    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        return target is Player && !AscarisInfectionController.isExpelling(target)
    }

    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        return Component.literal("§7宝塔糖只能喂给玩家（或直接对着粉白蛔虫娘使用）。")
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        AscarisInfectionController.administerDewormer(target, AscarisInfectionController.DRUG_PAGODA, user)
    }

    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.isCreative) stack.shrink(1)
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages? {
        if (user == target) return null
        return ActionMessages(
            userSuccessMessage = Component.literal("§d你喂给了 ${target.displayName?.string} 一颗香甜的宝塔糖。"),
            targetSuccessMessage = Component.literal("§d${user.displayName?.string} 喂给你吃了一颗粉白宝塔糖……")
        )
    }

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(
            ChemicalStructureTooltipData(Identifier.fromNamespaceAndPath("justarod", "textures/tooltip/piperazine.png"))
        )
    }
}
