package org.cneko.justarod.event;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.cneko.justarod.JRCriteria;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.item.JRComponents;
import org.cneko.justarod.item.bio.ClonerDevice;
import org.cneko.justarod.item.rod.GiantRodItem;
import org.cneko.justarod.item.JRItems;


public class EntityAttackEvent {
    public static void init(){
        AttackEntityCallback.EVENT.register(EntityAttackEvent::onAttack);
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
            if (playerEntity instanceof ServerPlayer sp) {
                JRCriteria.ITEM_USED_ON_ENTITY_CRITERION.trigger(sp, playerEntity.getItemInHand(hand), entity);
                ItemStack stack = playerEntity.getItemInHand(hand);
                if (entity instanceof EnderDragonPart ede&& stack.is(JRItems.Companion.getFREE_MATING())){
                    JRItems.Companion.getFREE_MATING().useOnEntity(stack,playerEntity,ede.parentMob,hand);
                }
                if (entity instanceof LivingEntity living){
                    if (stack.getItem() instanceof ClonerDevice cloner) {
                        boolean hasData = stack.has(JRComponents.Companion.getENTITY_TYPE());
                        boolean transferred = stack.getOrDefault(JRComponents.Companion.getCLONER_TRANSFERRED(),false);
                        if (hasData && transferred) {
                            return cloner.useOnEntity(stack, playerEntity,living, hand);
                        }
                    }
                }

            }
            return InteractionResult.PASS;
        });

    }

    public static InteractionResult onAttack(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult entityHitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof GiantRodItem) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equalsIgnoreCase("ender_dragon")){
                // 嗯啊♡~~
                RandomSource random = world.random;
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
                player.getInventory().removeItem(stack);
                if (!world.isClientSide()) {
                    player.sendSystemMessage(Component.translatable("item.justarod.end_rod.insert_success"));
                }
                // 快拔出来吧... 求求了...
                entity.hurt(player.damageSources().playerAttack(player), 5);
                entity.spawnAtLocation(Items.DRAGON_EGG);
                // 啊好像... 呜啊...♡
                return InteractionResult.SUCCESS;
            }else {
                player.sendSystemMessage(Component.translatable("item.justarod.giant_rod.too_big"));
            }
        }

        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equalsIgnoreCase("end_rod") && entity instanceof LivingEntity e){
            MobEffectInstance orgasm = new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolder(JREffects.Companion.getORGASM_EFFECT()), 20, 1);
            e.addEffect(orgasm);
        }
        return InteractionResult.PASS;
    }
}
