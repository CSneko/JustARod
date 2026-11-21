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
 * 阿苯达唑片：硬核强效广谱驱虫药。
 * 真实药理：抑制虫体微管蛋白聚合、阻断葡萄糖摄取；Ⅰ~Ⅱ期直接杀灭，Ⅲ~Ⅳ期强排出的蛔虫娘生命垂危。
 */
class AlbendazoleItem(properties: Properties) : MedicalItem(properties) {

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        val stack = user.getItemInHand(hand)
        if (world.isClientSide) return InteractionResult.SUCCESS
        val ok = AscarisInfectionController.administerDewormer(user, AscarisInfectionController.DRUG_ALBENDAZOLE, user)
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
        return Component.literal("§7阿苯达唑片只能给玩家服用（或直接对粉白蛔虫娘使用）。")
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        AscarisInfectionController.administerDewormer(target, AscarisInfectionController.DRUG_ALBENDAZOLE, user)
    }

    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.isCreative) stack.shrink(1)
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages? {
        if (user == target) return null
        return ActionMessages(
            userSuccessMessage = Component.literal("§b你给 ${target.displayName?.string} 服用了阿苯达唑片。"),
            targetSuccessMessage = Component.literal("§b${user.displayName?.string} 让你服下了阿苯达唑片……")
        )
    }

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(
            ChemicalStructureTooltipData(Identifier.fromNamespaceAndPath("justarod", "textures/tooltip/albendazole.png"))
        )
    }
}
