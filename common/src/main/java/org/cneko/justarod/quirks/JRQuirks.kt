package org.cneko.justarod.quirks

import org.cneko.toneko.common.mod.quirks.QuirkRegister

/*
人类的xp不可以那么奇怪的喵呜~
 */
class JRQuirks {
    companion object{
        val YURI_QUIRK = YuriQuirk()
        fun init(){
            QuirkRegister.register(EndRodQuirk())
            QuirkRegister.register(CCBQuirk())
            QuirkRegister.register(YURI_QUIRK)
        }
    }
}