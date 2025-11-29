package org.cneko.justarod.client.event

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.cneko.justarod.client.JRKeyBindings
import org.cneko.justarod.client.screen.JRSyncScreen

class RealClientTickEvent {
    companion object{
        fun init(){
            ClientTickEvents.START_CLIENT_TICK.register { client ->
                while (JRKeyBindings.EXCREMENT_KEY.wasPressed()){
                    client.player!!.networkHandler.sendCommand("excretion release")
                }
                while (JRKeyBindings.URINATE_KEY.wasPressed()){
                    client.player!!.networkHandler.sendCommand("urination release")
                }
                while (JRKeyBindings.STATUS_KEY.wasPressed()){
                    client.setScreen(JRSyncScreen(client.player))
                }
            }
        }
    }
}