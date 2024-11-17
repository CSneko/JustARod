package org.cneko.justarod.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.ai.control.MoveControl
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.effect.JREffects
import kotlin.math.sqrt

abstract class EndRodItem(settings: Settings) : Item(settings), EndRodItemInterface {
    override fun onUse(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean) : ActionResult{
        // 添加计数
        val count = stack.getOrDefault(JRComponents.USED_TIME_MARK, 0)
        stack.set(JRComponents.USED_TIME_MARK, count + 1)

        return ActionResult.SUCCESS
    }

    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Text>?, type: TooltipType?) {
        super.appendTooltip(stack, context, tooltip, type)
        // 将使用次数添加到tooltip中
        val markedCount: Int = stack?.getOrDefault(JRComponents.USED_TIME_MARK, 0)!!
        tooltip?.add(Text.translatable("item.justarod.end_rod.used_count", markedCount).formatted(Formatting.GREEN))
    }
    abstract fun getInstruction(): EndRodInstructions

}

abstract class OtherUsedItem(settings: Settings):EndRodItem(settings), OtherUsedItemInterface {
    override fun useOnOther(stack: ItemStack, world: World?, user: PlayerEntity, target: LivingEntity):ActionResult{
        // 插入其它实体
        if (getInstruction() == EndRodInstructions.USE_ON_OTHER_INSERT){
            // 从手上减少这根末地烛
            user.inventory.removeStack(user.inventory.selectedSlot)
            target.damage(user.damageSources?.generic(), 3f)
            // TODO : 实现目标实体插入判断逻辑和取出的逻辑
            user.sendMessage(Text.translatable("item.justarod.end_rod.insert_success"))
            return ActionResult.SUCCESS
        }else if (getInstruction() == EndRodInstructions.USE_ON_OTHER_ATTACK){
            // 攻击其它实体
            target.damage(user.damageSources?.generic(), 1f)
            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        if (super.useOnEntity(stack, user, entity, hand) == ActionResult.FAIL) return ActionResult.FAIL
        if (!canAcceptEntity(stack!!, entity!!)) return ActionResult.FAIL
        return useOnOther(stack, user?.world, user!!, entity)
    }

}

open class SelfUsedItem(settings: Settings) : EndRodItem(settings), SelfUsedItemInterface {
    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val speed = this.getSpeed(stack)
        tooltip?.add(Text.translatable("item.justarod.end_rod.speed", speed).formatted(Formatting.LIGHT_PURPLE))
    }
    override fun inventoryTick(stack: ItemStack, world: World?, entity: Entity?, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)

        // 如果耐久为0或者实体不是LivingEntity，则不处理
        if(stack.damage == stack.maxDamage || entity !is LivingEntity) return

        val e:LivingEntity = entity

        // 如果放在副手
        if (e.offHandStack == stack){
            // 减少一点耐久 (即使没耐久也不损坏)
            stack.damage++
            // 执行
            useOnSelf(stack, world, e, slot, selected)
        }

    }

    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.USE_ON_SELF
    }

}

abstract class BothUsedItem(settings: Settings) : EndRodItem(settings),SelfUsedItemInterface, OtherUsedItemInterface {

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val speed = this.getSpeed(stack)
        tooltip?.add(Text.translatable("item.justarod.end_rod.speed", speed).formatted(Formatting.LIGHT_PURPLE))
    }
    override fun useOnOther(stack: ItemStack, world: World?, user: PlayerEntity, target: LivingEntity):ActionResult{
        // 插入其它实体
        if (getInstruction() == EndRodInstructions.SELF_AND_OTHER_INSERT){
            // 从手上减少这根末地烛
            user.inventory.removeStack(user.inventory.selectedSlot)
            target.damage(user.damageSources?.generic(), 3f)
            // TODO : 实现目标实体插入判断逻辑和取出的逻辑
            user.sendMessage(Text.translatable("item.justarod.end_rod.insert_success"))
            return ActionResult.SUCCESS
        }else if (getInstruction() == EndRodInstructions.USE_ON_OTHER_ATTACK){
            // 攻击其它实体
            target.damage(user.damageSources?.generic(), 1f)
            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        if (super.useOnEntity(stack, user, entity, hand) == ActionResult.FAIL) return ActionResult.FAIL
        if (!canAcceptEntity(stack!!, entity!!)) return ActionResult.FAIL
        return useOnOther(stack, user?.world, user!!, entity)
    }

    override fun inventoryTick(stack: ItemStack, world: World?, entity: Entity?, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)

        // 如果耐久为0或者实体不是LivingEntity，则不处理
        if(stack.damage == stack.maxDamage || entity !is LivingEntity) return

        val e:LivingEntity = entity

        // 如果放在副手
        if (e.offHandStack == stack){
            // 减少一点耐久 (即使没耐久也不损坏)
            stack.damage++
            // 执行
            useOnSelf(stack, world, e, slot, selected)
        }

    }
}

/**
 * 给自己使用的末地烛接口
 */
interface SelfUsedItemInterface : EndRodItemInterface{
    /**
     * 使用末地烛
     * @param stack 使用的末地烛
     * @param world 使用的末地烛所在的世界
     * @param entity 使用的末地烛所在的实体
     * @param slot 使用的末地烛所在的槽位
     * @param selected 是否是选中的末地烛
     * @return 使用结果
     */
    fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean):ActionResult{
        val speed = this.getSpeed(stack)
        if (speed<=0){
            // 喵？
            return ActionResult.PASS
        }
        if (speed < 10000) {
            for (i: Int in 0..speed) {
                onUse(stack, world, entity, slot, selected)
            }
        }
        // 给予玩家gc效果
        JREffects.ORGASM_EFFECT?.let {
            val orgasm =
                StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(it), 100, sqrt(speed.toFloat()).toInt())
            entity.addStatusEffect(orgasm)
        }
       if (this.canDamage(stack, speed)){
           this.damage(stack, speed, world)
       }
        if (speed >=10){
            entity.damage(JRDamageTypes.sexualExcitement(entity), (speed*0.3).toFloat())
        }
        if (speed >= 100){
            // 被草飞了喵
            val random = world?.random
            entity.move(MovementType.SHULKER_BOX, Vec3d((random?.nextFloat()?.times(1) ?: 0f).toDouble()*0.05,
                (random?.nextFloat()?.times(speed) ?: 0f).toDouble()*0.01, (random?.nextFloat()?.times(1) ?: 0f).toDouble()*0.05)
            )
        }
        // TODO： 淫叫
        return ActionResult.SUCCESS
    }
    fun getSpeed(stack: ItemStack?):Int{
        if (stack != null) return stack.components.getOrDefault(JRComponents.SPEED,1)
        return 1
    }
}
interface OtherUsedItemInterface: EndRodItemInterface{
    fun canAcceptEntity(stack :ItemStack,entity: Entity):Boolean
    fun useOnOther(stack: ItemStack, world: World?, user: PlayerEntity,target: LivingEntity):ActionResult
}
interface EndRodItemInterface{
    /**
     * 使用末地烛
     * @param stack 使用的末地烛
     * @param world 使用的末地烛所在的世界
     * @param entity 使用的末地烛所在的实体
     * @param slot 使用的末地烛所在的槽位
     * @param selected 是否是选中的末地烛
     */
    fun onUse(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean) : ActionResult
    fun damage(stack: ItemStack, amount: Int, world: World?) {
        stack.damage += getDamageAmount(stack, amount, world)
    }

    fun getDamageAmount(stack: ItemStack, amount: Int, world: World?): Int {
        // 无法破坏
        if (stack.components.contains(DataComponentTypes.UNBREAKABLE)){
            return 0
        }
        // 获取物品上的耐久附魔等级
        val rm = world?.registryManager;
        val unbreakingLevel = stack.enchantments.getLevel(rm?.get(RegistryKeys.ENCHANTMENT)?.entryOf(Enchantments.UNBREAKING))
        var total = 0
        // 遍历每一点潜在耐久损失，进行概率判定
        for (i in 1..amount) {
            // 计算损失耐久的概率
            val chance = 1.0 / (unbreakingLevel + 1)

            // 判断是否真正损失耐久
            if (Math.random() < chance) {
                total++
            }
        }
        return total
    }

    fun canDamage(stack: ItemStack, amount: Int):Boolean{
        return stack.damage+amount < stack.maxDamage
    }
}
enum class EndRodInstructions{
    USE_ON_SELF,
    USE_ON_OTHER_INSERT,
    USE_ON_OTHER_ATTACK,
    SELF_AND_OTHER_INSERT,
    SELF_AND_OTHER_ATTACK
}