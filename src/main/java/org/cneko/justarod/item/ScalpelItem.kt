package org.cneko.justarod.item

import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.BuiltinRegistries
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.containsEnchantment
import org.cneko.justarod.effect.JREffects

class ScalpelItem(settings: Settings): Item(settings.maxCount(1).maxDamage(4)) {
    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Text?>?, type: TooltipType?) {
        super.appendTooltip(stack, context, tooltip, type)
        stack?.let { it ->
            if (it.containsEnchantment(JREnchantments.HYSTERECTOMY)){
                tooltip?.add(Text.of("§c使用它可进行子宫切除"))
                tooltip?.add(Text.of("§c此操作会永久切除子宫，请谨慎使用！"))
            }else if (it.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)){
                tooltip?.add(Text.of("§a使用它可以安装子宫"))
            }
        }
    }

    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        if (user != null && world?.isClient == false){
            val stack = user.getStackInHand(hand)
            val offHandStack = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
            if (stack.damage >= stack.maxDamage){
                return TypedActionResult.fail(stack)
            }
            if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
                if (user.isHysterectomy) {
                    user.sendMessage(Text.of("你已经切除过了！无法再次使用！"))
                }else{
                    user.isHysterectomy = true
                    // 扣血
                    user.damage(user.world.damageSources.generic(), 10f)
                    // 缓慢
                    user.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1))
                    // 挖掘疲劳
                    user.addStatusEffect(StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 1))
                    // 晕倒
                    user.addStatusEffect(StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.FAINT_EFFECT), 300, 1))
                    // 掉落子宫
                    user.dropStack(ItemStack(JRItems.UTERUS))
                    user.sendMessage(Text.of("已进行子宫切除！"))
                    return TypedActionResult.success(stack)
                }
            }else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)){
                if (!user.isHysterectomy){
                    user.sendMessage(Text.of("§c不可以同时安装多个子宫！"))
                }else if (!offHandStack.isOf(JRItems.UTERUS)){
                    user.sendMessage(Text.of("§c副手必须拿着子宫哦"))
                }else{
                    // 扣血
                    user.damage(user.world.damageSources.generic(), 10f)
                    // 缓慢
                    user.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1))
                    // 挖掘疲劳
                    user.addStatusEffect(StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 1))
                    // 晕倒
                    user.addStatusEffect(StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.FAINT_EFFECT), 300, 1))
                    // 减少一个子宫
                    offHandStack.decrement(1)
                    user.isHysterectomy = false
                    user.sendMessage(Text.of("已安装子宫！"))
                }
            }
        }
        return super.use(world, user, hand)
    }
}