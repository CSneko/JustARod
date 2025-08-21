package org.cneko.justarod.item.bdsm

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.BuiltinRegistries
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.entity.BDSMable
import kotlin.jvm.optionals.getOrNull

abstract class AbstractBDSMItem(
    settings: Settings,
    private val fieldGetter: (BDSMable) -> Int,
    private val fieldSetter: (BDSMable, Int) -> Unit,
    private val alreadyHasMessage: String,
    private val successMessage: String,
    private val durationTicks: Int = 20 * 60 * 5, // 默认 5 分钟
    private val alreadyHasColor: String = "§c",  // 默认红色
    private val successColor: String = "§a"      // 默认绿色
) : Item(settings) {

    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult {
        if (entity is BDSMable && !entity.world.isClient) {
            val finalDuration = getExtendedDuration(stack)
            if (fieldGetter(entity) > 0) {
                user?.sendMessage(Text.of("$alreadyHasColor$alreadyHasMessage"))
                return ActionResult.FAIL
            } else {
                fieldSetter(entity, finalDuration)
                user?.sendMessage(Text.of("$successColor$successMessage"))
                if (user?.isCreative == false) {
                    stack?.decrement(1)
                }
                return ActionResult.SUCCESS
            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack?>? {
        val stack = user.getStackInHand(hand)
        // shift + 右键作用于自己
        if (!world.isClient && user.isSneaking) {
            val finalDuration = getExtendedDuration(stack)
            if (fieldGetter(user) > 0) {
                user.sendMessage(Text.of("$alreadyHasColor$alreadyHasMessage"))
                return TypedActionResult.fail(stack)
            } else {
                fieldSetter(user, finalDuration)
                user.sendMessage(Text.of("$successColor$successMessage"))
                if (!user.isCreative) {
                    stack.decrement(1)
                }
                return TypedActionResult.success(stack)
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
        enchantment: RegistryEntry<Enchantment?>?,
        context: EnchantingContext?
    ): Boolean {
        return super.canBeEnchantedWith(stack, enchantment, context) || enchantment?.key?.getOrNull() == Enchantments.UNBREAKING
    }

}
