package org.cneko.justarod.entity;

import lombok.Getter;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.item.JRComponents;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.item.custom.PantsuItem;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface Pregnant{
    List<UUID> FOREVER_BABY = new ArrayList<>();

    // 可以同时为男性或女性

    default boolean isMale() {
        return false;
    }
    default void setMale(boolean male){
    }
    default boolean isFemale(){
        return false;
    }
    default void setFemale(boolean female){
    }

    default void tryPregnant() {
        this.setPregnant(20*60*20*5);
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
        if (!isFemale()) return MenstruationCycle.NONE;
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
        if (!isFemale()) return;
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
        boolean noMatingPlz = false;
        if (this instanceof BDSMable bm){
            if (bm.getNoMatingPlz() >0){
                noMatingPlz = true;
            }
        }
        boolean menstruationOk = isMale();
        if (isFemale()){
            menstruationOk = getMenstruationCycle() == MenstruationCycle.OVULATION;
        }

        boolean isSevereUterineCold = getUterineCold() > 20 * 60 * 20 * 2; // 积累超过2天寒气视为严重

        return menstruationOk && !this.isPregnant() && !this.isSterilization() && this.hasUterus() && !this.isPCOS()
                && !(this.getBrithControlling() > 0 && ((Entity)this).getRandom().nextInt(10) != 0) && !noMatingPlz
                && !isSevereUterineCold;
    }

    default void writePregnantToNbt(NbtCompound nbt) {
        nbt.putBoolean("Female", isFemale());
        nbt.putBoolean("Male", isMale());
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
        nbt.putBoolean("Uterus",hasUterus());
        nbt.putBoolean("PCOS",isPCOS());
        nbt.putInt("BrithControlling",getBrithControlling());
        nbt.putInt("OvarianCancer",getOvarianCancer());
        nbt.putInt("BreastCancer",getBreastCancer());
        nbt.putInt("Syphilis",getSyphilis());
        nbt.putInt("Excretion",getExcretion());
        nbt.putInt("Urination",getUrination());
        nbt.putBoolean("Amputated",isAmputated());
        nbt.putBoolean("Orchiectomy",isOrchiectomy());
        nbt.putInt("UterineCold", getUterineCold());
        nbt.putInt("Urethritis", getUrethritis());
    }
    default void readPregnantFromNbt(NbtCompound nbt) {
        setFemale(nbt.getBoolean("Female"));
        setMale(nbt.getBoolean("Male"));
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
        if (nbt.contains("Uterus")){
            setHasUterus(nbt.getBoolean("Uterus"));
        }else {
            if (this.isMale()){
                setHasUterus(false);
            }
            if (this.isFemale()){
                setHasUterus(true);
            }
        }
        if (nbt.contains("PCOS")){
            setPCOS(nbt.getBoolean("PCOS"));
        }
        if (nbt.contains("BrithControlling")){
            setBrithControlling(nbt.getInt("BrithControlling"));
        }
        if (nbt.contains("OvarianCancer")){
            setOvarianCancer(nbt.getInt("OvarianCancer"));
        }
        if (nbt.contains("BreastCancer")){
            setBreastCancer(nbt.getInt("BreastCancer"));
        }
        if (nbt.contains("Syphilis")){
            setSyphilis(nbt.getInt("Syphilis"));
        }
        if (nbt.contains("Excretion")){
            setExcretion(nbt.getInt("Excretion"));
        }
        if (nbt.contains("Urination")){
            setUrination(nbt.getInt("Urination"));
        }
        if (nbt.contains("Amputated")){
            setAmputated(nbt.getBoolean("Amputated"));
        }
        if (nbt.contains("Orchiectomy")){
            setOrchiectomy(nbt.getBoolean("Orchiectomy"));
        }
        if (nbt.contains("UterineCold")) {
            setUterineCold(nbt.getInt("UterineCold"));
        }
        if (nbt.contains("Urethritis")){
            setUrethritis(nbt.getInt("Urethritis"));
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

    default void setHasUterus(boolean uterus){}
    default boolean hasUterus(){
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

            if (this.isUterineCold()) {
                probability += 0.15f;
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
    default void setPCOS(boolean bl){}
    default boolean isPCOS(){
        return false;
    }
    default void setBrithControlling(int time){}
    default int getBrithControlling(){
        return 0;
    }
    default void updateBrithControlling() {
        if (getBrithControlling() > 0){
            setBrithControlling(getBrithControlling()-1);
        }
    }

    default void setOvarianCancer(int time){}
    default int getOvarianCancer(){
        return 0;
    }
    default void updateOvarianCancer() {
        if (getOvarianCancer() > 0){
            setOvarianCancer(getOvarianCancer()+1);
        }
    }

    default void setBreastCancer(int time){}
    default int getBreastCancer(){
        return 0;
    }
    default void updateBreastCancer() {
        if (getBreastCancer() > 0){
            setBreastCancer(getBreastCancer()+1);
        }
    }

    default void setSyphilis(int time){}
    default int getSyphilis(){
        return 0;
    }
    default void updateSyphilis() {
        if (getSyphilis() > 0){
            setSyphilis(getSyphilis()+1);
        }
    }

    default void setUterineCold(int value) {}
    default int getUterineCold() {
        return 0;
    }
    default boolean isUterineCold() {
        return getUterineCold() > 20 * 60 * 10;
    }



    //  --------------------- MALE --------------------------
    default void setOrchiectomy(boolean orchiectomy){}
    default boolean isOrchiectomy(){
        return false;
    }


    // ----------------------------- COMMON -----------------------------
    default void setExcretion(int time){}
    default int getExcretion(){
        return 0;
    }
    default void updateExcretion() {
        if (getExcretion() > 0){
            setExcretion(getExcretion()+1);
        }
    }

    default void setUrination(int time){}
    default int getUrination(){
        return 0;
    }
    default void updateUrination() {
        if (getUrination() > 0){
            setUrination(getUrination()+1);
        }
    }

    default void setAmputated(boolean amputated){}
    default boolean isAmputated(){return false;}

    // ----------------------------- URETHRITIS (尿道炎) -----------------------------
    default void setUrethritis(int time){}
    default int getUrethritis(){
        return 0;
    }
    default void updateUrethritis() {
        if (getUrethritis() > 0){
            setUrethritis(getUrethritis()+1);
        }
    }





    static <T extends LivingEntity&Pregnant> void pregnantTick(T pregnant) {
        pregnant.updateBrithControlling();
        if (!pregnant.hasUterus()){
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
        if (!pregnant.isFemale()) return;
        if (!pregnant.hasUterus() || pregnant.isPCOS()){
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
        if (!pregnant.isFemale()) return;
        if (!pregnant.hasUterus()){
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

    static <T extends LivingEntity&Pregnant> void ovarianCancerTick(T pregnant){
        if (!pregnant.isFemale()) return;
        if (!pregnant.hasUterus()){
            pregnant.setOvarianCancer(0);
        }
        pregnant.updateOvarianCancer();
        int oc = pregnant.getOvarianCancer();
        if (oc <= 0){
            pregnant.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getOVARIAN_CANCER_EFFECT()));
        }
        if (oc > 20*60*20*2){
            pregnant.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getOVARIAN_CANCER_EFFECT()), oc, 0));
        }
        if (oc >20*60*20*2 && oc <20*60*20*4){
            // 2～4天1/200出现恶心
            if (pregnant.getRandom().nextInt(200) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20*30));
            }
            // 1/200扣血0.1
            if (pregnant.getRandom().nextInt(200) == 0) {
                pregnant.damage(pregnant.getDamageSources().magic(),0.1f);
            }
            // 1/200挖掘疲劳
            if (pregnant.getRandom().nextInt(200) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20*30));
            }
        }else if (oc >= 20 * 60 * 20 *4){
            // 1/100恶心
            if (pregnant.getRandom().nextInt(100) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10));
            }
            // 1/2呼吸困难
            if (pregnant.getRandom().nextBoolean()) {
                int air = pregnant.getAir();
                if (air > 9) {
                    pregnant.setAir(air - 9);
                }
            }
        }
    }

    static <T extends LivingEntity&Pregnant> void breastCancerTick(T pregnant){
        if (!pregnant.isFemale()) return;
        pregnant.updateBreastCancer();
        int bc = pregnant.getBreastCancer();
        if (bc>20*60*20*2 && bc<20*60*20*4){
            // 1/200分泌物
            if (pregnant.getRandom().nextInt(200) == 0) {
                pregnant.dropStack(JRItems.Companion.getMOLE().getDefaultStack());
            }
        }else if (bc>=20*60*20*4){
            // 1/100缓慢
            if (pregnant.getRandom().nextInt(100) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20*30));
            }
            // 1/2呼吸困难
            if (pregnant.getRandom().nextBoolean()) {
                int air = pregnant.getAir();
                if (air > 9) {
                    pregnant.setAir(air - 9);
                }
            }
            // 1/100挖掘疲劳
            if (pregnant.getRandom().nextInt(100) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20*30));
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void syphilisTick(T pregnant) {
        pregnant.updateSyphilis();
        int syphilis = pregnant.getSyphilis();

        // 中期阈值（4 小时 tick 数）
        int midStage = 20 * 60 * 20 * 4;
        // 晚期阈值（8 小时 tick 数）
        int lateStage = 20 * 60 * 20 * 8;

        if (syphilis > 0){
            // 给予效果
            pregnant.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getSYPHILIS_EFFECT()), syphilis, 0));
        }else {
            // 移除效果
            pregnant.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getSYPHILIS_EFFECT()));
        }
        if (syphilis > midStage) {
            // 中期及以上：每隔一段时间轻微伤害
            if (pregnant.getRandom().nextInt(200) == 0) { // 大约每10秒触发一次
                pregnant.damage(pregnant.getDamageSources().magic(), 1.0F);
            }
        }

        if (syphilis > lateStage) {
            // 晚期：持续掉血
            if (pregnant.getRandom().nextInt(40) == 0) { // 每2秒掉一次
                pregnant.damage(pregnant.getDamageSources().magic(), 1.0F);
            }

            // 晚期并怀孕，有小概率流产
            if (pregnant.isPregnant() && pregnant.getRandom().nextInt(1000) == 0) {
                pregnant.miscarry();
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void excretionTick(T pregnant) {
        pregnant.updateExcretion();
        int excretion = pregnant.getExcretion();
        if (excretion > 20*60*20*2){
            // 开始缓慢...
            pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20*10, 0, false, false, true));
            if (pregnant.getRandom().nextInt(300) == 0) {
                pregnant.sendMessage(MutableText.of(new PlainTextContent.Literal("§a提示：按下")).append(Text.keybind("key.justarod.excrement"))
                        .append(Text.of("§a可以排便哦！")));
            }
        }
        if (excretion > 20*60*20*5){
            // 开始不适...
            if (pregnant.getRandom().nextInt(100) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20*10, 0));
            }
        }
        if (excretion > 20*60*20*8){
            // 开始剧烈不适...
            if (pregnant.getRandom().nextInt(50) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20*20, 1));
            }
            if (pregnant.getRandom().nextInt(200) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20*20, 1));
            }
        }
        if (excretion > 20*60*20*12){
            // 掉血
            if (pregnant.getRandom().nextInt(100) == 0) {
                pregnant.damage(pregnant.getDamageSources().magic(), 1.0F);
            }
            // 掉粑粑
            if (pregnant.getRandom().nextInt(200) == 0) {
                // 检查是否穿着胖次
                ItemStack legStack = pregnant.getEquippedStack(EquipmentSlot.LEGS);
                boolean hasPantsu = !legStack.isEmpty() && legStack.getItem() instanceof PantsuItem;

                if (hasPantsu) {
                    // 如果有胖次，不会掉落物品，而是弄脏胖次
                    legStack.set(JRComponents.Companion.getPANTSU_STATE(), JRComponents.PantsuState.SOILED);

                    pregnant.sendMessage(Text.of("§c糟糕，把胖次弄脏了..."));
                    // 给予更严重的恶心/缓慢效果因为身上有脏东西
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 60, 2));
                    pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 60, 2));
                } else {
                    // 没有胖次，正常掉落
                    pregnant.dropItem(JRItems.Companion.getEXCREMENT());
                }
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void urinationTick(T pregnant) {
        // 基础更新
        pregnant.updateUrination();

        // 怀孕加速产生: 基础每tick+1，孕妇有50%几率额外+1，平均速率为1.5倍
        if (pregnant.isPregnant() && pregnant.getRandom().nextBoolean()) {
            pregnant.setUrination(pregnant.getUrination() + 1);
        }

        int urination = pregnant.getUrination();
        int day = 20 * 60 * 20; // 1个Minecraft天

        // 阶段 1: 轻微尿意 (0.5天) -> 提示
        if (urination > day * 0.5) {
            // 5. 需要提示
            if (pregnant.getRandom().nextInt(300) == 0) {
                pregnant.sendMessage(MutableText.of(new PlainTextContent.Literal("§e提示：按下"))
                        .append(Text.keybind("key.justarod.urinate")) // 对应按键
                        .append(Text.of("§e可以排尿哦！")));
            }
        }

        // 阶段 2: 憋不住了 (0.8天) -> 负面效果: 缓慢 & 跳跃降低
        if (urination > day * 0.8) {
            // 缓慢 I
            pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 10, 0, false, false, true));
            // 2. 跳跃能力降低 (JUMP_NERF)
            pregnant.addStatusEffect(new StatusEffectInstance(
                    Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getJUMP_NERF_EFFECT()),
                    20 * 10,
                    0,
                    false,
                    false,
                    true
            ));
        }

        // 阶段 3: 极度憋尿 (1.2天) -> 负面效果: 强力缓慢 & 中毒
        if (urination > day * 1.2) {
            // 剧烈不适，加大缓慢等级
            if (pregnant.getRandom().nextInt(50) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 20, 1));
            }
            // 2. 只有毒效果符合尿毒症/膀胱受损的设定
            if (pregnant.getRandom().nextInt(200) == 0) {
                pregnant.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 20 * 20, 0));
            }
        }

        // 阶段 4: 失禁 (1.5天)
        if (urination > day * 1.5) {
            if (pregnant.getRandom().nextInt(100) == 0) {
                pregnant.sendMessage(Text.of("§c你失禁了..."));

                // 检查胖次
                ItemStack legStack = pregnant.getEquippedStack(EquipmentSlot.LEGS);
                if (!legStack.isEmpty() && legStack.getItem() instanceof PantsuItem) {
                    // 如果还没脏，就变成湿的；如果已经脏了，保持脏的状态（假设脏优先级更高）
                    JRComponents.PantsuState currentState = legStack.get(JRComponents.Companion.getPANTSU_STATE());
                    if (currentState == null || currentState == JRComponents.PantsuState.CLEAN) {
                        legStack.set(JRComponents.Companion.getPANTSU_STATE(), JRComponents.PantsuState.WET);
                        pregnant.sendMessage(Text.of("§c胖次湿透了..."));
                    }
                }
                pregnant.addStatusEffect(new StatusEffectInstance(
                        Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getSMEARY_EFFECT()),
                        20 * 60 * 5,
                        0, false, false, true
                ));

                // 排空膀胱
                pregnant.setUrination(0);
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void uterineColdTick(T entity) {
        if (!entity.hasUterus()) {
            entity.setUterineCold(0);
            return;
        }

        int currentCold = entity.getUterineCold();
        World world = entity.getWorld();
        net.minecraft.util.math.BlockPos pos = entity.getBlockPos();

        // 1. 寒气积累逻辑 (每 Tick 更新)
        boolean isEnvironmentCold = false;

        // 环境判断：寒冷群系 或 水中
        if (world.getBiome(pos).value().isCold(pos) || entity.isSubmergedInWater()) {
            isEnvironmentCold = true;
            // 基础增加
            if (entity.getRandom().nextInt(2) == 0) { // 减缓一下增长速度
                currentCold++;
            }
        }
        // 胖次保暖逻辑
        if (isEnvironmentCold) {
            ItemStack legStack = entity.getEquippedStack(EquipmentSlot.LEGS);
            if (!legStack.isEmpty() && legStack.getItem() instanceof PantsuItem) {
                // 如果穿着胖次，有几率抵消这次寒气增加
                if (entity.getRandom().nextInt(4) != 0) {
                    isEnvironmentCold = false; // 视为不冷
                }
            }
        }

        // 接触判断：脚下是冰、雪、雪块
        net.minecraft.block.Block blockBelow = world.getBlockState(pos.down()).getBlock();
        if (blockBelow == net.minecraft.block.Blocks.ICE ||
                blockBelow == net.minecraft.block.Blocks.PACKED_ICE ||
                blockBelow == net.minecraft.block.Blocks.BLUE_ICE ||
                blockBelow == net.minecraft.block.Blocks.SNOW_BLOCK ||
                blockBelow == net.minecraft.block.Blocks.SNOW ||
                blockBelow == net.minecraft.block.Blocks.POWDER_SNOW) {
            isEnvironmentCold = true;
            currentCold += 2; // 接触寒冷源增加更快
        }

        // 2. 暖宫/恢复逻辑
        // 搜索周围小范围是否有热源 (火、岩浆、营火)
        boolean isWarm = false;
        if (world.getStatesInBoxIfLoaded(entity.getBoundingBox().expand(2.0)).anyMatch(state ->
                state.isOf(net.minecraft.block.Blocks.FIRE) ||
                        state.isOf(net.minecraft.block.Blocks.SOUL_FIRE) ||
                        state.isOf(net.minecraft.block.Blocks.LAVA) ||
                        state.isOf(net.minecraft.block.Blocks.CAMPFIRE) ||
                        state.isOf(net.minecraft.block.Blocks.SOUL_CAMPFIRE) ||
                        state.isOf(net.minecraft.block.Blocks.MAGMA_BLOCK)
        )) {
            isWarm = true;
            currentCold -= 5; // 恢复速度快于积累速度
        }

        // 自然代谢：如果环境不冷，身体会慢慢自我调节（很慢）
        if (!isEnvironmentCold && !isWarm && currentCold > 0) {
            if (entity.getRandom().nextInt(20) == 0) {
                currentCold--;
            }
        }

        // 限制范围
        if (currentCold < 0) currentCold = 0;
        // 限制最大值 (积累上限为5天)
        if (currentCold > 20 * 60 * 20 * 5) currentCold = 20 * 60 * 20 * 5;

        entity.setUterineCold(currentCold);

        // 3. 状态效果逻辑
        // 如果寒气值超过阈值（积累了1天），给予宫寒效果
        if (entity.isUterineCold()) {
            entity.addStatusEffect(new StatusEffectInstance(
                    Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getUTERINE_COLD_EFFECT()),
                    20 * 5, // 持续时间短，保持刷新
                    0,
                    false,
                    false,
                    true // 显示图标
            ));

            // 如果寒气非常严重（超过3天），加深效果等级
            if (currentCold > 20 * 60 * 20 * 3) {
                entity.addStatusEffect(new StatusEffectInstance(
                        Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getUTERINE_COLD_EFFECT()),
                        20 * 5,
                        1,
                        false,
                        false,
                        true
                ));
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void urethritisTick(T entity) {
        entity.updateUrethritis();
        int time = entity.getUrethritis();

        if (time <= 0) return;

        // 阶段定义
        int stage1 = 20 * 60 * 20 * 2; // 2天：潜伏期结束，开始有轻微症状
        int stage2 = 20 * 60 * 20 * 5; // 5天：症状加重，开始有分泌物
        int stage3 = 20 * 60 * 20 * 8; // 8天：严重感染，伴随疼痛

        // --- 症状 1: 尿频/尿急 (加速 Urination 增长) ---
        if (time > stage1) {
            // 基础概率：每tick有 1/10 概率额外增加1点尿意值
            // 这会让尿意积累速度变成原来的约 1.5 倍到 2 倍
            if (entity.getRandom().nextInt(10) == 0) {
                entity.setUrination(entity.getUrination() + 1);
            }
            entity.addStatusEffect(
                    new StatusEffectInstance(
                            Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getURETHRITIS_EFFECT()),
                            time,
                            0,
                            false,
                            false,
                            true
                    )
            );
        }

        // --- 症状 2: 异常分泌物 (弄脏胖次 / 粘腻效果) ---
        if (time > stage2) {
            // 每 5~10 分钟一次判定
            if (entity.getRandom().nextInt(20 * 10 * 5) == 0) {
                ItemStack legStack = entity.getEquippedStack(EquipmentSlot.LEGS);
                boolean hasPantsu = !legStack.isEmpty() && legStack.getItem() instanceof PantsuItem;

                if (hasPantsu) {
                    // 如果穿着胖次，直接弄脏（模拟脓性分泌物）
                    JRComponents.PantsuState currentState = legStack.get(JRComponents.Companion.getPANTSU_STATE());
                    // 只有当胖次是干净的时候才弄脏，避免覆盖更严重的脏污状态
                    if (currentState == null || currentState == JRComponents.PantsuState.CLEAN) {
                        legStack.set(JRComponents.Companion.getPANTSU_STATE(), JRComponents.PantsuState.SOILED);
                    }
                } else {
                    // 没穿胖次，分泌物留在大腿上 -> 给予 SMEARY (粘腻) 效果
                    entity.addStatusEffect(new StatusEffectInstance(
                            Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getSMEARY_EFFECT()),
                            20 * 60 * 5, // 持续5分钟
                            0, false, false, true
                    ));
                    entity.sendMessage(Text.of("§e流出了奇怪的脓液..."));
                }
            }
        }

        // --- 症状 3: 尿痛 (烧灼感) & 男性额外惩罚 ---
        if (time > stage3) {
            // 1. 尿痛：基于当前尿意值判定的疼痛
            // 尿意越浓，炎症刺激越痛
            if (entity.getUrination() > 20 * 60 * 10) { // 憋了一点尿的时候
                if (entity.getRandom().nextInt(200) == 0) {
                    entity.damage(entity.getDamageSources().magic(), 1.0f);
                    // 偶尔伴随缓慢（痛得走不动）
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 5, 0));
                }
            }

            // 2. 男性专属惩罚 (模拟上行感染导致的前列腺炎/附睾炎)
            if (entity.isMale()) {
                // 挖掘疲劳 (模拟腹股沟/腰部酸痛)
                if (!entity.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 20, 0, false, false, false));
                }

                // 虚弱 (由于剧烈疼痛和炎症发热)
                if (entity.getRandom().nextInt(300) == 0) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 30, 0));
                }

                // 受到额外伤害 (男性尿道更长，炎症不仅痛苦且难以痊愈)
                if (entity.getRandom().nextInt(100) == 0) {
                    entity.damage(entity.getDamageSources().generic(), 1.0f);
                }
            }
            if (entity.isFemale()){
                // 女性虽然也有痛感，但通常比男性轻微一点点
                // 仅仅是偶尔的刺痛
                if (entity.getRandom().nextInt(300) == 0) {
                    entity.damage(entity.getDamageSources().magic(), 0.5f);
                }
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void amputatedTick(T entity) {
        if (entity.isAmputated()){
            entity.addStatusEffect(
                    new StatusEffectInstance(
                            Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getJUMP_NERF_EFFECT()),
                            20,
                            10, // 1秒刷新一次
                            true,
                            false
                    )
            );
            entity.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            20,
                            10,
                            true,
                            false
                    )
            );
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
