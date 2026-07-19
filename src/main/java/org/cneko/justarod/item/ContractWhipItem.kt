package org.cneko.justarod.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.network.chat.Component
import org.cneko.toneko.common.mod.entities.INeko

class ContractWhipItem(properties: Properties): WhipItem(properties) {
    override fun hitTarget(attacker: LivingEntity, target: LivingEntity, amount: Float) {
        if (target is INeko){
            if (target.isNeko && !target.hasOwner(attacker.uuid) && target.random.nextInt(3)==0){
                attacker.sendSystemMessage(Component.literal("§a你成为了${target.name.string}的主人喵！"))
                target.addOwner(attacker.uuid, INeko.Owner(ArrayList(),0))
                target.sendSystemMessage(Component.literal("§a${target.name.string}成为了你的主人喵！"))
            }
        }
        super.hitTarget(attacker, target, amount)
    }
}