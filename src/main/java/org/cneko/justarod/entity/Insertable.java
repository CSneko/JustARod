package org.cneko.justarod.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.throwables.MixinException;

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
            item.inventoryTick(stack, entity.getWorld(), entity, Integer.MIN_VALUE, false);
        }
    }
}
