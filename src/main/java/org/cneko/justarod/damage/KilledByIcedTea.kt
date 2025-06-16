package org.cneko.justarod.damage

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text

class KilledByIcedTea(type: RegistryEntry<DamageType>?, attacker: Entity?) :
    DamageSource(type, attacker) {
    override fun getDeathMessage(killed: LivingEntity?): Text {
        return Text.literal(String.format("%s坠机了",killed!!.displayName,attacker?.displayName))
    }
}