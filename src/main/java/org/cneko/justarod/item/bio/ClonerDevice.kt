package org.cneko.justarod.item.bio

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.TooltipFlag
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.getEnchantmentLevel
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.entity.Pregnant
import org.cneko.toneko.common.mod.entities.INeko

class ClonerDevice: Item(Settings().maxCount(1)) {

    enum class ClonerState {
        EMPTY,        // 未采集
        COLLECTED,    // 已采集
        TRANSFERRED   // 已转移
    }

    override fun useOnEntity(iStack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        if (user.level().isClientSide) return InteractionResult.PASS
        if (entity is Player) return InteractionResult.PASS

        user as INeko
        if (user.nekoEnergy>=30){
            user.nekoEnergy-=30
        } else {
            user.sendSystemMessage(Component.literal("§c能量不足，无法使用克隆装置。"), true)
            return InteractionResult.PASS
        }
        val stack = user.getItemInHand(hand)
        val state = stack.get(JRComponents.CLONER_STATE)?.let { ClonerState.valueOf(it) } ?: ClonerState.EMPTY
        val storedType = stack.get(JRComponents.ENTITY_TYPE)

        val handled = when (state) {
            ClonerState.EMPTY -> {
                // 采集细胞
                val entNbt = CompoundTag()
                entity.addAdditionalSaveData(entNbt)
                stack.set(JRComponents.CLONER_ENTITY_NBT, net.minecraft.component.type.NbtComponent.of(entNbt))
                stack.set(JRComponents.ENTITY_TYPE, entity.type)
                stack.set(JRComponents.CLONER_TRANSFERRED, false)
                stack.set(JRComponents.CLONER_STATE, ClonerState.COLLECTED.name)

                user.sendSystemMessage(Component.literal("§a已采集细胞（卵细胞+体细胞）。"), true)
                true
            }

            ClonerState.COLLECTED -> {
                // 已采集但未转移，普通右键不做任何操作
                false
            }

            ClonerState.TRANSFERRED -> {
                if (storedType == null || storedType != entity.type) {
                    user.sendSystemMessage(Component.literal("§c目标不是同类生物，无法产生后代。"), true)
                    true
                } else {
                    if (entity is Pregnant && entity.isFemale) {
                        entity.setChildrenType(storedType)
                        entity.setPregnant(20 * 60 * 5) // 5分钟怀孕
                        clearData(stack)
                        user.sendSystemMessage(Component.literal("§a成功将细胞注入，目标已怀孕。"), true)
                        true
                    } else {
                        val world = entity.level()
                        if (world is ServerLevel) {
                            val baby = storedType.create(world)
                            if (baby != null && baby is LivingEntity) {
                                val storedNbt = stack.get(JRComponents.CLONER_ENTITY_NBT)
                                if (storedNbt != null) {
                                    val copy = storedNbt.saveWithoutId()
                                    copy.remove("Age")
                                    copy.remove("AgeTicks")
                                    copy.remove("GrowingAge")
                                    copy.remove("UUID")
                                    copy.putInt("Age", -24000)
                                    try { baby.readAdditionalSaveData(copy) } catch (_: Exception) {}
                                }
                                try {
                                    val method = baby.javaClass.methods.firstOrNull {
                                        it.name.equals("setBaby", true) || it.name.equals("setChild", true)
                                    }
                                    method?.invoke(baby, true)
                                } catch (_: Exception) {}
                                baby.setPos(entity.x, entity.y, entity.z)
                                level().addFreshEntity(baby)
                                clearData(stack)
                                user.sendSystemMessage(Component.literal("§a成功生成幼崽！"), true)
                                true
                            } else false
                        } else false
                    }
                }
            }
        }

        return if (handled) InteractionResult.SUCCESS else InteractionResult.PASS
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        if (level().isClientSide) return InteractionResultHolder.pass(stack)

        // 按住 Shift 时尝试细胞核转移
        if (user.isShiftKeyDown()) {
            val transferred = stack.get(JRComponents.CLONER_TRANSFERRED) ?: false
            val storedType = stack.get(JRComponents.ENTITY_TYPE)

            if (storedType == null) {
                user.sendSystemMessage(Component.literal("§c尚未采集任何细胞，无法转移。"), true)
                return InteractionResultHolder.success(stack)
            }

            if (transferred) {
                user.sendSystemMessage(Component.literal("§e细胞核已转移，无需再次操作。"), true)
                return InteractionResultHolder.success(stack)
            }

            val success = user.random.nextFloat() < 0.10f + 0.2 * (stack.getEnchantmentLevel(world, JREnchantments.PRECISION))
            if (success) {
                stack.set(JRComponents.CLONER_TRANSFERRED, true)
                stack.set(JRComponents.CLONER_STATE, ClonerState.TRANSFERRED.name) // ✅ 同步状态
                user.sendSystemMessage(Component.literal("§a细胞核转移成功！"), true)
            } else {
                clearData(stack)
                user.sendSystemMessage(Component.literal("§c细胞核转移失败。"), true)
            }
            return InteractionResultHolder.success(stack)
        }

        return InteractionResultHolder.pass(stack)
    }

    private fun clearData(stack: ItemStack) {
        stack.remove(JRComponents.ENTITY_TYPE)
        stack.remove(JRComponents.CLONER_ENTITY_NBT)
        stack.remove(JRComponents.CLONER_TRANSFERRED)
        stack.remove(JRComponents.CLONER_STATE)
    }

    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, type)
        val storedType = stack.get(JRComponents.ENTITY_TYPE)
        if (storedType != null) {
            tooltip.add(Component.literal("§7生物种类: ${storedType.name.string}"))
            tooltip.add(Component.literal("§e包含细胞：卵细胞 + 体细胞"))
        }
        val transferred = stack.get(JRComponents.CLONER_TRANSFERRED) ?: false
        tooltip.add(Component.literal("§6细胞核转移：${if (transferred) "已转移" else "未转移（Shift+右键尝试转移）"}"))
    }
}
