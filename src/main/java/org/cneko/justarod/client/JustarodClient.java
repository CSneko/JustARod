package org.cneko.justarod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.cneko.justarod.client.event.ClientTickEvent;
import org.cneko.justarod.client.event.JRClientNetworkingEvents;
import org.cneko.justarod.client.renderer.SeeeeexNekoRenderer;
import org.cneko.justarod.entity.JREntities;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;

public class JustarodClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(JREntities.SEEEEEX_NEKO, NekoRenderer::new);
        JRClientNetworkingEvents.init();
        ClientTickEvent.Companion.init();
    }
}
