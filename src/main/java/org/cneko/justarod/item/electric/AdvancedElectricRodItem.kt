package org.cneko.justarod.item.electric

import net.minecraft.item.ItemStack
import org.cneko.justarod.item.JRComponents

class AdvancedElectricRodItem:MultiModeSelfUsedElectricRodItem(Settings().component(JRComponents.USED_TIME_MARK,0).maxDamage(100000).component(JRComponents.SPEED,20).component(JRComponents.MODE,"zako")) {
    override fun getModes(stack: ItemStack): List<String> {
        return listOf("zako","normal","quick")
    }

    override fun getDefaultMode(stack: ItemStack): String {
        return "zako"
    }

    override fun getSpeed(stack: ItemStack?): Int {
        return when (stack?.let { getMode(it) }){
            "zako" -> 5
            "normal" -> 15
            "quick" -> 25
            else -> 10
        }
    }
}