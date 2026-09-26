package org.cneko.justarod.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.client.event.ClientTickEvent;
import org.cneko.justarod.client.event.JRClientNetworkingEvents;
import org.cneko.justarod.client.event.JRHudRenderEvent;
import org.cneko.justarod.client.event.RealClientTickEvent;
import org.cneko.justarod.client.renderer.IcedTeaRenderer;
import org.cneko.justarod.client.renderer.LoliNekoRenderer;
import org.cneko.justarod.client.renderer.RodRenderer;
import org.cneko.justarod.client.screen.JRScreenBuilders;
import org.cneko.justarod.client.tooltip.ChemicalStructureTooltipComponent;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.common.mod.util.TickTaskQueue;

/**
 * 平台无关的客户端入口，由各加载器子项目在客户端初始化时调用。
 */
public class JustarodClient {

    public static void init() {
        EntityRendererRegistry.register(JREntities.SEEEEEX_NEKO, NekoRenderer::new);
        EntityRendererRegistry.register(JREntities.LOLI_NEKO, LoliNekoRenderer::new);
        EntityRendererRegistry.register(JREntities.ROD, RodRenderer::new);
        EntityRendererRegistry.register(JREntities.ICED_TEA_PROJECTILE, IcedTeaRenderer::new);
        JRClientNetworkingEvents.init();
        // 驯服史莱姆：模型层 / 渲染器 / 体内容器界面
        org.cneko.justarod.client.event.JRSlimeClientEvents.init();
        // 诊断命令 /jrslime（排查容器两端不一致用）
        org.cneko.justarod.client.command.SlimeDebugClientCommand.init();
        JRKeyBindings.init();
        ClientTickEvent.Companion.init();
        RealClientTickEvent.Companion.init();
        JRHudRenderEvent.init();
        // 骑着史莱姆时屏幕右侧的「按 G 吞噬 / 按 B 打开背包」提示
        org.cneko.justarod.client.event.JRSlimeHud.init();
        NekoScreenRegistry.register(JREntities.SEEEEEX_NEKO_ID, JRScreenBuilders.SEEEEEX_NEKO_INTERACTIVE_SCREEN);
        NekoScreenRegistry.register(JREntities.LOLI_NEKO_ID, JRScreenBuilders.LOLI_NEKO_INTERACTIVE_SCREEN);
        var queen = new TickTaskQueue();
        Runnable task = ()-> {
            try {
                NekoScreenRegistry.get(ToNekoEntities.RAVENN_ID).addButton(JRScreenBuilders.JRButtonFactories.RAVENN_BREED_BUTTON);
            } catch (Exception ignored) {
            }
        };
        queen.addTask(20,task);
        TickTasks.addClient(queen);

        ClientTooltipComponentCallback.EVENT.register(data ->{
            if (data instanceof ChemicalStructureTooltipData) {
                return new ChemicalStructureTooltipComponent((ChemicalStructureTooltipData) data);
            }
            return null;
        });
    }

    public static Player getClientPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }
}
