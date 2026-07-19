package org.cneko.justarod.damage

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component

/*
事实上想要被爽死其实没那么容易的，毕竟这个快感是让大脑兴奋，很难造成实际的伤害（也不排除过于兴奋的可能性）
 */

/*
 emmm 有心血管疾病的话更容易死的
 虽然你没有...对吧...?
 */
class SexualExcitement(type: Holder<DamageType>?, attacker: Entity?) :
    DamageSource(type, attacker) {
    override fun getLocalizedDeathMessage(killed: LivingEntity?): Component {
        return Component.translatable("death.attack.sexual_excitement", killed!!.displayName)
    }
}