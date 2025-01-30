package org.cneko.justarod.quirks

import org.cneko.toneko.common.quirks.QuirkRegister

class JRQuirks {
    companion object{
        fun init(){
            QuirkRegister.register(EndRodQuirk())
            QuirkRegister.register(CCBQuirk())
        }
    }
}