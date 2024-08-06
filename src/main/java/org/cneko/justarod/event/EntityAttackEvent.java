package org.cneko.justarod.event;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.cneko.justarod.item.OtherUsedItemInterface;

public class EntityAttackEvent {
    public static void init(){
        AttackEntityCallback.EVENT.register(EntityAttackEvent::onAttack);
    }

    public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(entity instanceof LivingEntity e)) return ActionResult.PASS;
        if (stack.getItem() instanceof OtherUsedItemInterface rod){
            rod.useOnOther(stack,world,player,e);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
