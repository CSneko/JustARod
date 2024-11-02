package org.cneko.justarod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.event.EntityAttackEvent;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.payload.SeeeeexNekoInteractivePayload;
import org.cneko.justarod.quirks.JRQuirks;

public class Justarod implements ModInitializer {
    public static final String MODID = "justarod";
    @Override
    public void onInitialize() {
        JRItems.Companion.init();
        JRBlocks.init();
        JREffects.Companion.init();
        JRAttributes.Companion.init();
        JRQuirks.Companion.init();
        EntityAttackEvent.init();
        JREntities.init();
        PayloadTypeRegistry.playS2C().register(SeeeeexNekoInteractivePayload.ID, SeeeeexNekoInteractivePayload.CODEC);
    }
}
