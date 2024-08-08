package org.cneko.justarod.event;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.cneko.justarod.item.GiantRodItem;


public class EntityAttackEvent {
    public static void init(){
        AttackEntityCallback.EVENT.register(EntityAttackEvent::onAttack);
    }

    public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof GiantRodItem rod) {
            if (Registries.ENTITY_TYPE.getId(entity.getType()).getPath().equalsIgnoreCase("ender_dragon")){
                Random random = world.random;
                world.addParticle(
                        ParticleTypes.HEART,
                        entity.getX() + random.nextInt(2) - 1,
                        entity.getY() + random.nextInt(2) + 2,
                        entity.getZ() + random.nextInt(2) - 1,
                        0.0,
                        2.0,
                        0.0
                );
                player.getInventory().removeOne(stack);
                if (!world.isClient()) {
                    player.sendMessage(Text.translatable("item.justarod.end_rod.insert_success"));
                }
                entity.damage(player.getDamageSources().playerAttack(player), 5);
                entity.dropItem(Items.DRAGON_EGG);
                return ActionResult.SUCCESS;
            }else {
                player.sendMessage(Text.translatable("item.justarod.giant_rod.too_big"));
            }
        }
        return ActionResult.PASS;
    }
}
