package org.cneko.justarod.quirks

// 原来你也喜欢被查吗
class EndRodQuirk : JRDefaultQuirk("end_rod"){
    override fun getInteractionValue(): Int {
        return 5
    }
}