package org.cneko.justarod.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;
import org.cneko.justarod.entity.Insertable;

public class EntityDeathEvent {

    public static void init(){
        ServerLivingEntityEvents.AFTER_DEATH.register(DROP_ROD_AFTER_DEATH);
    }
    private static final ServerLivingEntityEvents.AfterDeath DROP_ROD_AFTER_DEATH = (entity,dmgSource) -> {
        if (entity instanceof Insertable insertable //有点分不清mc要的Java版本,我用java16不过分吧
                && insertable.hasRodInside()
        ){
            if (!entity.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)){//这个是死亡掉落,看情况决定要不要
                entity.dropStack(insertable.getRodInside());
                insertable.setRodInside(ItemStack.EMPTY);//我试过null,但是在tick那边会npe,看了看发现有empty.我提议另一个也得改
            }
        }
    };

}
