package org.cneko.justarod.item.electric

import net.minecraft.item.ItemStack
import org.cneko.justarod.item.JRComponents

/*
电动的确实好用的说，比普通的棒子要舒服多了
 */
class AdvancedElectricRodItem:MultiModeSelfUsedElectricRodItem(Settings().component(JRComponents.USED_TIME_MARK,0).maxDamage(100000).component(JRComponents.SPEED,20).component(JRComponents.MODE,"zako")) {
    override fun getModes(stack: ItemStack): List<String> {
        return listOf("zako","normal","quick")
    }

    override fun getDefaultMode(stack: ItemStack): String {
        return "zako"
    }

    override fun getRodSpeed(stack: ItemStack?): Int {
        return when (stack?.let { getMode(it) }){
            "zako" -> 5
            "normal" -> 15
            "quick" -> 25
            else -> 10
        }
    }

    override fun getEnergyMaxInput(stack: ItemStack?): Long {
        return 10000
    }
}