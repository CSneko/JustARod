package org.cneko.justarod.damage

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text

class SexualExcitement(type: RegistryEntry<DamageType>?, attacker: Entity?) :
    DamageSource(type, attacker) {
    override fun getDeathMessage(killed: LivingEntity?): Text {
        return Text.translatable("death.attack.sexual_excitement", killed!!.displayName)
    }
}