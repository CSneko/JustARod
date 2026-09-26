package org.cneko.justarod.item.syringe
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.core.registries.BuiltInRegistries
import org.cneko.justarod.effect.JREffects

/*
这个的话呢.... 怎么说呢....
它...
可能...
就是...
咱也不了解
 */
class BremelanotideItem : BaseSyringeItem(JRIds.itemProps("bremelanotide")){
    companion object{
        const val CHEMICAL_FORMULA = "C50H68N14O10"
    }

    override fun applyEffect(target: LivingEntity) {
        target.addEffect(MobEffectInstance(MobEffects.NAUSEA, 600, 1))
        JREffects.ESTRUS_EFFECT?.let {
            // 26.x：JREffects 已是 Holder，直接使用
            target.addEffect(MobEffectInstance(it, 5000, 1))
        }
    }
}
