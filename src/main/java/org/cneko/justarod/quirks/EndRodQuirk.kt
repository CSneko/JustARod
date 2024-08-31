package org.cneko.justarod.quirks

import org.cneko.toneko.common.mod.quirks.QuirkContext

class EndRodQuirk : JRDefaultQuirk("end_rod"){
    override fun getInteractionValue(p0: QuirkContext?): Int {
        return 5
    }
}