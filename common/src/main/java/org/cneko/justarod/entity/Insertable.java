package org.cneko.justarod.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/*
插~ 进~ 嗯啊~ 去~~
 */
public interface Insertable {
    default boolean hasRodInside(){
        return !getRodInside().isEmpty();
    }

    default ItemStack getRodInside(){
        return ItemStack.EMPTY;
    }
    default void setRodInside(@NotNull ItemStack rodInside){
        throw new RuntimeException("要在子类实现哦");
    }

    default void tickInside(LivingEntity entity){
        if (hasRodInside()){
            var stack = this.getRodInside();
            var item = stack.getItem();
            // 26.x：inventoryTick 签名改为 (ItemStack, ServerLevel, Entity, EquipmentSlot?)
            if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                item.inventoryTick(stack, sl, entity, null);
            }
        }
    }
}
