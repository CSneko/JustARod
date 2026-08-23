package org.cneko.justarod.effect

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.player.Player
import net.minecraft.core.Holder
import org.cneko.justarod.JRAttributes

/*
在玩耍的过程中呢，润滑还是很必要的，不然会有点疼，还容易受伤
 */
class LubricatingEffect: MobEffect(MobEffectCategory.BENEFICIAL, 3507428) {
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }
    override fun applyEffectTick(entity: LivingEntity, amplifier: Int): Boolean {
        // 为玩家属性添加
        if (entity is Player){
            val player: Player = entity
            val attributes:Multimap<Holder<Attribute>, AttributeModifier> = ArrayListMultimap.create()
            attributes.put(JRAttributes.PLAYER_LUBRICATING, AttributeModifier(JRAttributes.PLAYER_LUBRICATING_ID, (amplifier+1)*2.0   , AttributeModifier.Operation.ADD_VALUE))
            player.attributes.addTransientAttributeModifiers(attributes)
        }
        return super.applyEffectTick(entity, amplifier)
    }
}