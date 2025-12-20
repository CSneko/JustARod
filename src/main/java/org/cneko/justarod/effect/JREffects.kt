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
        var PREGNANT_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "pregnant"),
            PregnantEffect()
        )
        var AIDS_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "aids"),
            AIDSEffect()
        )
        val HPV_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "hpv"),
            HPVEffect()
        )
        val VAGINITIS_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "vaginitis"),
            VaginitisEffect()
        )
        val OVARIAN_CANCER_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "ovarian_cancer"),
            OvarianCancerEffect()
        )
        val SYPHILIS_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "syphilis"),
            SyphilisEffect()
        )
        val JUMP_NERF_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "jump_nerf"),
            JumpNerfEffect()
        )
        val KENJA_TIME_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "kenja_time"),
            KenjaTimeEffect()
        )
        val SMEARY_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "smeary"),
            SmearyEffect()
        )
        val UTERINE_COLD_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "uterine_cold"),
            UterineColdEffect()
        )
        val URETHRITIS_EFFECT: StatusEffect? = Registry.register(
            Registries.STATUS_EFFECT,
            Identifier.of(MODID, "urethritis"),
            UrethritisEffect()
        )
        fun init(){
        }
    }

}