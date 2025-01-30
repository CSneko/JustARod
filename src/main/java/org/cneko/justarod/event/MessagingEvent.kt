package org.cneko.justarod.event

import net.minecraft.registry.Registries
import org.cneko.justarod.api.ImpactModel
import org.cneko.justarod.effect.JREffects.Companion.ESTRUS_EFFECT
import org.cneko.justarod.effect.JREffects.Companion.ORGASM_EFFECT
import org.cneko.justarod.effect.JREffects.Companion.STRONG_EFFECT
import org.cneko.toneko.common.api.NekoQuery
import org.cneko.toneko.common.api.NekoQuery.Neko
import org.cneko.toneko.common.mod.api.events.ChatEvents

class MessagingEvent {
    companion object {
        fun init() {
            ChatEvents.CREATE_CHAT_PREFIXES.register{player,prefixes->
                if (player != null) {
                    if (player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(ORGASM_EFFECT))) {
                        prefixes.add("§4高潮")
                    }
                    if (player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(ESTRUS_EFFECT))) {
                        prefixes.add("§6发情")
                    }
                    if (player.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(STRONG_EFFECT))){
                        prefixes.add("§b强壮")
                    }
                    if (ImpactModel.isEnable(player)){
                        prefixes.add("§4淫趴")
                    }
                    val neko = NekoQuery.getNeko(player.uuid)
                    if (neko != null) {
                        if (neko.level >= 100){
                            prefixes.add("§4淫乱猫猫")
                        }
                    }
                }
            }
        }
    }
}