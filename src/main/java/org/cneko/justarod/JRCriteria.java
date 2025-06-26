package org.cneko.justarod;

import net.minecraft.advancement.criterion.Criteria;
import org.cneko.justarod.advancment.criterion.ItemUsedOnEntityCriterion;

import static org.cneko.justarod.Justarod.MODID;

public class JRCriteria {
    public static final ItemUsedOnEntityCriterion ITEM_USED_ON_ENTITY_CRITERION = Criteria.register(MODID + ":item_used_on_entity", new ItemUsedOnEntityCriterion());

    public static void init(){
    }
}
