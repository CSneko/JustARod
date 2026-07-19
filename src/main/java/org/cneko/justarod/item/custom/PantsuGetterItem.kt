package org.cneko.justarod.item.custom

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import org.cneko.justarod.item.JRComponents

class PantsuGetterItem(properties: Properties) : Item(properties) {

    override fun useOnEntity(stack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        if (entity.level().isClientSide) return InteractionResult.PASS

        // 检查目标腿部装备
        val legStack = entity.getEquippedStack(EquipmentSlot.LEGS)

        if (!legStack.isEmpty && legStack.item is PantsuItem) {
            // 1. 复制胖次
            val stolenPantsu = legStack.copy()

            // 2. 标记原主人的名字 (如果还没有主人)
            if (!stolenPantsu.contains(JRComponents.OWNER)) {
                // 如果是玩家，用玩家名；如果是生物，用自定义名或类型名
                val ownerName = entity.name.string
                stolenPantsu.set(JRComponents.OWNER, ownerName)
            }

            // 3. 给予偷窃者
            if (!user.inventory.insertStack(stolenPantsu)) {
                user.dropItem(stolenPantsu, false)
            }

            // 4. 移除受害者身上的胖次
            entity.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY)

            // 5. 反馈
            user.sendSystemMessage(Component.literal("§d你成功偷走了 ${entity.name.string} 的胖次！"), true)
            // 给受害者发消息
            if (entity is Player) {
                entity.sendSystemMessage(Component.literal("§c感觉下半身凉飕飕的..."), true)
            }


            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }
}