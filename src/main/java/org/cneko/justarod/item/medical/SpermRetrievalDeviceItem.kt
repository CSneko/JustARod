package org.cneko.justarod.item.medical

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import org.cneko.justarod.entity.JREntities
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.item.rod.addEffect
import java.util.concurrent.TimeUnit

open class SpermRetrievalDeviceItem(val lifeTime: Int, settings: Properties) : MedicalItem(properties) {

    override fun inventoryTick(stack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (!level().isClientSide && stack.contains(JRComponents.ENTITY_TYPE)) {
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

    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        val hasSemen = stack.contains(JRComponents.ENTITY_TYPE)

        return if (hasSemen) {
            // 已经有精液 → 只能注入
            when {
                target is Pregnant && target.isFemale -> {
                    if (target.isPregnant) {
                        user.sendSystemMessage(Component.literal("§c目标已经怀孕，无法注入！"), true)
                        false
                    } else if (target.menstruationCycle != Pregnant.MenstruationCycle.OVULATION) {
                        user.sendSystemMessage(Component.literal("§c目标不在排卵期，无法受孕！"), true)
                        false
                    } else {
                        true
                    }
                }
                target is Animal -> {
                    if (target.loveTicks > 0) {
                        user.sendSystemMessage(Component.literal("§c动物正在繁殖冷却中，暂时不能注入！"), true)
                        false
                    } else {
                        true
                    }
                }
                else -> {
                    user.sendSystemMessage(Component.literal("§c该目标无法注入精液！"), true)
                    false
                }
            }
        } else {
            // 没有精液 → 只能采集
            when {
                target is Pregnant && target.isMale -> {
                    if (target.hasEffect(MobEffects.WEAKNESS)) {
                        user.sendSystemMessage(Component.literal("§c目标虚弱，无法采集精液！"), true)
                        false
                    } else {
                        true
                    }
                }
                target is Animal -> {
                    if (target.loveTicks > 0) {
                        user.sendSystemMessage(Component.literal("§c动物正在繁殖冷却中，无法采集！"), true)
                        false
                    } else {
                        true
                    }
                }
                else -> {
                    user.sendSystemMessage(Component.literal("§c无法从该目标采集精液！"), true)
                    false
                }
            }
        }
    }



    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component? {
        return Component.literal("§c条件不满足或精液已腐坏！")
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        val hasSemen = stack.contains(JRComponents.ENTITY_TYPE)

        if (hasSemen) {
            // --- 注入逻辑 ---
            if (target is Pregnant && target.isFemale) {
                val type = stack.get(JRComponents.ENTITY_TYPE)
                if (type != null) {
                    if (target.isPregnant) {
                        user.sendSystemMessage(Component.literal("§c${target.name.string} 已经怀孕，注入失败！"), true)
                    } else if (target.menstruationCycle != Pregnant.MenstruationCycle.OVULATION) {
                        user.sendSystemMessage(Component.literal("§c${target.name.string} 不在排卵期，无法怀孕！"), true)
                    } else {
                        target.setChildrenType(type)
                        target.tryPregnant()
                        val stackInHand = user.getItemInHand(hand)
                        stackInHand.remove(JRComponents.ENTITY_TYPE)
                        stackInHand.remove(JRComponents.COLLECTED_TIME)
                        user.sendSystemMessage(Component.literal("§a成功注入精液，${target.name.string} 可能怀孕了！"))
                    }
                }
            } else if (target is Animal) {
                val type = stack.get(JRComponents.ENTITY_TYPE)
                if (type == target.type) {
                    if (target.loveTicks > 0) {
                        user.sendSystemMessage(Component.literal("§c动物正在繁殖冷却中，无法注入！"), true)
                    } else {
                        val stackInHand = user.getItemInHand(hand)
                        stackInHand.remove(JRComponents.ENTITY_TYPE)
                        stackInHand.remove(JRComponents.COLLECTED_TIME)
                        user.sendSystemMessage(Component.literal("§a成功为动物注入精液"))
                        val world = target.level()
                        if (world is ServerLevel) {
                            // 爱心粒子
                            level().sendParticles(
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
                            val child = target.getBreedOffspring(world, target)
                            if (child != null) {
                                level().addFreshEntity(child)
                                child.isBaby = true
                                child.setPos(target.x, target.y + 1, target.z)
                            }
                        }
                    }
                } else {
                    user.sendSystemMessage(Component.literal("§c种类不匹配，无法注入！"), true)
                }
            } else {
                user.sendSystemMessage(Component.literal("§c该目标无法注入精液！"), true)
            }
        } else {
            if (target.hasEffect(MobEffects.WEAKNESS)) {
                user.sendSystemMessage(Component.literal("§c目标虚弱，无法采集精液！"), true)
                return
            }
            // --- 采集逻辑 ---
            if (target is Pregnant && target.isMale) {
                if (target.hasEffect(MobEffects.WEAKNESS)) {
                    user.sendSystemMessage(Component.literal("§c${target.name.string} 太虚弱，无法采集！"), true)
                } else {
                    if (target is Player) {
                        stack.set(JRComponents.ENTITY_TYPE, JREntities.SEEEEEX_NEKO)
                    }else {
                        stack.set(JRComponents.ENTITY_TYPE, target.type)
                    }
                    stack.set(JRComponents.COLLECTED_TIME, lifeTime)
                    user.sendSystemMessage(Component.literal("§a成功采集到 ${target.name.string} 的精液！"))
                    target.addEffect(MobEffects.WEAKNESS,20*60*5,0)
                }
            } else if (target is Animal) {
                if (target.loveTicks > 0) {
                    user.sendSystemMessage(Component.literal("§c动物正在繁殖冷却中，无法采集！"), true)
                } else {
                    val stackInHand = user.getItemInHand(hand)
                    stackInHand.set(JRComponents.ENTITY_TYPE, target.type)
                    stackInHand.set(JRComponents.COLLECTED_TIME, lifeTime)
                    user.sendSystemMessage(Component.literal("§a成功采集到动物精液！"))
                    target.addEffect(MobEffects.WEAKNESS,20*60*5,0)
                }
            } else {
                user.sendSystemMessage(Component.literal("§c该目标无法采集精液！"), true)
            }
        }
    }



    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages? {
        return ActionMessages(null,null)
    }


    // --- Tooltip 显示腐坏剩余时间 ---
    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        val entityType = stack.get(JRComponents.ENTITY_TYPE)
        val remaining = stack.get(JRComponents.COLLECTED_TIME)
        if (entityType != null && remaining != null) {
            if (remaining > 0) {
                val minutes = TimeUnit.SECONDS.toMinutes(remaining / 20L)
                val seconds = (remaining / 20L) % 60
                tooltip.add(Component.literal("§7来源: ${entityType.name.string}"))
                tooltip.add(Component.literal("§7剩余腐坏时间: ${minutes}分${seconds}秒"))
            } else {
                tooltip.add(Component.literal("§c已腐坏"))
            }
        }
    }

    // --- 食用逻辑 ---
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)

        return if (user.isShiftKeyDown()) {
            super.use(world, user, hand)
        } else {
            // 非潜行 → 食用逻辑，但只有有精液时才允许饮用
            return if (stack.contains(JRComponents.ENTITY_TYPE)) {
                ItemUtils.startUsingItem(world, user, hand)
            } else {
                // 没有 entityType → 不可饮用
                InteractionResultHolder.pass(stack)
            }
        }
    }


    override fun finishUsing(stack: ItemStack, world: Level, user: LivingEntity): ItemStack {
        if (user is Player) {
            user.playSound(SoundEvents.ENTITY_GENERIC_DRINK, 1.0f, 1.0f)
            stack.remove(JRComponents.ENTITY_TYPE)
            stack.remove(JRComponents.COLLECTED_TIME)
        }
        return stack
    }

    override fun getMaxUseTime(stack: ItemStack, user: LivingEntity): Int = 32
    override fun getUseAction(stack: ItemStack): UseAnim = UseAnim.DRINK
}
