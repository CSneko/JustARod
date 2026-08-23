package org.cneko.justarod.damage

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component

// 奇怪の名字
class KilledByGrass(type: Holder<DamageType>?,val attacker: Entity?) :
    DamageSource(type, attacker) {
        override fun getLocalizedDeathMessage(killed: LivingEntity?): Component {
            return Component.translatable("death.attack.grass", killed!!.displayName,attacker?.displayName)
        }
}