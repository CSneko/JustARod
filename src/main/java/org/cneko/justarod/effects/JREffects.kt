package org.cneko.justarod.effects

import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID

class JREffects {
    companion object{
        var TATER_EFFECT: StatusEffect? = null
        fun init(){
            TATER_EFFECT = Registry.register(
                Registries.STATUS_EFFECT,
                Identifier.of(MODID, "orgasm"),
                OrgasmEffect()
            )
        }
    }

}