package org.cneko.justarod.item.custom

import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.item.JRComponents.PantsuState

class PantsuItem(
    material: RegistryEntry<ArmorMaterial>,
    type: Type,
    settings: Settings
) : ArmorItem(material, type, settings) {

    // 每一刻都会运行，用于检测穿戴状态
    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        // 仅在服务端运行逻辑
        if (!world.isClient && entity is LivingEntity) {

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
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)

        // 1. 显示主人
        val ownerName = stack.get(JRComponents.OWNER)
        if (!ownerName.isNullOrEmpty()) {
            // 显示：原味: PlayerName
            tooltip.add(Text.translatable("tooltip.justarod.pantsu.owner", ownerName).formatted(Formatting.GOLD))
        }

        // 2. 显示状态 (湿、脏等)
        val state = stack.get(JRComponents.PANTSU_STATE) ?: PantsuState.CLEAN
        if (state != PantsuState.CLEAN) {
            tooltip.add(Text.translatable(state.translationKey).formatted(Formatting.RED))
        }
    }
}