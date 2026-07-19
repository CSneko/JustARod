package org.cneko.justarod.item.custom

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.level.Level
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.item.JRComponents.PantsuState

class DiaperItem(
    material: Holder<ArmorMaterial>,
    type: Type,
    settings: Properties
) : ArmorItem(material, type, properties) {

    // 每一刻都会运行，用于检测穿戴状态
    override fun inventoryTick(stack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        // 仅在服务端运行逻辑
        if (!level().isClientSide && entity is LivingEntity) {

            if (entity.getEquippedStack(EquipmentSlot.LEGS) === stack) {
                // 如果还没有主人标记，则打上标记
                if (!stack.contains(JRComponents.OWNER)) {
                    val ownerName = entity.name.string
                    stack.set(JRComponents.OWNER, ownerName)
                }
            }
        }
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltip, type)

        // 1. 显示主人
        val ownerName = stack.get(JRComponents.OWNER)
        if (!ownerName.isNullOrEmpty()) {
            // 显示：原味: PlayerName
            tooltip.add(Component.translatable("tooltip.justarod.pantsu.owner", ownerName).withStyle(ChatFormatting.GOLD))
        }

        // 2. 显示状态 (湿、脏等)
        val state = stack.get(JRComponents.PANTSU_STATE) ?: PantsuState.CLEAN
        if (state != PantsuState.CLEAN) {
            tooltip.add(Component.translatable(state.translationKey).withStyle(ChatFormatting.RED))
        }
    }
}