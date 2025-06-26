package org.cneko.justarod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.cneko.justarod.api.NetWorkingRodData;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.command.JRCommands;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.event.*;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.JRPackets;
import org.cneko.justarod.quirks.JRQuirks;

/*
说实话，这个模组其实算不上难做，难的就是呢得把涩涩的感触做进游戏里
可是吧，涩涩的时候大脑都是被快感给占领了，想要记住细节其实很难的
没有这些细节的话呢，做出来的感觉很奇怪
还有就是，得要阅本量丰富才能做的炉火纯青
 */

/* TODO : 无顺序
 1. 软体末地烛
 2. 高级电动末地烛
 3. 工业末地烛
 4. 海绵末地烛
 5. 淫水
 6. 末地烛处刑机
 8. 公屏求草
 10. quirks/SM
 12. 远控末地烛
 13. 全服共享末地烛
 14. 双头末地烛
 */

public class Justarod implements ModInitializer {
    public static final String MODID = "justarod";

    @Override
    public void onInitialize() {
        NetWorkingRodData.Companion.init();
        JRItems.Companion.init();
        JRBlocks.init();
        JREffects.Companion.init();
        JRAttributes.Companion.init();
        JRQuirks.Companion.init();
        EntityAttackEvent.init();
        MessagingEvent.Companion.init();
        JREntities.init();
        TickEvent.Companion.init();
        JRCommands.init();
        JRPackets.init();
        JRNetWorkingEvents.init();
        JRCriteria.init();

        EntityDeathEvent.init();
        EntityRespawnEvent.init();
    }
}
