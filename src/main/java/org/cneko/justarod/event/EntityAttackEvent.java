package org.cneko.justarod.event;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.cneko.justarod.JRCriteria;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.item.GiantRodItem;


public class EntityAttackEvent {
    public static void init(){
        AttackEntityCallback.EVENT.register(EntityAttackEvent::onAttack);
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
            if (playerEntity instanceof ServerPlayerEntity sp) {
                JRCriteria.ITEM_USED_ON_ENTITY_CRITERION.trigger(sp, playerEntity.getStackInHand(hand), entity);
            }
            return ActionResult.PASS;
        });
    }

    public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof GiantRodItem) {
            if (Registries.ENTITY_TYPE.getId(entity.getType()).getPath().equalsIgnoreCase("ender_dragon")){
                // 嗯啊♡~~
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
                // 这么大... 真的会受不了的...
                player.getInventory().removeOne(stack);
                if (!world.isClient()) {
                    player.sendMessage(Text.translatable("item.justarod.end_rod.insert_success"));
                }
                // 快拔出来吧... 求求了...
                entity.damage(player.getDamageSources().playerAttack(player), 5);
                entity.dropItem(Items.DRAGON_EGG);
                // 啊好像... 呜啊...♡
                return ActionResult.SUCCESS;
            }else {
                player.sendMessage(Text.translatable("item.justarod.giant_rod.too_big"));
            }
        }

        if (Registries.ITEM.getId(stack.getItem()).getPath().equalsIgnoreCase("end_rod") && entity instanceof LivingEntity e){
            StatusEffectInstance orgasm = new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getORGASM_EFFECT()), 20, 1);
            e.addStatusEffect(orgasm);
        }
        return ActionResult.PASS;
    }
}
