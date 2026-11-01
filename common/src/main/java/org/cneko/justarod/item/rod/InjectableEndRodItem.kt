package org.cneko.justarod.item.rod

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Entity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import net.minecraft.world.effect.MobEffects
import org.cneko.justarod.JRIds
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.packet.InjectableFluidPayload
import java.util.function.Consumer

/**
 * 注入式末地烛：插入逻辑复用 [SelfUsedItem]（副手每 tick 使用一次），
 * 额外提供「储存最多 3 桶单一流体」和「连续插入 15 秒后把储存的流体喷进去」。
 *
 * - 主手持本物品、副手持水/岩浆/牛奶桶时右键：灌入 1000 mB（只允许一种流体）。
 * - 插入累计 300 tick（15 秒）后：清空 1000 mB，给使用者发滤镜并挂上对应 buff：
 *   水 → 「喷... 喷进来了...」+ 缓慢 I（3 分钟，屁股持续冒水花）；
 *   牛奶 → 同上，粒子换成白色；
 *   岩浆 → 「好烫...」，效果等同点燃 6 秒，入水即灭。
 * - 无耐久；没有流体时完全不触发（连插入的高潮/润滑那套效果都不走）。
 */
class InjectableEndRodItem : SelfUsedItem(
    JRIds.itemProps("injectable_end_rod")
        .stacksTo(1)
        .component(JRComponents.Companion.USED_TIME_MARK, 0)
        .component(JRComponents.Companion.SPEED, 1)
) {
    companion object {
        /** 容量：3 桶。 */
        const val MAX_FLUID = 3000

        /** 一桶 = 1000 mB。 */
        const val BUCKET_VOLUME = 1000

        /** 插入 15 秒后喷入。 */
        const val INJECT_TICKS = 300

        /** 全屏滤镜持续时间：15 秒。 */
        const val FILTER_TICKS = 300

        /** 水/牛奶的 buff 持续时间：3 分钟。 */
        const val FLUID_EFFECT_TICKS = 3600

        /** 岩浆 buff 的持续时间：6 秒（等同于燃烧时间）。 */
        const val LAVA_EFFECT_TICKS = 120

        const val WATER = "minecraft:water"
        const val LAVA = "minecraft:lava"
        const val MILK = "minecraft:milk"

        fun fluidOf(stack: ItemStack): String? = stack.get(JRComponents.Companion.INJECTABLE_FLUID)

        fun amountOf(stack: ItemStack): Int = stack.getOrDefault(JRComponents.Companion.INJECTABLE_FLUID_AMOUNT, 0)

        fun progressOf(stack: ItemStack): Int = stack.getOrDefault(JRComponents.Companion.USED_TIME_MARK, 0)

        /** 副手桶对应的流体 id；不是可灌入的桶则返回 null。 */
        fun bucketFluid(stack: ItemStack): String? = when {
            stack.`is`(Items.WATER_BUCKET) -> WATER
            stack.`is`(Items.LAVA_BUCKET) -> LAVA
            stack.`is`(Items.MILK_BUCKET) -> MILK
            else -> null
        }

        fun fluidName(fluid: String): Component =
            Component.translatable("justarod.fluid." + fluid.replace(':', '.'))
    }

    /** 无耐久：基类判断「还能不能扣耐久」时永远放行。 */
    override fun canDamage(stack: ItemStack, amount: Int): Boolean = true

    /** 无耐久：故意不扣。 */
    override fun damage(stack: ItemStack, amount: Int, world: Level?) {
    }

    /**
     * 只有副手会触发；没有流体时什么都不做——连 [useOnSelf] 都不调用，
     * 所以高潮/润滑那一整串效果也不会出现，进度也不会涨。
     */
    override fun inventoryTick(
        stack: ItemStack,
        world: ServerLevel,
        entity: Entity,
        slot: EquipmentSlot?
    ) {
        if (entity !is LivingEntity) return
        if (world.isClientSide) return
        if (!isHeldInOffhand(entity, stack)) return
        if (amountOf(stack) < BUCKET_VOLUME) {
            // 没流体：清掉残留进度并保持静默
            if (progressOf(stack) != 0) stack.set(JRComponents.Companion.USED_TIME_MARK, 0)
            return
        }
        useOnSelf(stack, world, entity, slot?.ordinal ?: -1, true)
    }

    /** 已插入状态：规则同上——无流体就静默，有流体才走插入效果（也不会掉耐久）。 */
    override fun tickInserted(stack: ItemStack, world: Level?, entity: LivingEntity) {
        if (amountOf(stack) < BUCKET_VOLUME) {
            if (progressOf(stack) != 0) stack.set(JRComponents.Companion.USED_TIME_MARK, 0)
            return
        }
        useOnSelf(stack, world, entity, -1, true)
    }

    /**
     * 主手右键：从副手桶灌入流体。其它情况交还给默认逻辑（副手插入由 inventoryTick 处理）。
     */
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS
        val stack = player.getItemInHand(InteractionHand.MAIN_HAND)
        val fluid = bucketFluid(player.getItemInHand(InteractionHand.OFF_HAND)) ?: return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS

        val current = fluidOf(stack)
        val amount = amountOf(stack)
        if (current != null && current != fluid) {
            player.sendSystemMessage(Component.translatable("message.justarod.injectable_end_rod.wrong_fluid", fluidName(current)))
            return InteractionResult.FAIL
        }
        if (amount + BUCKET_VOLUME > MAX_FLUID) {
            player.sendSystemMessage(Component.translatable("message.justarod.injectable_end_rod.full"))
            return InteractionResult.FAIL
        }

        stack.set(JRComponents.Companion.INJECTABLE_FLUID, fluid)
        stack.set(JRComponents.Companion.INJECTABLE_FLUID_AMOUNT, amount + BUCKET_VOLUME)
        // 创造模式（无限材料）不消耗桶；用 hasInfiniteMaterials 而不是 isCreative，
        // 后者在 26.x 只看 gameMode()，对模拟玩家/旁观切换不敏感。
        if (!player.hasInfiniteMaterials()) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack(Items.BUCKET))
        }
        player.sendSystemMessage(Component.translatable("message.justarod.injectable_end_rod.filled",
            fluidName(fluid), amount + BUCKET_VOLUME, MAX_FLUID))
        return InteractionResult.SUCCESS
    }

    /** 插入时每 tick 由 [SelfUsedItem] 调用一次，这里累计时间并在满 30 秒时喷入。 */
    override fun onUse(
        stack: ItemStack,
        world: Level?,
        entity: LivingEntity,
        slot: Int,
        selected: Boolean,
        times: Int
    ): InteractionResult {
        val fluid = fluidOf(stack)
        val amount = amountOf(stack)
        if (fluid == null || amount < BUCKET_VOLUME) {
            // 没有可注入的流体时不计时，避免「存液前先攒满 30 秒」。
            if (progressOf(stack) != 0) stack.set(JRComponents.Companion.USED_TIME_MARK, 0)
            return InteractionResult.SUCCESS
        }

        val progress = progressOf(stack) + 1
        if (progress < INJECT_TICKS) {
            stack.set(JRComponents.Companion.USED_TIME_MARK, progress)
            return InteractionResult.SUCCESS
        }

        val remaining = amount - BUCKET_VOLUME
        stack.set(JRComponents.Companion.USED_TIME_MARK, 0)
        stack.set(JRComponents.Companion.INJECTABLE_FLUID_AMOUNT, remaining)
        if (remaining <= 0) stack.remove(JRComponents.Companion.INJECTABLE_FLUID)
        inject(entity, fluid)
        return InteractionResult.SUCCESS
    }

    private fun inject(entity: LivingEntity, fluid: String) {
        if (entity is ServerPlayer) {
            ServerPlayNetworking.send(entity, InjectableFluidPayload(fluid, FILTER_TICKS))
            entity.sendSystemMessage(Component.translatable("message.justarod.injectable_end_rod.injected", fluidName(fluid)))
        }
        when (fluid) {
            // 岩浆：「好烫...」效果等同点燃 6 秒，入水即灭
            LAVA -> entity.addEffect(JREffects.INJECTED_LAVA_EFFECT, LAVA_EFFECT_TICKS, 0)
            // 水与牛奶共用「喷... 喷进来了...」，都附带缓慢 I
            else -> {
                entity.addEffect(JREffects.INJECTED_FLUID_EFFECT, FLUID_EFFECT_TICKS, 0)
                entity.addEffect(MobEffects.SLOWNESS, FLUID_EFFECT_TICKS, 0)
            }
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        adder: Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        val fluid = fluidOf(stack)
        if (fluid == null || amountOf(stack) <= 0) {
            adder.accept(Component.translatable("item.justarod.injectable_end_rod.empty").withStyle(ChatFormatting.GRAY))
            return
        }
        adder.accept(Component.translatable("item.justarod.injectable_end_rod.fluid",
            fluidName(fluid), amountOf(stack), MAX_FLUID).withStyle(ChatFormatting.AQUA))
        adder.accept(Component.translatable("item.justarod.injectable_end_rod.progress",
            progressOf(stack) * 100 / INJECT_TICKS).withStyle(ChatFormatting.DARK_AQUA))
    }
}
