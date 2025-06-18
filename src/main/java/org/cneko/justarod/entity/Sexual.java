package org.cneko.justarod.entity;

import net.minecraft.entity.LivingEntity;

/*
写这个的时候起星雨了
 */
public interface Sexual {
    int getSexualDesire();
    void setSexualDesire(int sexualDesire);
    default void increaseSexualDesire(int increase){
        setSexualDesire(getSexualDesire() + increase);
    }
    default void decreaseSexualDesire(int decrease){
        setSexualDesire(getSexualDesire() - decrease);
    }
    default boolean enableCompleteProcess(){
        return true;
    }
    default boolean enableAgeLimit(){
        return false;
    }
    default void sexualSlowTick(LivingEntity entity){
        // 性欲大于100的时候，1/40的概率减少1
        if (getSexualDesire() > 100 && getSexualDesire() <200&& entity.getRandom().nextInt(40) == 0) {
            setSexualDesire(getSexualDesire() - 1);
        }else if (getSexualDesire() < 50 && entity.getRandom().nextInt(15) == 0) {
            // 性欲小于40的时候，1/15的概率增加1
            setSexualDesire(getSexualDesire() + 1);
        }else if (getSexualDesire() > 200 && entity.getRandom().nextInt(100) == 0) {
            // 性欲大于200的时候，1/10的概率减少1
            setSexualDesire(getSexualDesire() - 1);
        }else {
            // 1/50的概率增加-3~5
            if (entity.getRandom().nextInt(50) == 0) {
                setSexualDesire(getSexualDesire() + entity.getRandom().nextInt(9) - 3);
            }
        }
    }
}
