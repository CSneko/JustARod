package org.cneko.justarod.item.bdsm

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import org.cneko.justarod.entity.BDSMable
import kotlin.jvm.optionals.getOrNull

abstract class AbstractBDSMItem(
    settings: Properties,
    private val fieldGetter: (BDSMable) -> Int,
    private val fieldSetter: (BDSMable, Int) -> Unit,
    private val alreadyHasMessage: String,
    private val successMessage: String,
    private val durationTicks: Int = 20 * 60 * 5, // 默认 5 分钟
    private val alreadyHasColor: String = "§c",  // 默认红色
    private val successColor: String = "§a"      // 默认绿色
) : Item(properties) {

    override fun useOnEntity(
        stack: ItemStack?,
        user: Player?,
        entity: LivingEntity?,
        hand: InteractionHand?
    ): InteractionResult {
        if (entity is BDSMable && !entity.level().isClientSide) {
            val finalDuration = getExtendedDuration(stack)
            if (fieldGetter(entity) > 0) {
                user?.sendSystemMessage(Component.literal("$alreadyHasColor$alreadyHasMessage"))
                return InteractionResult.FAIL
            } else {
                fieldSetter(entity, finalDuration)
                user?.sendSystemMessage(Component.literal("$successColor$successMessage"))
                if (user?.isCreative == false) {
                    stack?.shrink(1)
                }
                return InteractionResult.SUCCESS
            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack?>? {
        val stack = user.getItemInHand(hand)
        // shift + 右键作用于自己
        if (!level().isClientSide && user.isShiftKeyDown()) {
            val finalDuration = getExtendedDuration(stack)
            if (fieldGetter(user) > 0) {
                user.sendSystemMessage(Component.literal("$alreadyHasColor$alreadyHasMessage"))
                return InteractionResultHolder.fail(stack)
            } else {
                fieldSetter(user, finalDuration)
                user.sendSystemMessage(Component.literal("$successColor$successMessage"))
                if (!user.isCreative) {
                    stack.shrink(1)
                }
                return InteractionResultHolder.success(stack)
            }
        }
        return super.use(world, user, hand)
    }

    private fun getExtendedDuration(stack: ItemStack?): Int {
        if (stack == null) return durationTicks
        var unbreaking = 0
        stack.components.get(DataComponentTypes.ENCHANTMENTS)?.enchantmentEntries?.forEach { entry ->
            if (entry.key.key.getOrNull() == Enchantments.UNBREAKING) {
                unbreaking = EnchantmentHelper.getLevel(entry.key, stack)
            }
        }
        if (unbreaking > 0) {
            return durationTicks + durationTicks / 2 * unbreaking
        }
        return durationTicks
    }

    override fun isEnchantable(stack: ItemStack): Boolean {
        return true // 允许被附魔
    }

    override fun getEnchantability(): Int {
        return 10 // 附魔能力，数值越高越容易获得高级附魔
    }

    override fun canBeEnchantedWith(
        stack: ItemStack?,
        enchantment: Holder<Enchantment?>?,
        context: EnchantingContext?
    ): Boolean {
        return super.canBeEnchantedWith(stack, enchantment, context) || enchantment?.key?.getOrNull() == Enchantments.UNBREAKING
    }

}
