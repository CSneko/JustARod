package org.cneko.justarod.effect

import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID

class JREffects {
    companion object{
        var ORGASM_EFFECT: StatusEffect? = OrgasmEffect()
        var LUBRICATING_EFFECT: StatusEffect? = LubricatingEffect()
        var ESTRUS_EFFECT: StatusEffect? = EstrusEffect()
        fun init(){
            ORGASM_EFFECT = Registry.register(
                Registries.STATUS_EFFECT,
                Identifier.of(MODID, "orgasm"),
                ORGASM_EFFECT
            )
            LUBRICATING_EFFECT = Registry.register(
                Registries.STATUS_EFFECT,
                Identifier.of(MODID, "lubricating"),
                LUBRICATING_EFFECT
            )
            ESTRUS_EFFECT = Registry.register(
                Registries.STATUS_EFFECT,
                Identifier.of(MODID, "estrus"),
                ESTRUS_EFFECT
            )
        }
    }

}