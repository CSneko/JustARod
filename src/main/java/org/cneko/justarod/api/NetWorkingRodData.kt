package org.cneko.justarod.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.MinecraftClient
import net.minecraft.component.Component
import net.minecraft.text.Text
import org.cneko.ctlib.common.network.HttpGet.HttpGetObject
import java.util.HashMap

class NetWorkingRodData {
    companion object{
        const val URL = "https://api.justarod.cneko.org/v0/get/" // 看我偷偷改速度把你草四~~
        var SPEED = 1
        var MAX_DAMAGE = 1000

        fun init() {
            update()
        }
         fun update() {
             val scope = CoroutineScope(Dispatchers.IO)
             scope.launch {
                 val req = HttpGetObject(URL)
                 req.let {
                     it.get()
                     val json = it.json
                     MAX_DAMAGE = json.getInt("max_damage")
                     SPEED = json.getInt("speed")
                 }
                 if (MAX_DAMAGE == 0){
                     MAX_DAMAGE = 1000
                 }
                 if (SPEED == 0){
                     SPEED = 1
                 }
             }
        }
    }
}