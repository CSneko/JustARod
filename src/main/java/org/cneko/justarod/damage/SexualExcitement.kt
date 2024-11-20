package org.cneko.justarod.damage

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text

/*
事实上想要被爽死其实没那么容易的，毕竟这个快感是让大脑兴奋，很难造成实际的伤害（也不排除过于兴奋的可能性）
 */
class SexualExcitement(type: RegistryEntry<DamageType>?, attacker: Entity?) :
    DamageSource(type, attacker) {
    override fun getDeathMessage(killed: LivingEntity?): Text {
        return Text.translatable("death.attack.sexual_excitement", killed!!.displayName)
    }
}