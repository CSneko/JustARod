package org.cneko.justarod.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;
import org.cneko.justarod.entity.Insertable;

// 啊我死了
public class EntityDeathEvent {

    public static void init(){
        ServerLivingEntityEvents.AFTER_DEATH.register(DROP_ROD_AFTER_DEATH);
    }
    private static final ServerLivingEntityEvents.AfterDeath DROP_ROD_AFTER_DEATH = (entity,dmgSource) -> {
        if (entity instanceof Insertable insertable //有点分不清mc要的Java版本,我用java16不过分吧
                && insertable.hasRodInside()
        ){
            var rodInside = insertable.getRodInside();
            var bindingCurseFlag = false;
            for (Holder<Enchantment> entry:EnchantmentHelper.getEnchantmentsForCrafting(rodInside).keySet()){
                if (entry.is(Enchantments.BINDING_CURSE)){
                    bindingCurseFlag = true;
                    break;
                }
            }

            if (!((net.minecraft.server.level.ServerLevel) entity.level()).getGameRules().get(GameRules.KEEP_INVENTORY)
                    && !bindingCurseFlag
                    && !rodInside.isEmpty()
            ){//这个是死亡掉落,看情况决定要不要
                org.cneko.justarod.JRUtilKt.spawnItemAtLocation(entity, rodInside);
                insertable.setRodInside(ItemStack.EMPTY);//我试过null,但是在tick那边会npe,看了看发现有empty.我提议另一个也得改
            }
        }
    };


}
