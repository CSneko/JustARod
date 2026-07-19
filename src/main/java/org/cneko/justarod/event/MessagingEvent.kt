package org.cneko.justarod.event

import net.minecraft.world.entity.player.Player
import net.minecraft.core.registries.BuiltInRegistries
import org.cneko.justarod.api.ImpactModel
import org.cneko.justarod.effect.JREffects.Companion.ESTRUS_EFFECT
import org.cneko.justarod.effect.JREffects.Companion.ORGASM_EFFECT
import org.cneko.justarod.effect.JREffects.Companion.STRONG_EFFECT
import org.cneko.toneko.common.mod.api.events.ChatEvents

class MessagingEvent {
    companion object {
        fun init() {
            ChatEvents.CREATE_CHAT_PREFIXES.register{player,prefixes->
                if (player != null) {
                    player as Player
                    if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.getOrThrow(ORGASM_EFFECT))) {
                        prefixes.add("§4高潮")
                    }
                    if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.getOrThrow(ESTRUS_EFFECT))) {
                        prefixes.add("§6发情")
                    }
                    if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.getOrThrow(STRONG_EFFECT))){
                        prefixes.add("§b强壮")
                    }
                    if (ImpactModel.isEnable(player)){
                        prefixes.add("§4淫趴")
                    }
                    if (player.nekoLevel >= 100){
                        // 一看这等级就不正经
                            prefixes.add("§4淫乱猫猫")
                    }

                }
            }
        }
    }
}