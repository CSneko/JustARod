package org.cneko.justarod.item.bdsm

import net.fabricmc.fabric.api.item.v1.EnchantingContext
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level
import org.cneko.justarod.entity.BDSMable
import kotlin.jvm.optionals.getOrNull

abstract class AbstractBDSMItem(
    properties: Properties,
    private val fieldGetter: (BDSMable) -> Int,
    private val fieldSetter: (BDSMable, Int) -> Unit,
    private val alreadyHasMessage: String,
    private val successMessage: String,
    private val durationTicks: Int = 20 * 60 * 5, // 默认 5 分钟
    private val alreadyHasColor: String = "§c",  // 默认红色
    private val successColor: String = "§a"      // 默认绿色
) : Item(properties) {

    override fun interactLivingEntity(
        stack: ItemStack,
        user: Player,
        entity: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        if (entity is BDSMable && !entity.level().isClientSide) {
            val finalDuration = getExtendedDuration(stack)
            if (fieldGetter(entity) > 0) {
                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("$alreadyHasColor$alreadyHasMessage"))
                return InteractionResult.FAIL
            } else {
                fieldSetter(entity, finalDuration)
                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("$successColor$successMessage"))
                if (!user.isCreative) {
                    stack.shrink(1)
                }
                return InteractionResult.SUCCESS
            }
        }
        return super.interactLivingEntity(stack, user, entity, hand)
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        val stack = user.getItemInHand(hand)
        // shift + 右键作用于自己
        if (!world.isClientSide && user.isShiftKeyDown()) {
            val finalDuration = getExtendedDuration(stack)
            // PlayerMixin 让 Player 实现 BDSMable
            val self = user as BDSMable
            if (fieldGetter(self) > 0) {
                user.sendSystemMessage(Component.literal("$alreadyHasColor$alreadyHasMessage"))
                return InteractionResult.FAIL
            } else {
                fieldSetter(self, finalDuration)
                user.sendSystemMessage(Component.literal("$successColor$successMessage"))
                if (!user.isCreative) {
                    stack.shrink(1)
                }
                return InteractionResult.SUCCESS
            }
        }
        return super.use(world, user, hand)
    }

    private fun getExtendedDuration(stack: ItemStack?): Int {
        if (stack == null) return durationTicks
        var unbreaking = 0
        val enchantments = stack.get(DataComponents.ENCHANTMENTS)
        if (enchantments != null) {
            for (entry in enchantments.entrySet()) {
                if (entry.key.`is`(Enchantments.UNBREAKING)) {
                    unbreaking = entry.intValue
                }
            }
        }
        if (unbreaking > 0) {
            return durationTicks + durationTicks / 2 * unbreaking
        }
        return durationTicks
    }

    // 26.x：Item#isEnchantable / #getEnchantmentValue 已移除，附魔能力改由
    // DataComponents.ENCHANTABLE 组件决定（在 JRItems 注册属性时设置）。
    override fun canBeEnchantedWith(
        stack: ItemStack,
        enchantment: Holder<Enchantment>,
        context: EnchantingContext
    ): Boolean {
        return super.canBeEnchantedWith(stack, enchantment, context) || enchantment.`is`(Enchantments.UNBREAKING)
    }
}
