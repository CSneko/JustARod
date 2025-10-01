package org.cneko.justarod.item.medical

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.world.World
import org.cneko.justarod.entity.JREntities
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.item.rod.addEffect
import java.util.concurrent.TimeUnit

open class SpermRetrievalDeviceItem(val lifeTime: Int, settings: Settings) : MedicalItem(settings) {

    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        if (!world.isClient && stack.contains(JRComponents.ENTITY_TYPE)) {
            val remaining = stack.get(JRComponents.COLLECTED_TIME) ?: 0
            if (remaining > 0) {
                stack.set(JRComponents.COLLECTED_TIME, remaining - 1)
            } else {
                // 腐坏：清除数据
                stack.remove(JRComponents.ENTITY_TYPE)
                stack.remove(JRComponents.COLLECTED_TIME)
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected)
    }

    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        val hasSemen = stack.contains(JRComponents.ENTITY_TYPE)

        return if (hasSemen) {
            // 已经有精液 → 只能注入
            when {
                target is Pregnant && target.isFemale -> {
                    if (target.isPregnant) {
                        user.sendMessage(Text.of("§c目标已经怀孕，无法注入！"), true)
                        false
                    } else if (target.menstruationCycle != Pregnant.MenstruationCycle.OVULATION) {
                        user.sendMessage(Text.of("§c目标不在排卵期，无法受孕！"), true)
                        false
                    } else {
                        true
                    }
                }
                target is AnimalEntity -> {
                    if (target.loveTicks > 0) {
                        user.sendMessage(Text.of("§c动物正在繁殖冷却中，暂时不能注入！"), true)
                        false
                    } else {
                        true
                    }
                }
                else -> {
                    user.sendMessage(Text.of("§c该目标无法注入精液！"), true)
                    false
                }
            }
        } else {
            // 没有精液 → 只能采集
            when {
                target is Pregnant && target.isMale -> {
                    if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                        user.sendMessage(Text.of("§c目标虚弱，无法采集精液！"), true)
                        false
                    } else {
                        true
                    }
                }
                target is AnimalEntity -> {
                    if (target.loveTicks > 0) {
                        user.sendMessage(Text.of("§c动物正在繁殖冷却中，无法采集！"), true)
                        false
                    } else {
                        true
                    }
                }
                else -> {
                    user.sendMessage(Text.of("§c无法从该目标采集精液！"), true)
                    false
                }
            }
        }
    }



    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text? {
        return Text.of("§c条件不满足或精液已腐坏！")
    }

    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        val hasSemen = stack.contains(JRComponents.ENTITY_TYPE)

        if (hasSemen) {
            // --- 注入逻辑 ---
            if (target is Pregnant && target.isFemale) {
                val type = stack.get(JRComponents.ENTITY_TYPE)
                if (type != null) {
                    if (target.isPregnant) {
                        user.sendMessage(Text.of("§c${target.name.string} 已经怀孕，注入失败！"), true)
                    } else if (target.menstruationCycle != Pregnant.MenstruationCycle.OVULATION) {
                        user.sendMessage(Text.of("§c${target.name.string} 不在排卵期，无法怀孕！"), true)
                    } else {
                        target.setChildrenType(type)
                        target.tryPregnant()
                        val stackInHand = user.getStackInHand(hand)
                        stackInHand.remove(JRComponents.ENTITY_TYPE)
                        stackInHand.remove(JRComponents.COLLECTED_TIME)
                        user.sendMessage(Text.of("§a成功注入精液，${target.name.string} 可能怀孕了！"))
                    }
                }
            } else if (target is AnimalEntity) {
                val type = stack.get(JRComponents.ENTITY_TYPE)
                if (type == target.type) {
                    if (target.loveTicks > 0) {
                        user.sendMessage(Text.of("§c动物正在繁殖冷却中，无法注入！"), true)
                    } else {
                        val stackInHand = user.getStackInHand(hand)
                        stackInHand.remove(JRComponents.ENTITY_TYPE)
                        stackInHand.remove(JRComponents.COLLECTED_TIME)
                        user.sendMessage(Text.of("§a成功为动物注入精液"))
                        val world = target.world
                        if (world is ServerWorld) {
                            // 爱心粒子
                            world.spawnParticles(
                                ParticleTypes.HEART,
                                target.x,
                                target.y + 0.5,
                                target.z,
                                10,
                                0.5,
                                0.5,
                                0.5,
                                1.0
                            )
                            // 生产幼体
                            val child = target.createChild(world, target)
                            if (child != null) {
                                world.spawnEntity(child)
                                child.isBaby = true
                                child.setPos(target.x, target.y + 1, target.z)
                            }
                        }
                    }
                } else {
                    user.sendMessage(Text.of("§c种类不匹配，无法注入！"), true)
                }
            } else {
                user.sendMessage(Text.of("§c该目标无法注入精液！"), true)
            }
        } else {
            if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                user.sendMessage(Text.of("§c目标虚弱，无法采集精液！"), true)
                return
            }
            // --- 采集逻辑 ---
            if (target is Pregnant && target.isMale) {
                if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                    user.sendMessage(Text.of("§c${target.name.string} 太虚弱，无法采集！"), true)
                } else {
                    if (target is PlayerEntity) {
                        stack.set(JRComponents.ENTITY_TYPE, JREntities.SEEEEEX_NEKO)
                    }else {
                        stack.set(JRComponents.ENTITY_TYPE, target.type)
                    }
                    stack.set(JRComponents.COLLECTED_TIME, lifeTime)
                    user.sendMessage(Text.of("§a成功采集到 ${target.name.string} 的精液！"))
                    target.addEffect(StatusEffects.WEAKNESS,20*60*5,0)
                }
            } else if (target is AnimalEntity) {
                if (target.loveTicks > 0) {
                    user.sendMessage(Text.of("§c动物正在繁殖冷却中，无法采集！"), true)
                } else {
                    val stackInHand = user.getStackInHand(hand)
                    stackInHand.set(JRComponents.ENTITY_TYPE, target.type)
                    stackInHand.set(JRComponents.COLLECTED_TIME, lifeTime)
                    user.sendMessage(Text.of("§a成功采集到动物精液！"))
                    target.addEffect(StatusEffects.WEAKNESS,20*60*5,0)
                }
            } else {
                user.sendMessage(Text.of("§c该目标无法采集精液！"), true)
            }
        }
    }



    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
    }

    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages? {
        return ActionMessages(null,null)
    }


    // --- Tooltip 显示腐坏剩余时间 ---
    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        val entityType = stack.get(JRComponents.ENTITY_TYPE)
        val remaining = stack.get(JRComponents.COLLECTED_TIME)
        if (entityType != null && remaining != null) {
            if (remaining > 0) {
                val minutes = TimeUnit.SECONDS.toMinutes(remaining / 20L)
                val seconds = (remaining / 20L) % 60
                tooltip.add(Text.of("§7来源: ${entityType.name.string}"))
                tooltip.add(Text.of("§7剩余腐坏时间: ${minutes}分${seconds}秒"))
            } else {
                tooltip.add(Text.of("§c已腐坏"))
            }
        }
    }

    // --- 食用逻辑 ---
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)

        return if (user.isSneaking) {
            super.use(world, user, hand)
        } else {
            // 非潜行 → 食用逻辑，但只有有精液时才允许饮用
            return if (stack.contains(JRComponents.ENTITY_TYPE)) {
                ItemUsage.consumeHeldItem(world, user, hand)
            } else {
                // 没有 entityType → 不可饮用
                TypedActionResult.pass(stack)
            }
        }
    }


    override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
        if (user is PlayerEntity) {
            user.playSound(SoundEvents.ENTITY_GENERIC_DRINK, 1.0f, 1.0f)
            stack.remove(JRComponents.ENTITY_TYPE)
            stack.remove(JRComponents.COLLECTED_TIME)
        }
        return stack
    }

    override fun getMaxUseTime(stack: ItemStack, user: LivingEntity): Int = 32
    override fun getUseAction(stack: ItemStack): UseAction = UseAction.DRINK
}
