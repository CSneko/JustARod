package org.cneko.justarod.client.tooltip

import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.util.Identifier

object StructureTextureCache {
    // 用于缓存图片原始尺寸的 Map
    private val dimensions = mutableMapOf<Identifier, Pair<Int, Int>>()

    fun getOriginalSize(id: Identifier): Pair<Int, Int> {
        return dimensions.getOrPut(id) {
            try {
                // 获取客户端的资源管理器
                val resourceManager = MinecraftClient.getInstance().resourceManager
                val resource = resourceManager.getResource(id)

                if (resource.isPresent) {
                    // 读取图片文件
                    resource.get().inputStream.use { stream ->
                        val image = NativeImage.read(stream)
                        val w = image.width
                        val h = image.height
                        // 【非常重要】NativeImage 使用的是直接内存，读取完必须 close()，否则会导致严重的内存泄漏！
                        image.close()
                        return@getOrPut Pair(w, h)
                    }
                } else {
                    Pair(64, 64) // 找不到文件时的默认回退大小
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(64, 64) // 报错时的默认回退大小
            }
        }
    }
}