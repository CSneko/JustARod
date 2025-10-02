package org.cneko.justarod.item.bio

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
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

    override fun useOnEntity(iStack: ItemStack, user: PlayerEntity, entity: LivingEntity, hand: Hand): ActionResult {
        if (user.world.isClient) return ActionResult.PASS
        if (entity is PlayerEntity) return ActionResult.PASS

        user as INeko
        if (user.nekoEnergy>=30){
            user.nekoEnergy-=30
        } else {
            user.sendMessage(Text.of("§c能量不足，无法使用克隆装置。"), true)
            return ActionResult.PASS
        }
        val stack = user.getStackInHand(hand)
        val state = stack.get(JRComponents.CLONER_STATE)?.let { ClonerState.valueOf(it) } ?: ClonerState.EMPTY
        val storedType = stack.get(JRComponents.ENTITY_TYPE)

        val handled = when (state) {
            ClonerState.EMPTY -> {
                // 采集细胞
                val entNbt = NbtCompound()
                entity.writeNbt(entNbt)
                stack.set(JRComponents.CLONER_ENTITY_NBT, net.minecraft.component.type.NbtComponent.of(entNbt))
                stack.set(JRComponents.ENTITY_TYPE, entity.type)
                stack.set(JRComponents.CLONER_TRANSFERRED, false)
                stack.set(JRComponents.CLONER_STATE, ClonerState.COLLECTED.name)

                user.sendMessage(Text.of("§a已采集细胞（卵细胞+体细胞）。"), true)
                true
            }

            ClonerState.COLLECTED -> {
                // 已采集但未转移，普通右键不做任何操作
                false
            }

            ClonerState.TRANSFERRED -> {
                if (storedType == null || storedType != entity.type) {
                    user.sendMessage(Text.of("§c目标不是同类生物，无法产生后代。"), true)
                    true
                } else {
                    if (entity is Pregnant && entity.isFemale) {
                        entity.setChildrenType(storedType)
                        entity.setPregnant(20 * 60 * 5) // 5分钟怀孕
                        clearData(stack)
                        user.sendMessage(Text.of("§a成功将细胞注入，目标已怀孕。"), true)
                        true
                    } else {
                        val world = entity.world
                        if (world is ServerWorld) {
                            val baby = storedType.create(world)
                            if (baby != null && baby is LivingEntity) {
                                val storedNbt = stack.get(JRComponents.CLONER_ENTITY_NBT)
                                if (storedNbt != null) {
                                    val copy = storedNbt.copyNbt()
                                    copy.remove("Age")
                                    copy.remove("AgeTicks")
                                    copy.remove("GrowingAge")
                                    copy.remove("UUID")
                                    copy.putInt("Age", -24000)
                                    try { baby.readNbt(copy) } catch (_: Exception) {}
                                }
                                try {
                                    val method = baby.javaClass.methods.firstOrNull {
                                        it.name.equals("setBaby", true) || it.name.equals("setChild", true)
                                    }
                                    method?.invoke(baby, true)
                                } catch (_: Exception) {}
                                baby.setPos(entity.x, entity.y, entity.z)
                                world.spawnEntity(baby)
                                clearData(stack)
                                user.sendMessage(Text.of("§a成功生成幼崽！"), true)
                                true
                            } else false
                        } else false
                    }
                }
            }
        }

        return if (handled) ActionResult.SUCCESS else ActionResult.PASS
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.pass(stack)

        // 按住 Shift 时尝试细胞核转移
        if (user.isSneaking) {
            val transferred = stack.get(JRComponents.CLONER_TRANSFERRED) ?: false
            val storedType = stack.get(JRComponents.ENTITY_TYPE)

            if (storedType == null) {
                user.sendMessage(Text.of("§c尚未采集任何细胞，无法转移。"), true)
                return TypedActionResult.success(stack)
            }

            if (transferred) {
                user.sendMessage(Text.of("§e细胞核已转移，无需再次操作。"), true)
                return TypedActionResult.success(stack)
            }

            val success = user.random.nextFloat() < 0.10f + 0.2 * (stack.getEnchantmentLevel(world, JREnchantments.PRECISION))
            if (success) {
                stack.set(JRComponents.CLONER_TRANSFERRED, true)
                stack.set(JRComponents.CLONER_STATE, ClonerState.TRANSFERRED.name) // ✅ 同步状态
                user.sendMessage(Text.of("§a细胞核转移成功！"), true)
            } else {
                clearData(stack)
                user.sendMessage(Text.of("§c细胞核转移失败。"), true)
            }
            return TypedActionResult.success(stack)
        }

        return TypedActionResult.pass(stack)
    }

    private fun clearData(stack: ItemStack) {
        stack.remove(JRComponents.ENTITY_TYPE)
        stack.remove(JRComponents.CLONER_ENTITY_NBT)
        stack.remove(JRComponents.CLONER_TRANSFERRED)
        stack.remove(JRComponents.CLONER_STATE)
    }

    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        val storedType = stack.get(JRComponents.ENTITY_TYPE)
        if (storedType != null) {
            tooltip.add(Text.of("§7生物种类: ${storedType.name.string}"))
            tooltip.add(Text.of("§e包含细胞：卵细胞 + 体细胞"))
        }
        val transferred = stack.get(JRComponents.CLONER_TRANSFERRED) ?: false
        tooltip.add(Text.of("§6细胞核转移：${if (transferred) "已转移" else "未转移（Shift+右键尝试转移）"}"))
    }
}
