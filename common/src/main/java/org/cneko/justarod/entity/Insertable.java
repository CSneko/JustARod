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
                // 末地烛走专门的插入 tick：背包格子的 inventoryTick 也传 null slot，
                // 用它来区分「插入」会让背包里的末地烛自己动起来。
                if (item instanceof org.cneko.justarod.item.rod.EndRodItem rod) {
                    rod.tickInserted(stack, sl, entity);
                } else {
                    item.inventoryTick(stack, sl, entity, null);
                }
            }
        }
    }
}
