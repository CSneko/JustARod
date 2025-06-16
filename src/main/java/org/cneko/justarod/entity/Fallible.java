package org.cneko.justarod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public interface Fallible {
    default Entity justARod$getFallenBy(){
        return null;
    }
    default void justARod$setFallenBy(Entity entity){}
}
