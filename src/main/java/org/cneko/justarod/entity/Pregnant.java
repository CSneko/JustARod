package org.cneko.justarod.entity;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.cneko.justarod.effect.JREffects;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;

public interface Pregnant{

    /*
    插的太深了...
     */

    default void setPregnant(int time) {
    }

    default int getPregnant() {
        return 0;
    }

    default boolean isPregnant() {
        return getPregnant() > 0;
    }

    default void setChildrenType(EntityType<?> type){}

    default EntityType<?> getChildrenType() {
        throw new RuntimeException("getChildrenType() is not implemented in " + this.getClass().getName());
    }

    default void updatePregnant() {
        if (getPregnant() > 0) {
            setPregnant(getPregnant() - 1);
            if (getPregnant() == 0) {
                if (this instanceof LivingEntity liv){
                    liv.sendMessage(Text.of("§a分娩完成！"));
                    liv.damage(liv.getDamageSources().generic(),6.0f);
                }
                makeBaby();
            }
        }
    }

    default void miscarry() {
        // 流产
        if (this instanceof LivingEntity pregnantEntity) {
            pregnantEntity.setHealth(pregnantEntity.getHealth()-10);
            pregnantEntity.sendMessage(Text.of("§c你流产了！"));
        }
        setPregnant(0);
    }

    default void makeBaby() {
        Entity baby = createBaby();
        if (baby != null) {
            // 产仔
            baby.getWorld().spawnEntity(baby);
            // 受伤
            if (this instanceof LivingEntity pregnantEntity) {
                pregnantEntity.damage(pregnantEntity.getDamageSources().generic(), 2.0F);
            }
        }
    }

    default int getMenstruation(){
        return 0;
    }
    default void setMenstruation(int time){}
    default MenstruationCycle getMenstruationCycle(){
        int menstruation = getMenstruation();
        if (menstruation <=0){
            return MenstruationCycle.NONE;
        }else if(menstruation <= 20*60*20*2){
            return MenstruationCycle.MENSTRUATION;
        } else if (menstruation <= 20*60*20*7) {
            return MenstruationCycle.FOLLICLE;
        } else if (menstruation <= 20*60*20*8) {
            return MenstruationCycle.OVULATION;
        } else if (menstruation <=20*60*20*11) {
            return MenstruationCycle.LUTEINIZATION;
        }
        return MenstruationCycle.NONE;
    }

    default void setMenstruationComfort(int time){
    }
    default int getMenstruationComfort(){
        return 0;
    }

    default void updateMenstruation() {
        if (!isPregnant() && getMenstruation() <= 20*60*20*11) {
            setMenstruation(getMenstruation() + 1);
        }else {
            setMenstruation(0);
        }
        if (getMenstruationComfort() > 0){
            setMenstruationComfort(getMenstruationComfort()-1);
        }
    }
    default boolean canPregnant(){
        return getMenstruationCycle() == MenstruationCycle.OVULATION && !this.isPregnant() && !this.isSterilization();
    }

    default void writePregnantToNbt(NbtCompound nbt) {
        nbt.putInt("Pregnant", getPregnant());
        nbt.putString("ChildrenType", EntityType.getId(getChildrenType()).toString());
        nbt.putInt("Menstruation", getMenstruation());
        nbt.putInt("MenstruationComfort", getMenstruationComfort());
        nbt.putBoolean("Sterilization", isSterilization());
    }
    default void readPregnantFromNbt(NbtCompound nbt) {
        if (nbt.contains("Pregnant")) {
            setPregnant(nbt.getInt("Pregnant"));
        }
        if (nbt.contains("ChildrenType")) {
            Identifier id = Identifier.tryParse(nbt.getString("ChildrenType"));
            if (id != null) {
                EntityType<?> childrenType = Registries.ENTITY_TYPE.get(id);
                setChildrenType(childrenType);
            }
        }
        if (nbt.contains("Menstruation")) {
            setMenstruation(nbt.getInt("Menstruation"));
        }
        if (nbt.contains("MenstruationComfort")) {
            setMenstruationComfort(nbt.getInt("MenstruationComfort"));
        }
        if (nbt.contains("Sterilization")) {
            setSterilization(nbt.getBoolean("Sterilization"));
        }
    }

    default Entity createBaby() {
        throw new RuntimeException("createBaby() is not implemented in " + this.getClass().getName());
    }

    default void setSterilization(boolean sterilization){}
    // 是否绝育
    default boolean isSterilization(){
        return false;
    }

    static <T extends LivingEntity&Pregnant> void pregnantTick(T pregnant) {
        pregnant.updatePregnant();
        if (!pregnant.isPregnant()) {
            // 清除怀孕效果（如果有的话）
            pregnant.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPREGNANT_EFFECT()));
        }else {
            // 设置怀孕效果
            if (!pregnant.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPREGNANT_EFFECT()))) {
                pregnant.addStatusEffect(new StatusEffectInstance(
                        Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPREGNANT_EFFECT()),
                        pregnant.getPregnant(),
                        0,
                        false,
                        false,
                        true
                ));
            }
        }
    }
    static <T extends LivingEntity&Pregnant> void menstruationTick(T pregnant) {
        pregnant.updateMenstruation();
        MenstruationCycle cycle = pregnant.getMenstruationCycle();
        // 1/3000的几率获得效果
        if (pregnant.getRandom().nextInt(3000) == 0) {
            if (cycle == MenstruationCycle.MENSTRUATION){
                // 缓慢效果（60s）
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20*60, 0));
                // 虚弱效果（30s）
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20*30, 0));
            } else if (cycle == MenstruationCycle.FOLLICLE) {
                // 速度效果（60s）
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20*60, 0));
                // 跳跃提升（30s）
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20*30, 0));
            } else if (cycle == MenstruationCycle.OVULATION) {
                // 生命恢复(60s)
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20*60, 0));
                // 兴奋效果(30s)
                if (pregnant instanceof INeko neko && neko.isNeko()){
                    neko.getEntity().addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(ToNekoEffects.NEKO_EFFECT), 20*30, 0));
                }
            } else if (cycle == MenstruationCycle.LUTEINIZATION) {
                // 饥饿效果
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20*60, 0));
                // 挖掘疲劳
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20*30, 0));
            }
        }
        // 1/400的几率附加
        if (pregnant.getRandom().nextInt(400) == 0) {
            if (cycle == MenstruationCycle.MENSTRUATION){
                if (pregnant.getMenstruationComfort()>0){
                    if (pregnant.getRandom().nextBoolean()){
                        // 扣血
                        pregnant.damage(pregnant.getDamageSources().magic(), 1.0f);
                    }
                }else {
                    // 扣血
                    pregnant.damage(pregnant.getDamageSources().magic(), 1.0f);
                }
            }
        }
    }

    @Getter
    enum MenstruationCycle{
        NONE(0,"无"), //  无
        MENSTRUATION(20*60*20*2,"月经期"), // 月经期
        FOLLICLE(20*60*20*5,"卵泡期"), // 卵泡期
        OVULATION(20*60*2,"排卵期"), // 排卵期
        LUTEINIZATION(20*60*20*3,"黄体期") // 黄体期
        ;

        private final int duration;
        public final String text;
        MenstruationCycle(int duration,String name) {
            this.duration = duration;
            this.text = name;
        }

    }


}
