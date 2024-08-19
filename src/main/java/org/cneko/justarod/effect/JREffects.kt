package org.cneko.justarod.effect

import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID

class JREffects {
    companion object{
        var ORGASM_EFFECT: StatusEffect? = null
        var LUBRICATING_EFFECT: StatusEffect? = null
        var ESTRUS_EFFECT: StatusEffect? = null
        fun init(){
            ORGASM_EFFECT = Registry.register(
                Registries.STATUS_EFFECT,
                Identifier.of(MODID, "orgasm"),
                OrgasmEffect()
            )
            LUBRICATING_EFFECT = Registry.register(
                Registries.STATUS_EFFECT,
                Identifier.of(MODID, "lubricating"),
                LubricatingEffect()
            )
            ESTRUS_EFFECT = Registry.register(
                Registries.STATUS_EFFECT,
                Identifier.of(MODID, "estrus"),
                EstrusEffect()
            )
        }
    }

}