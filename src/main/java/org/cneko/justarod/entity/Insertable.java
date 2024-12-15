package org.cneko.justarod.entity;

import org.spongepowered.asm.mixin.throwables.MixinException;

public interface Insertable {
    default boolean hasRodInside(){
        return !getRodInside().isEmpty();
    }

    default String getRodInside(){
        return "";
    }
    default void setRodInside(String rodId){
        throw new RuntimeException("要在子类实现哦");
    }
}
