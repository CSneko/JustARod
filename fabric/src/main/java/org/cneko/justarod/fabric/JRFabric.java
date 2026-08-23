package org.cneko.justarod.fabric;

import net.fabricmc.api.ModInitializer;
import org.cneko.justarod.Justarod;

public class JRFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Justarod.init();
    }
}
