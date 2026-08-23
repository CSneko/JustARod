package org.cneko.justarod;

import org.cneko.justarod.api.NetWorkingRodData;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.command.JRCommands;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.genetics.RodGenetics;
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


/**
 * 平台无关的主入口，由各加载器子项目调用。
 */
public class Justarod {
    public static final String MODID = "justarod";

    public static void init() {
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
        LactationInteractionEvents.Companion.init();
        RodGenetics.INSTANCE.init();
    }

}
