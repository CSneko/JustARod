package org.cneko.justarod.client.event

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.cneko.justarod.client.JRKeyBindings
import org.cneko.justarod.client.screen.JRSyncScreen

class RealClientTickEvent {
    companion object{
        fun init(){
            ClientTickEvents.START_CLIENT_TICK.register { client ->
                while (JRKeyBindings.EXCREMENT_KEY.consumeClick()){
                    client.player!!.connection.sendCommand("jr excretion release")
                }
                while (JRKeyBindings.URINATE_KEY.consumeClick()){
                    client.player!!.connection.sendCommand("jr urination release")
                }
                while (JRKeyBindings.STATUS_KEY.consumeClick()){
                    client.setScreen(JRSyncScreen(client.player))
                }
            }
        }
    }
}