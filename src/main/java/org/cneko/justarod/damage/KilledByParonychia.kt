package org.cneko.justarod.damage

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text

class KilledByParonychia(type: RegistryEntry<DamageType>?, entity: Entity?) :
    DamageSource(type, entity) {
    override fun getDeathMessage(killed: LivingEntity?): Text {
        return Text.translatable("death.attack.paronychia", killed!!.displayName)
    }
}