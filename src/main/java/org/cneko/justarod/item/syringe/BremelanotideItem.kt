package org.cneko.justarod.item.syringe

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.addEffect

/*
这个的话呢.... 怎么说呢....
它...
可能...
就是...
咱也不了解
 */
class BremelanotideItem : BaseSyringeItem(Settings()){
    companion object{
        const val CHEMICAL_FORMULA = "C50H68N14O10"
    }

    override fun applyEffect(target: LivingEntity) {
        target.addEffect(StatusEffects.NAUSEA, 600, 1)
        target.addEffect(JREffects.ESTRUS_EFFECT, 5000, 1)
    }
}
