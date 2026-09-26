package org.cneko.justarod.event

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.cneko.justarod.item.JRGuideBook

/*
进服送手册，人手一本喵~
（参考 toNeko 的做法：背包里没有就补发，装了帕秋莉才发）
 */
class GuideBookEvent {
    companion object {
        fun init() {
            ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
                val player = handler.player

                // 已经拥有手册的玩家不再发放
                for (i in 0 until player.inventory.containerSize) {
                    if (JRGuideBook.isOurGuideBook(player.inventory.getItem(i))) return@register
                }

                val guideBook = JRGuideBook.createGuideBookStack()
                if (!guideBook.isEmpty) {
                    player.inventory.add(guideBook)
                }
            }
        }
    }
}
