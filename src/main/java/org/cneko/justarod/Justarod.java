package org.cneko.justarod;

import net.fabricmc.api.ModInitializer;
import org.cneko.justarod.effects.JREffects;
import org.cneko.justarod.item.JRItems;

public class Justarod implements ModInitializer {
    public static final String MODID = "justarod";
    @Override
    public void onInitialize() {
        JRItems.Companion.init();
        JREffects.Companion.init();
    }
}
