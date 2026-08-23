package org.cneko.justarod.fabric;

import net.fabricmc.api.ClientModInitializer;
import org.cneko.justarod.client.JustarodClient;

public class JRFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JustarodClient.init();
    }
}
