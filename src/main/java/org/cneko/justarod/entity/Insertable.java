package org.cneko.justarod.entity;

public interface Insertable {
    default boolean hasRodInside(){
        return false;
    }
}
