package org.cneko.justarod.item.rod

import net.minecraft.component.DataComponentTypes
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.cneko.justarod.JRAttributes
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.Powerable
import org.cneko.justarod.item.JRComponents
import org.cneko.toneko.common.mod.items.BazookaItem.Ammunition
import kotlin.math.sqrt

/*
末地烛啊喵~♡ 我也想被大调查呀♡ 我也想和别人玩扣扣空间喵♡
 */
abstract class EndRodItem(properties: Properties) : Item(properties), EndRodItemInterface {
    override fun onUse(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean,times: Int) : InteractionResult{
        // 添加计数
        val count = stack.getOrDefault(JRComponents.Companion.USED_TIME_MARK, 0)
        stack.set(JRComponents.Companion.USED_TIME_MARK, count + times)

        return InteractionResult.SUCCESS
    }

    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Component>?, type: TooltipFlag?) {
        super.appendHoverText(stack, context, tooltip, type)
        // 将使用次数添加到tooltip中
        val markedCount: Int = stack?.getOrDefault(JRComponents.Companion.USED_TIME_MARK, 0)!!
        tooltip?.add(Component.translatable("item.justarod.end_rod.used_count", markedCount).withStyle(ChatFormatting.GREEN))
        tooltip?.add(Component.translatable("item.justarod.end_rod.owner", stack.getOrDefault(JRComponents.Companion.OWNER,"无")).withStyle(ChatFormatting.YELLOW))
    }

    override fun onCraftByPlayer(stack: ItemStack?, world: Level?, player: Player?) {
        super.onCraftByPlayer(stack, world, player)
        stack?.set(JRComponents.Companion.OWNER, player?.name?.string)
    }
    abstract fun getInstruction(): EndRodInstructions

}

abstract class OtherUsedItem(properties: Properties):EndRodItem(properties), OtherUsedItemInterface {
    override fun useOnOther(stack: ItemStack, world: Level?, user: Player, target: LivingEntity):InteractionResult{
        // 插入其它实体
        if (getInstruction() == EndRodInstructions.USE_ON_OTHER_INSERT){
            // 从手上减少这根末地烛
            if (!user.isCreative) {
                stack.shrink(1)
            }
            target.hurt(JRDamageTypes.grass(user), 3f)
            // TODO : 实现目标实体插入判断逻辑和取出的逻辑
            user.sendSystemMessage(Component.translatable("item.justarod.end_rod.insert_success"))
            return InteractionResult.SUCCESS
        }else if (getInstruction() == EndRodInstructions.USE_ON_OTHER_ATTACK){
            // 攻击其它实体
            target.hurt(JRDamageTypes.grass(user), 1f)
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun useOnEntity(
        stack: ItemStack?,
        user: Player?,
        entity: LivingEntity?,
        hand: InteractionHand?
    ): InteractionResult? {
        if (super.useOnEntity(stack, user, entity, hand) == InteractionResult.FAIL) return InteractionResult.FAIL
        if (!canAcceptEntity(stack!!, entity!!)) return InteractionResult.FAIL
        return useOnOther(stack, user?.world, user!!, entity)
    }


}

open class SelfUsedItem(properties: Properties) : EndRodItem(properties), SelfUsedItemInterface {
    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        val speed = this.getRodSpeed(stack)
        tooltip?.add(Component.translatable("item.justarod.end_rod.speed", speed).withStyle(ChatFormatting.LIGHT_PURPLE))
    }
    override fun inventoryTick(stack: ItemStack, world: Level?, entity: Entity?, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        // 如果耐久为0或者实体不是LivingEntity，则不处理
        if(stack.damage == stack.maxDamage || entity !is LivingEntity) return

        val e:LivingEntity = entity

        // 如果放在副手
        // 修bug:在工具栏第一格也生效
        if (
            e.getItemInHand(InteractionHand.OFF_HAND) == stack //是的,直接用==
            || slot == Int.MIN_VALUE // now works with inserted rods
            ){
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

abstract class BothUsedItem(properties: Properties) : EndRodItem(properties),SelfUsedItemInterface, OtherUsedItemInterface {

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        val speed = this.getRodSpeed(stack)
        tooltip?.add(Component.translatable("item.justarod.end_rod.speed", speed).withStyle(ChatFormatting.LIGHT_PURPLE))
    }
    override fun useOnOther(stack: ItemStack, world: Level?, user: Player, target: LivingEntity):InteractionResult{
        // 插入其它实体
        if (getInstruction() == EndRodInstructions.SELF_AND_OTHER_INSERT){
            // 从手上减少这根末地烛
            if (!user.isCreative){
                stack.shrink(1)
            }
            target.hurt(user.damageSources?.generic(), 3f)
            // TODO : 实现目标实体插入判断逻辑和取出的逻辑
            user.sendSystemMessage(Component.translatable("item.justarod.end_rod.insert_success"))
            return InteractionResult.SUCCESS
        }else if (getInstruction() == EndRodInstructions.USE_ON_OTHER_ATTACK){
            // 攻击其它实体
            target.hurt(user.damageSources?.generic(), 1f)
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun useOnEntity(
        stack: ItemStack?,
        user: Player?,
        entity: LivingEntity?,
        hand: InteractionHand?
    ): InteractionResult? {
        // if (super.useOnEntity(stack, user, entity, hand) == InteractionResult.FAIL) return InteractionResult.FAIL
        if (!canAcceptEntity(stack!!, entity!!)) return InteractionResult.FAIL
        return useOnOther(stack, user?.world, user!!, entity)
    }

    override fun inventoryTick(stack: ItemStack, world: Level?, entity: Entity?, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)

        // 如果耐久为0或者实体不是LivingEntity，则不处理
        if(stack.damage == stack.maxDamage || entity !is LivingEntity) return

        val e:LivingEntity = entity

        // 如果放在副手
        if (
            e.getItemInHand(InteractionHand.OFF_HAND) == stack //是的,直接用==
            || slot == Int.MIN_VALUE // now works with inserted rods
        ){
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
    fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean):InteractionResult{
        val speed = this.getRodSpeed(stack)
        if (this.canDamage(stack, speed)){
           this.hurt(stack, speed, world)
        }else{
            return InteractionResult.FAIL
        }
        if (speed<=0){
            // 喵？
            return InteractionResult.FAIL
        }
        // 这个速度... 什么东西啊
        if (speed >= 10000) return InteractionResult.PASS
        onUse(stack, world, entity, slot, selected,speed)

        // 要... 要高潮了
        entity.addEffect(JREffects.ORGASM_EFFECT,100,sqrt(speed.toFloat()).toInt())

        // 润滑还是得要的哦
        var lubricate = entity.getAttributeValue(JRAttributes.PLAYER_LUBRICATING)

        if (lubricate == 0.toDouble()) lubricate = 1.0

        // 最终的伤害指数
        val amount = speed / (lubricate)

        if (amount >=10){
            // 痛死了！！！
            entity.hurt(JRDamageTypes.sexualExcitement(entity), (amount*0.3).toFloat())
        }
        if (amount >= 100){
            // 被草飞了喵
            val random = world?.random
            entity.move(MoverType.SHULKER_BOX, Vec3((random?.nextFloat()?.times(1) ?: 0f).toDouble()*0.05,
                (random?.nextFloat()?.times(amount) ?: 0f).toDouble()*0.01, (random?.nextFloat()?.times(1) ?: 0f).toDouble()*0.05)
            )
        }

        // 要晕掉惹...
        if (entity is Powerable){
            entity.power = entity.power - 0.1
        }

        // TODO： 淫叫
        return InteractionResult.SUCCESS
    }
    fun getRodSpeed(stack: ItemStack?):Int{
        if (stack != null) return stack.components.getOrDefault(JRComponents.Companion.SPEED,1)
        return 1
    }
}

interface OtherUsedItemInterface: EndRodItemInterface,Ammunition{
    fun canAcceptEntity(stack :ItemStack,entity: Entity):Boolean
    fun useOnOther(stack: ItemStack, world: Level?, user: Player,target: LivingEntity):InteractionResult

    override fun getSpeed(p0: ItemStack?, p1: ItemStack?): Float {
        return 1f
    }

    override fun getCooldownTicks(p0: ItemStack?, p1: ItemStack?): Int {
        return 20
    }

    override fun getMaxDistance(p0: ItemStack?, p1: ItemStack?): Float {
        return 30f
    }

    override fun hitOnEntity(shooter: LivingEntity?, target: LivingEntity?, stack: ItemStack?, p3: ItemStack?) {
        if (shooter is Player){
            if (stack != null && target != null) {
                if (canAcceptEntity(stack, target)) {
                    useOnOther(stack, shooter.world, shooter, target)
                }
            }
        }
    }

    override fun hitOnAir(p0: LivingEntity?, p1: BlockPos?, p2: ItemStack?, p3: ItemStack?) {
    }

    override fun hitOnBlock(p0: LivingEntity?, p1: BlockPos?, p2: ItemStack?, p3: ItemStack?) {
    }
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
    fun onUse(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean, times: Int) : InteractionResult
    fun damage(stack: ItemStack, amount: Int, world: Level?) {
        stack.damage += getDamageAmount(stack, amount, world)
    }

    fun getDamageAmount(stack: ItemStack, amount: Int, world: Level?): Int {
        // 无法破坏
        if (stack.components.contains(DataComponentTypes.UNBREAKABLE)){
            return 0
        }
        // 获取物品上的耐久附魔等级
        val rm = world?.registryAccess()
        val unbreakingLevel = stack.enchantments.getLevel(rm?.get(BuiltInRegistries.ENCHANTMENT)?.entryOf(Enchantments.UNBREAKING))
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


fun LivingEntity.addEffect(effect: MobEffect?, duration: Int, amplifier: Int) {
    effect?.let {
        this.addEffect(MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getOrThrow(it), duration, amplifier))
    }
}
fun LivingEntity?.addEffect(effect: Holder<MobEffect>?, duration: Int, amplifier: Int) {
    this?.addEffect(MobEffectInstance(effect, duration, amplifier))
}
fun LivingEntity.hasEffect(effect: MobEffect?): Boolean {
    return this.hasEffect(BuiltInRegistries.MOB_EFFECT.getOrThrow(effect))
}