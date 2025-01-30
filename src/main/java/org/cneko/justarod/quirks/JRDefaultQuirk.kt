package org.cneko.justarod.quirks

import net.minecraft.text.Text
import org.cneko.toneko.common.mod.quirks.ToNekoQuirk
import org.cneko.toneko.common.quirks.QuirkRegister

open class JRDefaultQuirk(id:String): ToNekoQuirk(id) {

    override fun getInteractionValue(): Int {
        return 1
    }

    override fun getTooltip(): Text? {
        return Text.translatable("quirk.toneko.${id}.des")
    }

    companion object{
        /**
         * Not a good code, Don't use it
         */
        fun of(id:String){
            QuirkRegister.register(JRDefaultQuirk(id))
        }
    }
}