package org.cneko.justarod.api

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class NetWorkingRodData {
    companion object{
        const val URL = "https://api.justarod.cneko.org/v0/get/" // 看我偷偷改速度把你草四~~
        var SPEED = 1
        var MAX_DAMAGE = 1000

        fun init() {
            update()
        }

        fun update() {
            // 避免额外打包 kotlinx-coroutines，用普通线程执行异步 HTTP 请求
            Thread {
                try {
                    val client = HttpClient.newHttpClient()
                    val request = HttpRequest.newBuilder(URI.create(URL))
                        .GET()
                        .build()
                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                    val json = JsonParser.parseString(response.body()).asJsonObject
                    MAX_DAMAGE = json.get("max_damage").asInt
                    SPEED = json.get("speed").asInt
                } catch (e: Exception) {
                    // 请求失败时保持默认值即可，不影响游戏
                }
                if (MAX_DAMAGE == 0){
                    MAX_DAMAGE = 1000
                }
                if (SPEED == 0){
                    SPEED = 1
                }
            }.start()
        }
    }
}
