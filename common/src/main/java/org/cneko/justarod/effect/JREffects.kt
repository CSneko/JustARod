package org.cneko.justarod.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import org.cneko.justarod.Justarod.MODID

/**
 * 26.x：药水效果必须以 Holder<MobEffect> 形式注册与使用
 * （MobEffectInstance/addEffect/hasEffect 均要求 Holder），因此这里用
 * Registry.registerForHolder，字段类型为 Holder<MobEffect>（非空）。
 */
class JREffects {
    companion object{
        val ORGASM_EFFECT: Holder<MobEffect> = register("orgasm", OrgasmEffect())
        val LUBRICATING_EFFECT: Holder<MobEffect> = register("lubricating", LubricatingEffect())
        val ESTRUS_EFFECT: Holder<MobEffect> = register("estrus", EstrusEffect())
        val STRONG_EFFECT: Holder<MobEffect> = register("strong", StrongEffect())
        val FAINT_EFFECT: Holder<MobEffect> = register("faint", FaintEffect())
        val PREGNANT_EFFECT: Holder<MobEffect> = register("pregnant", PregnantEffect())
        val AIDS_EFFECT: Holder<MobEffect> = register("aids", AIDSEffect())
        val HPV_EFFECT: Holder<MobEffect> = register("hpv", HPVEffect())
        val VAGINITIS_EFFECT: Holder<MobEffect> = register("vaginitis", VaginitisEffect())
        val OVARIAN_CANCER_EFFECT: Holder<MobEffect> = register("ovarian_cancer", OvarianCancerEffect())
        val SYPHILIS_EFFECT: Holder<MobEffect> = register("syphilis", SyphilisEffect())
        val JUMP_NERF_EFFECT: Holder<MobEffect> = register("jump_nerf", JumpNerfEffect())
        val KENJA_TIME_EFFECT: Holder<MobEffect> = register("kenja_time", KenjaTimeEffect())
        val SMEARY_EFFECT: Holder<MobEffect> = register("smeary", SmearyEffect())
        val UTERINE_COLD_EFFECT: Holder<MobEffect> = register("uterine_cold", UterineColdEffect())
        val URETHRITIS_EFFECT: Holder<MobEffect> = register("urethritis", UrethritisEffect())
        val PROSTATITIS_EFFECT: Holder<MobEffect> = register("prostatitis", ProstatitisEffect())
        val LILY_PHEROMONE_EFFECT: Holder<MobEffect> = register("lily_pheromone", LilyPheromoneEffect())
        val PARONYCHIA_EFFECT: Holder<MobEffect> = register("paronychia", ParonychiaEffect())
        val FATIGUE_EFFECT: Holder<MobEffect> = register("fatigue", FatigueEffect())
        fun init(){
        }

        private fun register(id: String, effect: MobEffect): Holder<MobEffect> =
            Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(MODID, id),
                effect
            )
    }

}
