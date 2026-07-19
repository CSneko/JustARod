package org.cneko.justarod;

import org.cneko.justarod.advancment.criterion.ItemUsedOnEntityCriterion;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.advancements.CriteriaTriggers;

public class JRCriteria {
    public static final ItemUsedOnEntityCriterion ITEM_USED_ON_ENTITY_CRITERION = CriteriaTriggers.register(MODID + ":item_used_on_entity", new ItemUsedOnEntityCriterion());

    public static void init(){
    }
}
