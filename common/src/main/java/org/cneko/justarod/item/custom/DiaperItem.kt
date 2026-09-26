package org.cneko.justarod.item.custom

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.level.Level
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.item.JRComponents.PantsuState

class DiaperItem(
    material: ArmorMaterial,
    properties: Properties
) : Item(properties.humanoidArmor(material, ArmorType.LEGGINGS)) {

    override fun inventoryTick(
        stack: ItemStack,
        world: net.minecraft.server.level.ServerLevel,
        entity: net.minecraft.world.entity.Entity,
        slot: EquipmentSlot?
) {
        // 仅在服务端运行逻辑
        if (!world.isClientSide && entity is LivingEntity) {
            if (entity.getItemBySlot(EquipmentSlot.LEGS) === stack) {
                if (!stack.has(JRComponents.OWNER)) {
                    val ownerName = entity.name.string
                    stack.set(JRComponents.OWNER, ownerName)
                }
            }
        }
    }

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        val tooltips = mutableListOf<Component>()

        val ownerName = stack.get(JRComponents.OWNER)
        if (!ownerName.isNullOrEmpty()) {
            tooltips.add(Component.translatable("tooltip.justarod.pantsu.owner", ownerName).withStyle(ChatFormatting.GOLD))
        }

        val state = stack.get(JRComponents.PANTSU_STATE) ?: PantsuState.CLEAN
        if (state != PantsuState.CLEAN) {
            tooltips.add(Component.translatable(state.translationKey).withStyle(ChatFormatting.RED))
        }

        for (t in tooltips) adder.accept(t)
    }
}
