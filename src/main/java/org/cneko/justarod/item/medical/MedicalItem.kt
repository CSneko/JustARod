package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level

/**
 * 医疗物品的抽象基类
 * @param settings 物品设置
 */
abstract class MedicalItem(properties: Properties) : Item(properties) {

    /**
     * 用于保存操作成功时各种消息的数据类
     * @param userSuccessMessage 发送给执行操作的玩家的消息
     * @param targetSuccessMessage 发送给目标实体(如果与用户不同)的消息
     * @param userExtraMessage 给用户的额外可选消息
     * @param targetExtraMessage 给目标的额外可选消息
     */
    data class ActionMessages(
        val userSuccessMessage: Component?,
        val targetSuccessMessage: Component?,
        val userExtraMessage: Component? = null,
        val targetExtraMessage: Component? = null,
    )


    /**
     * 检查是否可以对目标执行操作
     * @param user 执行操作的玩家
     * @param target 被操作的目标实体
     * @param stack 使用的物品堆栈
     * @param hand 持有物品的手
     * @return 如果可以执行操作返回true，否则返回false
     */
    abstract fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean

    /**
     * 获取当操作无法通过`canApply`检查时显示的消息
     * @param user 执行操作的玩家
     * @param target 尝试操作的目标实体
     * @param stack 使用的物品堆栈
     * @return 失败消息的Text对象
     */
    abstract fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component?

    /**
     * 对目标执行物品的核心效果
     * (例如：设置布尔标志、应用状态效果、掉落物品等)
     * @param user 执行操作的玩家
     * @param target 被操作的目标实体
     * @param stack 使用的物品堆栈
     * @param hand 持有物品的手
     */
    abstract fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand)

    /**
     * 在成功操作后消耗物品
     * 可以是减少堆栈数量或应用耐久损耗
     * @param user 执行操作的玩家
     * @param target 被操作的目标实体
     * @param stack 使用的物品堆栈
     */
    abstract fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand)

    /**
     * 获取操作成功时显示的消息集合
     * @param user 执行操作的玩家
     * @param target 被操作的目标实体
     * @param stack 使用的物品堆栈
     * @return 包含相关消息的[ActionMessages]对象
     */
    abstract fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages?



    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        if (level().isClientSide) return InteractionResultHolder.pass(stack)

        // 只有按下 Shift 时才对自己使用
        if (!user.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack)
        }

        val result = performAction(user, user, stack, hand)

        return if (result == InteractionResult.SUCCESS)
            InteractionResultHolder.success(stack)
        else
            InteractionResultHolder.fail(stack)
    }


    final override fun useOnEntity(stack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        if (user.level().isClientSide) return InteractionResult.PASS

        val result = performAction(user, entity, stack, hand)

        // 如果对目标失败了，返回 FAIL 防止 fallback 到 use()
        return if (result == InteractionResult.SUCCESS) InteractionResult.SUCCESS else InteractionResult.FAIL
    }


    /**
     * 执行操作的核心共享逻辑
     */
    private fun performAction(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): InteractionResult {
        if (canApply(user, target, stack, hand)) {
            // 执行操作
            applyEffect(user, target, stack, hand)

            // 消耗物品
            consumeItem(user, target, stack, hand)

            // 发送消息
            getSuccessMessages(user, target, stack)?.let { messages ->
                messages.userSuccessMessage?.let { user.sendSystemMessage(it, false) }
                messages.userExtraMessage?.let { user.sendSystemMessage(it, false) }

                // 如果目标是其他玩家，也给他们发送消息
                if (target != user && target is Player) {
                    messages.targetSuccessMessage?.let { target.sendSystemMessage(it, false) }
                    messages.targetExtraMessage?.let { target.sendSystemMessage(it, false) }
                }
            }

            return InteractionResult.SUCCESS
        } else {
            // 发送失败消息
            getFailureMessage(user, target, stack)?.let { user.sendSystemMessage(it, true) }
            return InteractionResult.FAIL
        }
    }
}