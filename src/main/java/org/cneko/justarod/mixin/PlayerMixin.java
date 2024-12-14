package org.cneko.justarod.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.justarod.entity.Insertable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public class PlayerMixin implements Insertable {

    @Override
    public boolean hasRodInside() {
        return false;
    }
}
