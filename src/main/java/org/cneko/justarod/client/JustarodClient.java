package org.cneko.justarod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.cneko.justarod.client.renderer.SeeeeexNekoRenderer;
import org.cneko.justarod.entity.JREntities;

public class JustarodClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(JREntities.SEEEEEX_NEKO, SeeeeexNekoRenderer::new);
    }
}
