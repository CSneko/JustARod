package org.cneko.justarod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.cneko.justarod.client.event.ClientTickEvent;
import org.cneko.justarod.client.event.JRClientNetworkingEvents;
import org.cneko.justarod.client.renderer.IcedTeaRenderer;
import org.cneko.justarod.client.renderer.LoliNekoRenderer;
import org.cneko.justarod.client.renderer.RodRenderer;
import org.cneko.justarod.client.screen.JRScreenBuilders;
import org.cneko.justarod.entity.JREntities;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder;
import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry;
import org.cneko.toneko.common.mod.client.screens.factories.ScreenBuilders;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.common.mod.util.TickTaskQueue;

public class JustarodClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(JREntities.SEEEEEX_NEKO, NekoRenderer::new);
        EntityRendererRegistry.register(JREntities.LOLI_NEKO, LoliNekoRenderer::new);
        EntityRendererRegistry.register(JREntities.ROD, RodRenderer::new);
        EntityRendererRegistry.register(JREntities.ICED_TEA_PROJECTILE, IcedTeaRenderer::new);
        JRClientNetworkingEvents.init();
        ClientTickEvent.Companion.init();
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

    }
}
