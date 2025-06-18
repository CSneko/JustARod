package org.cneko.justarod.item.electric

import net.minecraft.item.ItemStack
import org.cneko.justarod.item.JRComponents

/*
这这这已经不是人可以承受的了，我我我我至少承受不住
 */
class IndustrialElectricRodItem: MultiModeSelfUsedElectricRodItem(Settings().component(JRComponents.USED_TIME_MARK,0).maxDamage(10000000).component(JRComponents.SPEED,50).component(JRComponents.MODE,"normal")) {
    override fun getModes(stack: ItemStack): List<String> {
        return listOf("normal","quick","fly")
    }

    override fun getDefaultMode(stack: ItemStack): String {
        return "normal"
    }

    override fun getRodSpeed(stack: ItemStack?): Int {
        return when (stack?.let { getMode(it) }){
            "normal" -> 30
            "quick" -> 60
            "fly" -> 100
            else -> 50
        }
    }

    override fun getEnergyMaxInput(stack: ItemStack?): Long {
        return 100000
    }
}