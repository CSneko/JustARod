package org.cneko.justarod.effect

import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID

class JREffects {
    companion object{
        var ORGASM_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "orgasm"),
            OrgasmEffect()
        )
        var LUBRICATING_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "lubricating"),
            LubricatingEffect()
        )
        var ESTRUS_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "estrus"),
            EstrusEffect()
        )
        var STRONG_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "strong"),
            StrongEffect()
        )
        var FAINT_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "faint"),
            FaintEffect()
        )
        fun init(){
        }
    }

}