package org.cneko.justarod.item.syringe

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
class BremelanotideItem : BaseSyringeItem(Properties()){
    companion object{
        const val CHEMICAL_FORMULA = "C50H68N14O10"
    }

    override fun applyEffect(target: LivingEntity) {
        target.addEffect(MobEffectInstance(MobEffects.CONFUSION, 600, 1))
        JREffects.ESTRUS_EFFECT?.let {
            target.addEffect(MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(it), 5000, 1))
        }
    }
}
