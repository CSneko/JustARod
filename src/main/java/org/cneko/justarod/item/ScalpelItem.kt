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
            }
        }
    }

    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        if (user != null && world?.isClient == false){
            val stack = user.getStackInHand(hand)
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
                    // 减少一点耐久
                    stack.damage--
                    return TypedActionResult.success(stack)
                }
            }
        }
        return super.use(world, user, hand)
    }
}