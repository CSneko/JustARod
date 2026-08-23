package org.cneko.justarod.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import org.cneko.justarod.Justarod.MODID

class JREffects {
    companion object{
        var ORGASM_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "orgasm"),
            OrgasmEffect()
        )
        var LUBRICATING_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "lubricating"),
            LubricatingEffect()
        )
        var ESTRUS_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "estrus"),
            EstrusEffect()
        )
        var STRONG_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "strong"),
            StrongEffect()
        )
        var FAINT_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "faint"),
            FaintEffect()
        )
        var PREGNANT_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "pregnant"),
            PregnantEffect()
        )
        var AIDS_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "aids"),
            AIDSEffect()
        )
        val HPV_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "hpv"),
            HPVEffect()
        )
        val VAGINITIS_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "vaginitis"),
            VaginitisEffect()
        )
        val OVARIAN_CANCER_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "ovarian_cancer"),
            OvarianCancerEffect()
        )
        val SYPHILIS_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "syphilis"),
            SyphilisEffect()
        )
        val JUMP_NERF_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "jump_nerf"),
            JumpNerfEffect()
        )
        val KENJA_TIME_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "kenja_time"),
            KenjaTimeEffect()
        )
        val SMEARY_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "smeary"),
            SmearyEffect()
        )
        val UTERINE_COLD_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "uterine_cold"),
            UterineColdEffect()
        )
        val URETHRITIS_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "urethritis"),
            UrethritisEffect()
        )
        val PROSTATITIS_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "prostatitis"),
            ProstatitisEffect()
        )
        val LILY_PHEROMONE_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "lily_pheromone"),
            LilyPheromoneEffect()
        )
        val PARONYCHIA_EFFECT: MobEffect? = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(MODID, "paronychia"),
            ParonychiaEffect()
        )
        fun init(){
        }
    }

}