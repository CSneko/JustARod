package org.cneko.justarod.item.bio
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.CustomData
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.getEnchantmentLevel
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.entity.Pregnant
import org.cneko.toneko.common.mod.entities.INeko

class ClonerDevice: Item(JRIds.itemProps("cloner_device").stacksTo(1)) {

    enum class ClonerState {
        EMPTY,        // 未采集
        COLLECTED,    // 已采集
        TRANSFERRED   // 已转移
    }

    override fun interactLivingEntity(iStack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        if (user.level().isClientSide) return InteractionResult.PASS
        if (entity is Player) return InteractionResult.PASS

        user as INeko
        if (user.nekoEnergy>=30){
            user.nekoEnergy-=30
        } else {
            (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§c能量不足，无法使用克隆装置。"))
            return InteractionResult.PASS
        }
        val stack = user.getItemInHand(hand)
        val state = stack.get(JRComponents.CLONER_STATE)?.let { ClonerState.valueOf(it) } ?: ClonerState.EMPTY
        val storedType = stack.get(JRComponents.ENTITY_TYPE)

        val handled = when (state) {
            ClonerState.EMPTY -> {
                // 采集细胞
                // 26.x：addAdditionalSaveData 为 protected 且签名改为 ValueOutput；
                // 改用 TagValueOutput 桥接 saveWithoutId 拿到完整实体 NBT
                val bridge = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(net.minecraft.util.ProblemReporter.DISCARDING)
                entity.saveWithoutId(bridge)
                val entNbt = bridge.buildResult()
                entNbt.putString("id", net.minecraft.world.entity.EntityType.getKey(entity.type).toString())
                stack.set(JRComponents.CLONER_ENTITY_NBT, CustomData.of(entNbt))
                stack.set(JRComponents.ENTITY_TYPE, entity.type)
                stack.set(JRComponents.CLONER_TRANSFERRED, false)
                stack.set(JRComponents.CLONER_STATE, ClonerState.COLLECTED.name)

                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§a已采集细胞（卵细胞+体细胞）。"))
                true
            }

            ClonerState.COLLECTED -> {
                // 已采集但未转移，普通右键不做任何操作
                false
            }

            ClonerState.TRANSFERRED -> {
                if (storedType == null || storedType != entity.type) {
                    (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§c目标不是同类生物，无法产生后代。"))
                    true
                } else {
                    if (entity is Pregnant && entity.isFemale) {
                        entity.setChildrenType(storedType)
                        entity.setPregnant(20 * 60 * 5) // 5分钟怀孕
                        clearData(stack)
                        (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§a成功将细胞注入，目标已怀孕。"))
                        true
                    } else {
                        val world = entity.level()
                        if (world is ServerLevel) {
                            val storedNbt = stack.get(JRComponents.CLONER_ENTITY_NBT)
                            var baby: net.minecraft.world.entity.Entity? = null
                            if (storedNbt != null) {
                                val copy = storedNbt.copyTag()
                                copy.remove("Age")
                                copy.remove("AgeTicks")
                                copy.remove("GrowingAge")
                                copy.remove("UUID")
                                copy.putInt("Age", -24000)
                                // 26.x：由保存的实体 NBT 直接创建实体（TagValueInput 桥接）
                                val input = net.minecraft.world.level.storage.TagValueInput.create(
                                    net.minecraft.util.ProblemReporter.DISCARDING, world.registryAccess(), copy
                                )
                                baby = net.minecraft.world.entity.EntityType.create(input, world, EntitySpawnReason.BREEDING).orElse(null)
                            }
                            if (baby != null && baby is LivingEntity) {
                                try {
                                    val method = baby.javaClass.methods.firstOrNull {
                                        it.name == "setBaby" || it.name == "setChild"
                                    }
                                    method?.invoke(baby)
                                } catch (_: Exception) {}
                                baby.setPos(entity.x, entity.y, entity.z)
                                world.addFreshEntity(baby)
                                clearData(stack)
                                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§a成功生成幼崽！"))
                                true
                            } else false
                        } else false
                    }
                }
            }
        }

        return if (handled) InteractionResult.SUCCESS else InteractionResult.PASS
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        val stack = user.getItemInHand(hand)
        if (world.isClientSide) return InteractionResult.PASS

        // 按住 Shift 时尝试细胞核转移
        if (user.isShiftKeyDown()) {
            val transferred = stack.get(JRComponents.CLONER_TRANSFERRED) ?: false
            val storedType = stack.get(JRComponents.ENTITY_TYPE)

            if (storedType == null) {
                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§c尚未采集任何细胞，无法转移。"))
                return InteractionResult.SUCCESS
            }

            if (transferred) {
                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§e细胞核已转移，无需再次操作。"))
                return InteractionResult.SUCCESS
            }

            val success = user.random.nextFloat() < 0.10f + 0.2 * (stack.getEnchantmentLevel(world, JREnchantments.PRECISION))
            if (success) {
                stack.set(JRComponents.CLONER_TRANSFERRED, true)
                stack.set(JRComponents.CLONER_STATE, ClonerState.TRANSFERRED.name) // ✅ 同步状态
                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§a细胞核转移成功！"))
            } else {
                clearData(stack)
                (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§c细胞核转移失败。"))
            }
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    private fun clearData(stack: ItemStack) {
        stack.remove(JRComponents.ENTITY_TYPE)
        stack.remove(JRComponents.CLONER_ENTITY_NBT)
        stack.remove(JRComponents.CLONER_TRANSFERRED)
        stack.remove(JRComponents.CLONER_STATE)
    }

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        val storedType = stack.get(JRComponents.ENTITY_TYPE)
        if (storedType != null) {
            adder.accept(Component.literal("§7生物种类: ${storedType.description.string}"))
            adder.accept(Component.literal("§e包含细胞：卵细胞 + 体细胞"))
        }
        val transferred = stack.get(JRComponents.CLONER_TRANSFERRED) ?: false
        adder.accept(Component.literal("§6细胞核转移：${if (transferred) "已转移" else "未转移（Shift+右键尝试转移）"}"))
    }
}
