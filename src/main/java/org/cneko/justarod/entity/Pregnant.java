package org.cneko.justarod.entity;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.item.JRItems;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface Pregnant{
    List<UUID> FOREVER_BABY = new ArrayList<>();
    /*
    插的太深了...
     */

    default void tryPregnant() {
        this.setPregnant(20*60*20*10);
        // 是否会葡萄胎
        if (((Entity)this).getRandom().nextFloat() < getHydatidiformMoleProbability()) {
            this.setHydatidiformMole(true);
            return;
        }
        // 是否会宫外孕
        if (((Entity)this).getRandom().nextFloat() < getEctopicPregnancyProbability()) {
            this.setEctopicPregnancy(true);
        }
    }

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
            if (getPregnant() == 0 && !isEctopicPregnancy() && !isHydatidiformMole()) {
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
        makeBaby(false);
    }
    default void makeBaby(boolean pretermBirth) {
        for (int i = 0; i < getBabyCount(); i++) {
            Entity baby = createBaby();
            if (baby != null) {
                // 产仔
                baby.getWorld().spawnEntity(baby);
                if (baby instanceof LivingEntity b && pretermBirth){
                    // 永久性的缓慢
                    b.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, Integer.MAX_VALUE, 1));
                    // 1/10几率死亡
                    if (b.getRandom().nextInt(10) == 0) {
                        b.kill();
                    }
                    // 1/4的概率中毒
                    if (b.getRandom().nextInt(4) == 0) {
                        b.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 20*60, 0));
                    }
                }
                // 受伤
                if (this instanceof LivingEntity pregnantEntity) {
                    if (baby instanceof EnderDragonEntity){
                        // 玩家爆炸
                        pregnantEntity.getWorld().createExplosion(baby, pregnantEntity.getX(), pregnantEntity.getY(), pregnantEntity.getZ(), 10.0F, World.ExplosionSourceType.MOB);
                        FOREVER_BABY.add(baby.getUuid());
                    }else {
                        pregnantEntity.damage(pregnantEntity.getDamageSources().generic(), 2.0F);
                    }
                }
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
        return getMenstruationCycle() == MenstruationCycle.OVULATION && !this.isPregnant() && !this.isSterilization() && !this.isHysterectomy();
    }

    default void writePregnantToNbt(NbtCompound nbt) {
        nbt.putInt("Pregnant", getPregnant());
        nbt.putString("ChildrenType", EntityType.getId(getChildrenType()).toString());
        nbt.putInt("Menstruation", getMenstruation());
        nbt.putInt("MenstruationComfort", getMenstruationComfort());
        nbt.putBoolean("Sterilization", isSterilization());
        nbt.putBoolean("EctopicPregnancy", isEctopicPregnancy());
        nbt.putInt("AIDS", getAids());
        nbt.putBoolean("HydatidiformMole", isHydatidiformMole());
        nbt.putInt("BabyCount", getBabyCount());
        nbt.putInt("HPV", getHPV());
        nbt.putBoolean("Immune2HPV", isImmune2HPV());
        nbt.putBoolean("Hysterectomy",isHysterectomy());
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
        if (nbt.contains("EctopicPregnancy")) {
            setEctopicPregnancy(nbt.getBoolean("EctopicPregnancy"));
        }
        if (nbt.contains("AIDS")) {
            setAids(nbt.getInt("AIDS"));
        }
        if (nbt.contains("HydatidiformMole")) {
            setHydatidiformMole(nbt.getBoolean("HydatidiformMole"));
        }
        if (nbt.contains("BabyCount")) {
            setBabyCount(nbt.getInt("BabyCount"));
        }
        if (nbt.contains("HPV")) {
            setHPV(nbt.getInt("HPV"));
        }
        if (nbt.contains("Immune2HPV")){
            setImmune2HPV(nbt.getBoolean("Immune2HPV"));
        }
        if (nbt.contains("Hysterectomy")){
            setHysterectomy(nbt.getBoolean("Hysterectomy"));
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

    default void setHysterectomy(boolean hysterectomy){}
    default boolean isHysterectomy(){
        return false;
    }

    default float getEctopicPregnancyProbability(){
        float probability = 0.02f;
        if (this instanceof LivingEntity entity){
            if (entity.getAttributeValue(EntityAttributes.GENERIC_SCALE) < 1){
                probability += 0.1f;
            }
            if (entity.getStatusEffects().stream().anyMatch(effect -> !effect.getEffectType().value().isBeneficial())){
                probability += 0.1f;
            }
            var luck = entity.getAttributeInstance(EntityAttributes.GENERIC_LUCK);
            if (luck != null){
                probability -= (float) (luck.getValue() * 0.01f);
            }
        }
        if (this instanceof MobEntity mob){
            if (mob.isBaby()){
                probability += 0.1f;
            }
        }
        return Math.max(probability, 0.01f);
    }
    default float getHydatidiformMoleProbability(){
        float probability = 0.01f;
        if (this instanceof LivingEntity entity){
            if (entity.getAttributeValue(EntityAttributes.GENERIC_SCALE) < 1){
                probability += 0.1f;
            }
            if (entity.getStatusEffects().stream().anyMatch(effect -> !effect.getEffectType().value().isBeneficial())){
                probability += 0.05f;
            }
        }
        return Math.max(probability, 0.01f);
    }
    default int calculateBabyCount(LivingEntity target){
        // 获取对方目前的体型
        double targetScale = target.getAttributeBaseValue(EntityAttributes.GENERIC_SCALE);
        // 计算对方真实体积
        EntityDimensions targetDimensions = target.getDimensions(target.getPose());
        double targetVolume = targetDimensions.width() * targetDimensions.height() * targetDimensions.height();
        // 真实体积
        double targetRealVolume = targetScale * targetVolume;
        // 获取自己目前的体型
        double selfScale = ((LivingEntity)this).getAttributeValue(EntityAttributes.GENERIC_SCALE);
        // 自己的体型与(对方体积除以4)相除
        double r = selfScale*selfScale*selfScale / (targetRealVolume / 4);
        // 向上取整
        int baseBabyCount = (int)Math.ceil(r);
        // 随机上下2个波动（必须大于等于1）
        int babyCount = baseBabyCount + (int)(Math.random() * 3) - 1;
        return Math.max(babyCount, 1);
    }

    default void setEctopicPregnancy(boolean ectopicPregnancy){
    }

    default boolean isEctopicPregnancy(){
        return false;
    }
    
    default void setAids(int time){
    }
    
    default int getAids(){
        return 0;
    }
    default void updateAids() {
        if (getAids() > 0){
            setAids(getAids()+1);
        }
    }

    default void setHydatidiformMole(boolean hydatidiformMole){}
    default boolean isHydatidiformMole(){
        return false;
    }

    default void setBabyCount(int count){}
    default int getBabyCount(){
        return 0;
    }

    default void setHPV(int time){}
    default int getHPV(){
        return 0;
    }
    default void updateHPV() {
        if (getHPV() > 0){
            setHPV(getHPV()+1);
        }
    }
    default void setImmune2HPV(boolean bl){
    }
    default boolean isImmune2HPV(){
        return false;
    }


    static <T extends LivingEntity&Pregnant> void pregnantTick(T pregnant) {
        if (pregnant.isHysterectomy()){
            // 清除怀孕效果（如果有的话）
            pregnant.setEctopicPregnancy(false);
            pregnant.setHydatidiformMole(false);
            pregnant.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPREGNANT_EFFECT()));
            return;
        }
        pregnant.updatePregnant();
        if (!pregnant.isPregnant()) {
            // 清除怀孕效果（如果有的话）
            pregnant.setEctopicPregnancy(false);
            pregnant.setHydatidiformMole(false);
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
            if (pregnant.isHydatidiformMole()) {
                // 1/200的概率随机掉1~3血
                if (pregnant.getRandom().nextInt(200) == 0) {
                    pregnant.damage(pregnant.getDamageSources().generic(), pregnant.getRandom().nextInt(3) + 1);
                }
                // 1/400的概率反胃
                if (pregnant.getRandom().nextInt(400) == 0) {
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20*20, 0));
                }
                // 1/600的概率掉落奇怪物品
                if (pregnant.getRandom().nextInt(600) == 0) {
                    pregnant.dropItem(JRItems.Companion.getMOLE());
                }
            }
            if (pregnant.isEctopicPregnancy()) {
                int time = pregnant.getPregnant();
                if (20 * 60 * 20 * 8 > time && time > 20 * 60 * 20 * 7) {
                    // 怀孕2~3天时1/1000概率掉血
                    if (pregnant.getRandom().nextInt(1000) == 0) {
                        pregnant.damage(pregnant.getDamageSources().generic(), 1.0F);
                    }
                    // 1/2000概率反胃
                    if (pregnant.getRandom().nextInt(2000) == 0) {
                        pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20*10, 0));
                    }
                }else if (20 * 60 * 20 * 7 > time && time > 20 * 60 * 20 * 6){
                    // 怀孕3~4天时1/200概率掉血
                    if (pregnant.getRandom().nextInt(200) == 0) {
                        pregnant.damage(pregnant.getDamageSources().generic(), 2.0F);
                    }
                } else if (20 * 60 * 20 * 6 > time) {
                    // 怀孕4~5天时1/50概率掉血
                    if (pregnant.getRandom().nextInt(50) == 0) {
                        pregnant.damage(pregnant.getDamageSources().generic(), 6.0F);
                    }
                    // 1/400概率昏迷
                    if (pregnant.getRandom().nextInt(400) == 0) {
                        pregnant.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getFAINT_EFFECT()), 20*60, 0));
                    }
                }
            }
            if (pregnant.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getVAGINITIS_EFFECT())) && pregnant.getPregnant() < 20*60*20*3){
                // 阴道炎&小于3天，有几率早产
                pregnant.setPregnant(pregnant.getPregnant() + 1);
                if (pregnant.getRandom().nextInt(500) == 0) {
                    pregnant.setPregnant(0);
                    pregnant.makeBaby(true);
                }
            }
        }
    }
    static <T extends LivingEntity&Pregnant> void menstruationTick(T pregnant) {
        if (pregnant.isHysterectomy()){
            pregnant.setMenstruation(0);
            return;
        }
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
        // /1000的几率附加
        if (pregnant.getRandom().nextInt(1000) == 0) {
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
    
    static <T extends LivingEntity&Pregnant> void aidsTick(T pregnant) {
        pregnant.updateAids();
        int aids = pregnant.getAids();
        if (aids > 0){
            // 给予效果
            pregnant.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getAIDS_EFFECT()), pregnant.getAids(), 0));
            if (aids < 20 * 60 * 20){
                // 1~2天内1/500反胃，缓慢，失明，虚弱
                if (pregnant.getRandom().nextInt(500) == 0) {
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20*10, 0));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20*10, 0));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20*10, 0));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20*10, 0));
                }
            }
            if (aids > 20*60*20*10){
                // 大于10天后1/20随机凋零，缓慢，失明，虚弱，剧毒，反胃
                if (pregnant.getRandom().nextInt(20) == 0) {
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 20*10, 2));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20*10, 2));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20*10, 2));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20*10, 2));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 20*10, 2));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20*10, 2));
                }
            }
        }
    }

    static <T extends LivingEntity&Pregnant> void HPVTick(T pregnant) {
        if (pregnant.isHysterectomy()){
            pregnant.setHPV(0);
            return;
        }
        pregnant.updateHPV();
        int hpv = pregnant.getHPV();
        if (20 * 60 * 20 * 3 <= hpv){
            // 设置效果
            pregnant.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getHPV_EFFECT()), hpv, 0));
        }
        if (20 * 60 * 20 * 3 <= hpv && hpv < 20 * 60 * 20 * 6){
            // 4~6天内1/40低级挖掘疲劳
            if (pregnant.getRandom().nextInt(40) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20*10, 0));
            }
        }else if (hpv >= 20 * 60 * 20 * 6 && hpv < 20 * 60 * 20 * 10){
            // 7~10天内1/80低级挖掘疲劳+掉血
            if (pregnant.getRandom().nextInt(80) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20*20, 0));
                pregnant.damage(pregnant.getDamageSources().generic(), 1);
            }
        }else if (hpv >= 20 * 60 * 20 * 10){
            // 10~12天内1/10高级挖掘疲劳+缓慢
            if (pregnant.getRandom().nextInt(10) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20*20, 1));
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20*20, 1));
            }
            // 1/400晕倒
            if (pregnant.getRandom().nextInt(400) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getFAINT_EFFECT()), 20*30, 0));
            }
            // 1/40掉血
            if (pregnant.getRandom().nextInt(40) == 0) {
                pregnant.damage(pregnant.getDamageSources().magic(), 1.0F);
            }
            // 大于12天直接死亡
            if (hpv > 20 * 60 *20 *12){
                pregnant.kill();
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
