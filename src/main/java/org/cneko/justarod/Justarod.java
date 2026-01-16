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

import java.util.logging.Logger;

/*
说实话，这个模组其实算不上难做，难的就是呢得把涩涩的感触做进游戏里
可是吧，涩涩的时候大脑都是被快感给占领了，想要记住细节其实很难的
没有这些细节的话呢，做出来的感觉很奇怪
还有就是，得要阅本量丰富才能做的炉火纯青
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


    /*
     *                        _oo0oo_
     *                       o8888888o
     *                       88" . "88
     *                       (| -_- |)
     *                       0\  =  /0
     *                     ___/`---'\___
     *                   .' \\|     |// '.
     *                  / \\|||  :  |||// \
     *                 / _||||| -:- |||||- \
     *                |   | \\\  - /// |   |
     *                | \_|  ''\---/''  |_/ |
     *                \  .-\__  '-'  ___/-. /
     *              ___'. .'  /--.--\  `. .'___
     *           ."" '<  `.___\_<|>_/___.' >' "".
     *          | | :  `- \`.;`\ _ /`;.`/ - ` : | |
     *          \  \ `_.   \_ __\ /__ _/   .-` /  /
     *      =====`-.____`.___ \_____/___.-`___.-'=====
     *                        `=---='
     *
     *
     *      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     *            佛祖保佑       永不宕机     永无BUG
     *
     *        佛曰:
     *                写字楼里写字间，写字间里程序员；
     *                程序人员写程序，又拿程序换酒钱。
     *                酒醒只在网上坐，酒醉还来网下眠；
     *                酒醉酒醒日复日，网上网下年复年。
     *                但愿老死电脑间，不愿鞠躬老板前；
     *                奔驰宝马贵者趣，公交自行程序员。
     *                别人笑我忒疯癫，我笑自己命太贱；
     *                不见满街漂亮妹，哪个归得程序员？
     */
}
