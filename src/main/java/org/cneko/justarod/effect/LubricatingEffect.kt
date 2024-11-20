package org.cneko.justarod.effect

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.entry.RegistryEntry
import org.cneko.justarod.JRAttributes

/*
在玩耍的过程中呢，润滑还是很必要的，不然会有点疼，还容易受伤
 */
class LubricatingEffect: StatusEffect(StatusEffectCategory.BENEFICIAL, 3507428) {
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }
    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int): Boolean {
        // 为玩家属性添加
        if (entity is PlayerEntity){
            val player: PlayerEntity = entity
            val attributes:Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> = ArrayListMultimap.create()
            attributes.put(JRAttributes.PLAYER_LUBRICATING, EntityAttributeModifier(JRAttributes.PLAYER_LUBRICATING_ID, (amplifier+1)*2.0   , EntityAttributeModifier.Operation.ADD_VALUE))
            player.attributes.addTemporaryModifiers(attributes)
        }
        return super.applyUpdateEffect(entity, amplifier)
    }
}