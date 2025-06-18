package org.cneko.justarod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/* It has benn a long day~ Without you my friend~ We'll tell you all about it when we see you again~
   And we've come a long way~ From where we began~ Oh I'll tell you all about it when I see you again~ I see you again~
 */
public interface Fallible {
    default Entity justARod$getFallenBy(){
        return null;
    }
    default void justARod$setFallenBy(Entity entity){}
}
