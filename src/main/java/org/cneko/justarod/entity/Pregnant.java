package org.cneko.justarod.entity;

import lombok.Getter;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.item.JRComponents;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.item.custom.PantsuItem;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;
import org.cneko.toneko.common.mod.entities.INeko;

import java.util.*;

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
        // 破处
        this.ruptureHymen("怀孕");
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
                if (baby instanceof LivingEntity babyLiving && getParthenogenesisVariance() > 0) {
                    float variance = getParthenogenesisVariance();
                    List<RegistryEntry<EntityAttribute>> attributes = new ArrayList<>();

                    // 始终可选的其他属性
                    attributes.add(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    attributes.add(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                    attributes.add(EntityAttributes.GENERIC_SCALE);

                    // 随机决定额外选择几个属性（0~3个），总变异数为 1~4（因为生命值必选）
                    Random random = new Random();
                    int extraCount = random.nextInt(4); // 0, 1, 2, or 3

                    // 打乱并选取 extraCount 个其他属性
                    Collections.shuffle(attributes, random);
                    List<RegistryEntry<EntityAttribute>> selected = new ArrayList<>();
                    selected.add(EntityAttributes.GENERIC_MAX_HEALTH); // 必选
                    selected.addAll(attributes.subList(0, extraCount));

                    // 应用变异
                    for (RegistryEntry<EntityAttribute> attr : selected) {
                        applyAttributeVariance(babyLiving, attr, variance);
                    }

                    // 变异后回满血（因为最大生命值可能已变）
                    babyLiving.setHealth(babyLiving.getMaxHealth());
                }
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

        boolean physicalBlock = isImperforateHymen();

        return menstruationOk && !this.isPregnant() && !this.isSterilization() && this.hasUterus() && !this.isPCOS()
                && !(this.getBrithControlling() > 0 && ((Entity)this).getRandom().nextInt(10) != 0) && !noMatingPlz
                && !isSevereUterineCold && !physicalBlock && this.getCorpusLuteumRupture() <= 0;
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
        nbt.putBoolean("Immune2AIDS", isImmune2Aids());
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
        nbt.putInt("Prostatitis", getProstatitis());
        nbt.putInt("Hemorrhoids", getHemorrhoids());
        nbt.putFloat("ParthenogenesisVariance", getParthenogenesisVariance());
        nbt.putBoolean("HasHymen", hasHymen());
        nbt.putBoolean("ImperforateHymen", isImperforateHymen());
        nbt.putBoolean("ProtogynyEnabled", isProtogynyEnabled());
        nbt.putBoolean("IsUndergoingProtogyny", isUndergoingProtogyny());
        nbt.putInt("ProtogynyProgress", getProtogynyProgress());
        nbt.putFloat("Hormone_T", getTestosterone());
        nbt.putFloat("Hormone_E", getEstrogen());
        nbt.putFloat("Hormone_P", getProgesterone());
        nbt.putInt("Cataract", getCataract());
        nbt.putInt("CorpusLuteumRupture", getCorpusLuteumRupture());
        nbt.putBoolean("SevereCorpusLuteumRupture", isSevereCorpusLuteumRupture());
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
        if (nbt.contains("Immune2AIDS")){
            setImmune2Aids(nbt.getBoolean("Immune2AIDS"));
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
        if (nbt.contains("Prostatitis")){
            setProstatitis(nbt.getInt("Prostatitis"));
        }
        if (nbt.contains("Hemorrhoids")) {
            setHemorrhoids(nbt.getInt("Hemorrhoids"));
        }
        if (nbt.contains("ParthenogenesisVariance")) {
            setParthenogenesisVariance(nbt.getFloat("ParthenogenesisVariance"));
        }
        if (nbt.contains("HasHymen")) {
            setHasHymen(nbt.getBoolean("HasHymen"));
        } else {
            if (hasUterus()) {
                setHasHymen(true);
            }
        }
        if (nbt.contains("ImperforateHymen")) {
            setImperforateHymen(nbt.getBoolean("ImperforateHymen"));
        }
        if (nbt.contains("ProtogynyEnabled")) {
            setProtogynyEnabled(nbt.getBoolean("ProtogynyEnabled"));
        }
        if (nbt.contains("IsUndergoingProtogyny")) {
            setUndergoingProtogyny(nbt.getBoolean("IsUndergoingProtogyny"));
        }
        if (nbt.contains("ProtogynyProgress")) {
            setProtogynyProgress(nbt.getInt("ProtogynyProgress"));
        }
        if (nbt.contains("Hormone_T")) setTestosterone(nbt.getFloat("Hormone_T"));
        if (nbt.contains("Hormone_E")) setEstrogen(nbt.getFloat("Hormone_E"));
        if (nbt.contains("Hormone_P")) setProgesterone(nbt.getFloat("Hormone_P"));
        if (nbt.contains("Cataract")) {
            setCataract(nbt.getInt("Cataract"));
        }
        if (nbt.contains("CorpusLuteumRupture")) {
            setCorpusLuteumRupture(nbt.getInt("CorpusLuteumRupture"));
        }
        if (nbt.contains("SevereCorpusLuteumRupture")) {
            setSevereCorpusLuteumRupture(nbt.getBoolean("SevereCorpusLuteumRupture"));
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
    default void setImmune2Aids(boolean bl){
    }
    default boolean isImmune2Aids(){
        return false;
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

// ----------------- 处女膜 & 闭锁 ------------------------------

    default void setHasHymen(boolean hasHymen) {}
    /**
     * 是否拥有完整的处女膜
     */
    default boolean hasHymen() {
        return false;
    }

    default void setImperforateHymen(boolean imperforate) {}
    /**
     * 是否患有（先天性畸形）
     * 如果为 true，会导致经血无法排出（腹痛）且无法自然受孕
     */
    default boolean isImperforateHymen() {
        return false;
    }

    /**
     * 执行处女膜切开术/修复术 (手术)
     * 用于治疗处女膜闭锁，或者单纯的手术破坏
     */
    default void performHymenotomy() {
        if (isImperforateHymen() || hasHymen()) {
            setImperforateHymen(false);
            setHasHymen(false);
            if (this instanceof LivingEntity entity) {
                entity.sendMessage(Text.of("§a手术成功，处女膜闭锁已解除。"));
            }
        }
    }

    /**
     * 破处
     * @return 是否成功破处
     */
    default boolean ruptureHymen(String cause) {
        if (!hasHymen()) return false;

        if (isImperforateHymen()) return false;

        setHasHymen(false);

        if (this instanceof LivingEntity entity) {
            // 1. 扣血 (撕裂痛)
            entity.damage(entity.getDamageSources().generic(), 2.0f);

            // 2. 负面效果 (疼痛导致的虚弱)
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 30, 0));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 10, 0));

            entity.sendMessage(Text.of("§c下体感到一阵撕裂般的剧痛... (" + cause + ")"));

            // 3. 弄脏胖次 / 落红逻辑
            ItemStack legStack = entity.getEquippedStack(EquipmentSlot.LEGS);
            boolean hasPantsu = !legStack.isEmpty() && legStack.getItem() instanceof PantsuItem;

            if (hasPantsu) {
                JRComponents.PantsuState currentState = legStack.get(JRComponents.Companion.getPANTSU_STATE());
                // 只有干净的时候才染红
                if (currentState == null || currentState == JRComponents.PantsuState.CLEAN) {
                    legStack.set(JRComponents.Companion.getPANTSU_STATE(), JRComponents.PantsuState.BLOODY);
                }
            } else {
                // 没穿胖
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 0));
            }
        }
        return true;
    }

    //  --------------------- MALE --------------------------
    default void setOrchiectomy(boolean orchiectomy){}
    default boolean isOrchiectomy(){
        return false;
    }

    // ----------------------------- PROSTATITIS (前列腺炎) -----------------------------
    default void setProstatitis(int time){}
    default int getProstatitis(){
        return 0;
    }
    default void updateProstatitis() {
        if (getProstatitis() > 0){
            setProstatitis(getProstatitis()+1);
        }
    }
    default void cureProstatitis(int amount) {
        int current = getProstatitis();
        if (current > 0) {
            int time = Math.max(0, current - amount);
            setProstatitis(time);

            if (time == 0 && this instanceof LivingEntity entity) {
                entity.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPROSTATITIS_EFFECT()));
                entity.sendMessage(Text.of("§a你的前列腺不再疼痛了。"));
            }
        }
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

    // ----------------------------- HEMORRHOIDS (痔疮) -----------------------------
    default void setHemorrhoids(int time){}
    default int getHemorrhoids(){
        return 0;
    }

    // 手动排便时调用此方法检测是否疼痛
    default void doDefecationPain() {
        if (this instanceof LivingEntity entity && getHemorrhoids() > 20 * 60 * 20 * 2) { // 严重程度超过2天
            entity.damage(entity.getDamageSources().generic(), 2.0f);
            entity.sendMessage(Text.of("§c肛门像撕裂一样疼痛..."));
            // 严重的会有流血效果（缓慢+虚弱）
            if (getHemorrhoids() > 20 * 60 * 20 * 5) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 10, 0));
            }
        }
    }

    // ----------------- 雌转雄机制 (Protogyny) -----------------

    // 变性总时长：5分钟 (6000 ticks)
    int PROTOGYNY_TOTAL_DURATION = 20 * 60 * 5;
    // 雄性特征发育时间点：3分钟 (3600 ticks)
    int PROTOGYNY_MALE_DEVELOP_TIME = 20 * 60 * 3;

    /**
     * 该实体是否开启了“雌转雄”能力的开关
     */
    default void setProtogynyEnabled(boolean enabled) {}
    default boolean isProtogynyEnabled() {
        return false;
    }

    /**
     * 是否正在经历变性过程
     */
    default void setUndergoingProtogyny(boolean undergoing) {}
    default boolean isUndergoingProtogyny() {
        return false;
    }

    /**
     * 变性进度 (Tick)
     */
    default void setProtogynyProgress(int progress) {}
    default int getProtogynyProgress() {
        return 0;
    }

    /**
     * 触发变性的体型阈值
     * 默认设定：体型大于 1.2 倍标准大小时尝试触发
     */
    default double getProtogynyScaleThreshold() {
        return 1.2;
    }


    // ----------------- 孤雌生殖 ------------------------------
    default void setParthenogenesisVariance(float variance) {}
    default float getParthenogenesisVariance() {
        return 0.0f;
    }
    private void applyAttributeVariance(LivingEntity entity, RegistryEntry<EntityAttribute> attribute, float variance) {
        var instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            double base = instance.getBaseValue();
            // 生成 -1.0 到 1.0 之间的随机数
            double randomFactor = entity.getRandom().nextDouble() * 2.0 - 1.0;
            // 计算倍率：例如 variance=0.1, random=-0.5 => multiplier = 1.0 + (-0.05) = 0.95
            double multiplier = 1.0 + (randomFactor * variance);
            instance.setBaseValue(base * multiplier);
        }
    }

    // ----------------- 激素系统 (Hormones) -----------------
    Identifier TESTOSTERONE_ID = Identifier.of(Justarod.MODID,"testosterone");
    Identifier ESTROGEN_ID = Identifier.of(Justarod.MODID,"estrogen");
    default void setTestosterone(float value) {}
    default float getTestosterone() { return 0.0f; }

    default void setEstrogen(float value) {}
    default float getEstrogen() { return 0.0f; }

    default void setProgesterone(float value) {} // 孕酮
    default float getProgesterone() { return 0.0f; }


    /**
     * 强制转为雄性
     * 效果：清除女性器官/病症，获得男性特征。
     * 属性判定：如果之前不是男性，属性提升。
     */
    default void forceToMale() {
        boolean wasMale = this.isMale();
        boolean isEntity = this instanceof LivingEntity;

        // 1. 属性变更判定：之前不属于男性 -> 属性上升
        if (isEntity && !wasMale) {
            applySexChangeAttributeModifier((LivingEntity) this, true);
        }

        // 2. 性别设定
        this.setMale(true);
        this.setFemale(false);

        // 3. 移除女性器官与状态
        this.setHasUterus(false);
        this.setHasHymen(false);
        this.setImperforateHymen(false);

        // 4. 重置/赋予男性器官
        this.setOrchiectomy(false); // 恢复睾丸（如果有切除）
        // 前列腺炎归零，代表健康的前列腺
        this.setProstatitis(0);
    }

    /**
     * 强制转为雌性
     * 效果：清除男性器官/病症，获得女性特征。
     * 属性判定：如果之前不是女性，属性下降。
     */
    default void forceToFemale() {
        boolean wasFemale = this.isFemale();
        boolean isEntity = this instanceof LivingEntity;

        // 1. 属性变更判定：之前不属于女性 -> 属性下降
        if (isEntity && !wasFemale) {
            applySexChangeAttributeModifier((LivingEntity) this, false);
        }

        // 2. 性别设定
        this.setMale(false);
        this.setFemale(true);

        // 3. 赋予女性器官
        this.setHasUterus(true);
        // 默认给予完整的处女膜，除非是强制转换通常意味着重塑
        this.setHasHymen(true);
        this.setImperforateHymen(false); // 默认为健康状态

        // 4. 移除男性状态
        this.setProstatitis(0);
        this.setOrchiectomy(true); // 概念上移除睾丸（虽然isMale=false已经屏蔽了逻辑）
    }

    /**
     * 强制转为双性 (扶她/雌雄同体)
     * 效果：同时拥有男性和女性的性征和器官。
     * 属性判定：属于男性 -> 下降；属于女性 -> 上升。
     */
    default void forceToMaleAndFemale() {
        boolean wasMale = this.isMale();
        boolean wasFemale = this.isFemale();
        boolean isEntity = this instanceof LivingEntity;

        // 1. 属性变更判定
        if (isEntity) {
            if (wasMale && !wasFemale) {
                // 属于纯男性 -> 下降
                applySexChangeAttributeModifier((LivingEntity) this, false);
            } else if (!wasMale && wasFemale) {
                // 属于纯女性 -> 上升
                applySexChangeAttributeModifier((LivingEntity) this, true);
            }
            // 既是男又是女，或者都不是 -> 属性不变
        }

        // 2. 性别设定
        this.setMale(true);
        this.setFemale(true);

        // 3. 赋予全套器官
        this.setHasUterus(true);
        if (!this.hasHymen()) this.setHasHymen(true); // 如果没有就给一个
        this.setOrchiectomy(false); // 有睾丸
    }

    /**
     * 强制转为无性别
     * 效果：清除所有性征和器官。
     * 属性判定：属于男性 -> 下降；属于女性 -> 上升。
     */
    default void forceToNoSex() {
        boolean wasMale = this.isMale();
        boolean wasFemale = this.isFemale();
        boolean isEntity = this instanceof LivingEntity;

        // 1. 属性变更判定 (同双性逻辑)
        if (isEntity) {
            if (wasMale && !wasFemale) {
                // 属于纯男性 -> 下降
                applySexChangeAttributeModifier((LivingEntity) this, false);
            } else if (!wasMale && wasFemale) {
                // 属于纯女性 -> 上升
                applySexChangeAttributeModifier((LivingEntity) this, true);
            }
        }

        // 2. 性别设定
        this.setMale(false);
        this.setFemale(false);

        // 3. 清除所有器官和状态
        this.setHasUterus(false);
        this.setHasHymen(false);
        this.setOrchiectomy(true); // 移除
    }

    /**
     * 内部辅助方法：处理属性的提升与下降
     * 提升：生命上限 +4.0 (2心)，攻击力 +1.0
     * 下降：生命上限 -4.0 (2心)，攻击力 -1.0
     */
    private void applySexChangeAttributeModifier(LivingEntity entity, boolean isBonus) {
        // 修改生命上限
        var healthAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            double currentBase = healthAttr.getBaseValue();
            double modifier = isBonus ? 4.0 : -4.0;
            double newValue = Math.max(1.0, currentBase + modifier); // 此时防止生命值归零
            healthAttr.setBaseValue(newValue);

            // 如果是扣血，需要同步更新当前血量，防止当前血量 > 上限
            if (!isBonus && entity.getHealth() > newValue) {
                entity.setHealth((float) newValue);
            }
        }

        // 修改攻击力
        var attackAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttr != null) {
            double currentBase = attackAttr.getBaseValue();
            double modifier = isBonus ? 1.0 : -1.0;
            // 攻击力最低为 0.5
            attackAttr.setBaseValue(Math.max(0.5, currentBase + modifier));
        }
    }

    // ----------------- 白内障 (Cataract) -----------------

    // 严重程度阈值 (Tick)
    int CATARACT_STAGE_1 = 20 * 60 * 20 * 2; // 2天：初期
    int CATARACT_STAGE_2 = 20 * 60 * 20 * 5; // 5天：中期（开始畏光）
    int CATARACT_STAGE_3 = 20 * 60 * 20 * 10; // 10天：晚期（严重白翳）

    default void setCataract(int ticks) {}
    default int getCataract() { return 0; }

    // 手术治疗白内障（换晶状体）
    default void cureCataract() {
        setCataract(0);
        if (this instanceof LivingEntity entity) {
            entity.sendMessage(Text.of("§a手术成功，眼前变得清晰了！"));
            // 手术后眼睛敏感，给予短时间畏光（失明/夜视闪烁）
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20 * 5, 0));
        }
    }

    // ----------------------------- CORPUS LUTEUM RUPTURE (黄体破裂) -----------------------------
    default void setCorpusLuteumRupture(int time) {}
    default int getCorpusLuteumRupture() {
        return 0;
    }

    default void setSevereCorpusLuteumRupture(boolean severe) {}
    default boolean isSevereCorpusLuteumRupture() {
        return false;
    }

    /**
     * 触发黄体破裂
     * @param cause 破裂原因（用于文本提示）
     */
    default boolean ruptureCorpusLuteum(String cause) {
        if (!isFemale() || !hasUterus()) return false;

        // 只有在黄体期才容易发生黄体破裂
        if (getMenstruationCycle() != MenstruationCycle.LUTEINIZATION) return false;
        // 如果已经破裂了则不重复触发
        if (getCorpusLuteumRupture() > 0) return false;

        // 随机判定是否为重症 (例如 20% 概率是大血管破裂的重症)
        boolean severe = ((Entity)this).getRandom().nextInt(100) < 20;
        setSevereCorpusLuteumRupture(severe);
        setCorpusLuteumRupture(20 * 60 * 3); // 启动计时器

        if (this instanceof LivingEntity entity) {
            // 瞬间的高额伤害 (重症 6点/3心，轻症 4点/2心)
            entity.damage(entity.getDamageSources().generic(), severe ? 6.0f : 4.0f);

            // 痛得无法动弹
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 15, 2));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 15, 1));

            entity.sendMessage(Text.of("§c你的下腹部突然传来一阵撕裂般的剧痛... (" + cause + ")"));
        }
        return true;
    }

    /**
     * 手术/药物 治愈黄体破裂
     */
    default void cureCorpusLuteumRupture() {
        if (getCorpusLuteumRupture() > 0) {
            setCorpusLuteumRupture(0);
            setSevereCorpusLuteumRupture(false);
            if (this instanceof LivingEntity entity) {
                entity.sendMessage(Text.of("§a经过及时治疗，腹腔内的出血停止了。"));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 0));
            }
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
        if (pregnant.isImmune2Aids()) {
            // 如果免疫，且身上有AIDS计数，将其清零（治愈）
            if (pregnant.getAids() > 0) {
                pregnant.setAids(0);
                // 顺便移除已有的药水效果
                pregnant.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getAIDS_EFFECT()));
            }
            return; // 直接返回，不执行后续 AIDS 逻辑
        }
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
                pregnant.doDefecationPain();
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

            // 2. 男性专属惩罚
            if (entity.isMale()) {
                if (entity.getProstatitis() == 0) {
                    // 平均每分钟检测一次
                    if (entity.getRandom().nextInt(1200) == 0) {
                        entity.setProstatitis(1); // 激活前列腺炎
                        entity.sendMessage(Text.of("§c你感觉会阴深处传来一阵坠胀感，炎症似乎蔓延了..."));
                    }
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

    static <T extends LivingEntity & Pregnant> void prostatitisTick(T entity) {
        // 只有男性会得前列腺炎
        if (!entity.isMale()) {
            if (entity.getProstatitis() > 0) entity.setProstatitis(0);
            return;
        }

        entity.updateProstatitis();
        int time = entity.getProstatitis();

        if (time <= 0) return;

        entity.addStatusEffect(new StatusEffectInstance(
                Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getPROSTATITIS_EFFECT()),
                time,
                0,
                false,
                false,
                true
        ));

        // 物理治疗 (温水坐浴) ---
        // 判定条件：身体在水中 (SubmergedInWater) 或者下半身在水中
        if (entity.isSubmergedInWater() || entity.isTouchingWater()) {
            // 加速恢复：每 tick 额外减少 5 点病程
            // 正常情况下病程是增加的 (updateProstatitis)，这里减去更多就能实现“慢慢治愈”
            entity.setProstatitis(Math.max(0, entity.getProstatitis() - 5));

            // 给予舒适提示 (偶尔)
            if (entity.getRandom().nextInt(600) == 0) {
                entity.sendMessage(Text.of("§a温水缓解了你的下半身胀痛..."));
            }
            return;
        }

        // ---------------- 1. 尿频逻辑 (Urination Acceleration) ----------------
        // 前列腺炎会导致尿意频繁
        // 基础 tick 增加 1，这里额外增加
        if (entity.getRandom().nextInt(3) == 0) {
            // 约增加 33% 的尿意积累速度
            entity.setUrination(entity.getUrination() + 1);
        }

        // ---------------- 2. 疼痛逻辑 (Pain) ----------------
        // 只要膀胱里有尿，前列腺充血就会导致疼痛
        // 即使尿意不高，也会有不适感
        if (entity.getUrination() > 20 * 60 * 5) { // 稍微有一点尿意时
            if (entity.getRandom().nextInt(400) == 0) { // 偶尔刺痛
                entity.damage(entity.getDamageSources().magic(), 1.0f);
                // 痛得缩了一下 (瞬间缓慢)
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 2, 1, false, false, false));
            }
        }

        // ---------------- 3. Debuffs (挖掘疲劳 & 虚弱) ----------------
        // 随着患病时间增加，症状加重

        // 阶段 1: 早期 (1天后) - 出现腰酸背痛 (挖掘疲劳)
        if (time > 20 * 60 * 20) {
            // 持续给予挖掘疲劳 I
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 20, 0, false, false, true));
        }

        // 阶段 2: 慢性期 (3天后) - 精神萎靡、身体虚弱 (虚弱)
        if (time > 20 * 60 * 20 * 3) {
            // 持续给予虚弱 I
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 20, 0, false, false, true));

            // 加重挖掘疲劳到 II 级
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 20, 1, false, false, true));
        }

        // 阶段 3: 长期未愈 (7天后) - 严重影响生活
        if (time > 20 * 60 * 20 * 7) {
            // 精神衰弱，偶尔反胃
            if (entity.getRandom().nextInt(600) == 0) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 0));
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void hemorrhoidsTick(T entity) {
        int current = entity.getHemorrhoids();
        boolean isSitting = entity.hasVehicle(); // 检查是否坐在载具上（矿车、船、椅子模组等）

        // 1. 诱因机制：久坐 & 高排便值 & 怀孕压迫
        int increaseRate = 0;

        // 如果正在坐着，必然增加进度
        if (isSitting) {
            increaseRate += 1;
        }

        // 如果便意很浓 (超过1天)，增加患病风险
        if (entity.getExcretion() > 20 * 60 * 20) {
            // 1/10 概率增加（模拟便秘压力）
            if (entity.getRandom().nextInt(10) == 0) increaseRate += 1;
        }

        // 怀孕压迫：如果是孕晚期
        if (entity.isPregnant() && entity.getPregnant() < 20 * 60 * 20 * 2) {
            if (entity.getRandom().nextInt(5) == 0) increaseRate += 1;
        }

        // 只有在有诱因时才增加，否则缓慢自然恢复（极慢）
        if (increaseRate > 0) {
            entity.setHemorrhoids(current + increaseRate);
        } else if (current > 0) {
            // 没有诱因时，非常缓慢的自愈 (每秒减1tick，几乎不可自愈，必须手术)
            if (entity.getRandom().nextInt(20) == 0) {
                entity.setHemorrhoids(current - 1);
            }
        }

        // 更新当前值用于症状判断
        current = entity.getHemorrhoids();
        if (current <= 0) return;

        // 2. 症状机制

        // 阶段 A: 轻微 (积累 > 1天)
        // 没什么明显症状，偶尔瘙痒
        if (current > 20 * 60 * 20 && current < 20 * 60 * 20 * 3) {
            if (entity.getRandom().nextInt(2400*5) == 0) { // 约10分钟一次
                entity.sendMessage(Text.of("§7感觉后面有些瘙痒..."));
            }
        }

        // 阶段 B: 严重 (积累 > 3天) - 坐姿不适
        if (current >= 20 * 60 * 20 * 3) {
            // 如果坐着，持续不适
            if (isSitting) {
                // 每60秒一次刺痛
                if (entity.getRandom().nextInt(1200) == 0) {
                    entity.damage(entity.getDamageSources().generic(), 1.0f);
                    entity.sendMessage(Text.of("§c坐得太久了，下面好痛！"));
                    // 站起来的冲动（给予反胃）
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 0));
                }
            }
        }

        // 阶段 C: 极度严重 (积累 > 5天) - 出血 & 持续痛苦
        if (current >= 20 * 60 * 20 * 5) {
            // 偶尔弄脏胖次 (出血)
            if (entity.getRandom().nextInt(3000) == 0) { // 约2.5分钟
                ItemStack legStack = entity.getEquippedStack(EquipmentSlot.LEGS);
                if (!legStack.isEmpty() && legStack.getItem() instanceof PantsuItem) {
                    // 弄脏胖次
                    legStack.set(JRComponents.Companion.getPANTSU_STATE(), JRComponents.PantsuState.SOILED);
                    entity.sendMessage(Text.of("§c糟糕，痔疮破裂出血弄脏了胖次..."));
                } else {
                    entity.sendMessage(Text.of("§c感觉后面流血了..."));
                }
                // 虚弱效果
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 30, 0));
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

    /**
     * 处理处女膜在剧烈运动下的自然破裂逻辑
     */
    static <T extends LivingEntity & Pregnant> void hymenTick(T entity) {
        if (!entity.hasHymen() || !entity.hasUterus()) return;

        // 如果是闭锁状态，通常组织非常厚韧，一般的运动不会导致破裂，必须手术
        if (entity.isImperforateHymen()) return;

        // 1. 骑乘判定 (骑马、矿车等颠簸)
        if (entity.hasVehicle()) {
            // 骑乘时极低概率破裂 (1/10000 每tic)
            if (entity.getRandom().nextInt(10000) == 0) {
                entity.ruptureHymen("剧烈颠簸");
            }
        }

        // 2. 疾跑判定 (剧烈拉伸)
        if (entity.isSprinting()) {
            // 疾跑时极低概率 (1/8000)
            if (entity.getRandom().nextInt(8000) == 0) {
                entity.ruptureHymen("剧烈运动");
            }
        }

        // 3. 跳跃/摔落判定
        if (entity.fallDistance > 5.0f) {
            // 落地受击时有概率
            if (entity.getRandom().nextInt(100) == 0) { // 落地时1%概率
                entity.ruptureHymen("高处坠落冲击");
            }
        }
    }
    /**
     * 处理处女膜闭锁导致的病理状态
     * 核心逻辑：月经期经血无法排出 -> 经血潴留 -> 腹痛、伤害、负面效果
     */
    static <T extends LivingEntity & Pregnant> void imperforateHymenTick(T entity) {
        if (!entity.isFemale() || !entity.hasUterus() || !entity.isImperforateHymen()) return;

        // 只有在月经期才会出现严重症状
        if (entity.getMenstruationCycle() == MenstruationCycle.MENSTRUATION) {

            // 1. 阻断经血排出带来的“舒适度”或清洁逻辑
            // (这里不需要写代码，只是逻辑上说明：因为闭锁，胖次不会脏，但人会废掉)

            // 2. 周期性剧痛 (每 30秒 - 1分钟)
            if (entity.getRandom().nextInt(600) == 0) {
                // 魔法伤害（无视护甲，模拟内脏疼痛）
                entity.damage(entity.getDamageSources().magic(), 2.0f);
                entity.sendMessage(Text.of("§c下腹部因经血无法排出而肿胀剧痛！"));

                // 3. 伴随效果
                // 反胃 (疼痛引起)
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 20, 0));
                // 缓慢 (痛得走不动)
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 30, 2));
                // 虚弱
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 30, 1));
            }

            // 4. 极度严重情况 (如果不治疗，可能导致持续掉血)
            // 模拟“处女膜闭锁导致的阴道积血/子宫积血”
            if (entity.getRandom().nextInt(100) == 0) {
                entity.damage(entity.getDamageSources().magic(), 0.5f);
            }
        }
    }

    /**
     * 雌转雄核心逻辑 Tick
     */
    static <T extends LivingEntity & Pregnant> void protogynyTick(T entity) {
        // 1. 全局开关检查
        if (!entity.isProtogynyEnabled()) return;

        // 2. 触发检测 (如果尚未开始)
        if (!entity.isUndergoingProtogyny()) {
            // 必须是雌性才能转雄
            if (!entity.isFemale()) return;
            // 怀孕中严禁变性
            if (entity.isPregnant()) return;

            // 检测 Scale 属性
            double currentScale = entity.getAttributeValue(EntityAttributes.GENERIC_SCALE);
            if (currentScale >= entity.getProtogynyScaleThreshold()) {
                // 触发变性！
                entity.setUndergoingProtogyny(true);
                entity.sendMessage(Text.of("§c你感觉到体内燥热难耐，似乎正在发生某种剧变..."));

                // 特殊情况处理：如果已经是双性 (Male=true, Female=true)
                // 直接快进到 3分钟 阶段，跳过单纯雌性阶段
                if (entity.isMale()) {
                    entity.setProtogynyProgress(PROTOGYNY_MALE_DEVELOP_TIME);
                } else {
                    entity.setProtogynyProgress(0);
                }
            }
        }
        // 3. 进行中逻辑
        else {
            // 安全检查：如果中途怀孕了（虽然不应该发生），强制终止变性
            if (entity.isPregnant()) {
                entity.setUndergoingProtogyny(false);
                entity.setProtogynyProgress(0);
                entity.sendMessage(Text.of("§c由于受孕，身体的重塑停止了。"));
                return;
            }

            int progress = entity.getProtogynyProgress();
            entity.setProtogynyProgress(progress + 1);

            // 伴随效果：变性消耗大量能量，给予饥饿感
            if (entity.getRandom().nextInt(100) == 0) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * 10, 0));
            }

            // --- 阶段 A: 0 ~ 3分钟 (纯雌性阶段，但在积累雄激素) ---
            if (progress < PROTOGYNY_MALE_DEVELOP_TIME) { // 0 ~ 3600 tick
                // 偶尔给予微弱力量提示 (模拟雄激素逐渐起效)
                if (entity.getRandom().nextInt(600) == 0) {
                    if(entity.getWorld() instanceof ServerWorld sw){
                        sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                entity.getX(), entity.getY() + 1.0, entity.getZ(),
                                5,
                                0.3, 0.5, 0.3,
                                0.1);
                    }
                    entity.sendMessage(Text.of("§e你感觉体内有股力量在涌动..."));
                }
            }

            // --- 阶段 B: 达到 3分钟 (开启雄性，进入间性期) ---
            if (progress == PROTOGYNY_MALE_DEVELOP_TIME) { // 3600 tick
                if (!entity.isMale()) {
                    entity.setMale(true);
                    entity.sendMessage(Text.of("§6你的身体生长出了雄性特征..."));
                    // 给予瞬间治疗，模拟激素激增
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 0));
                    // 给予力量 I，因为有了雄激素
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, PROTOGYNY_TOTAL_DURATION - progress, 0));
                    if(entity.getWorld() instanceof ServerWorld sw){
                        // 生成粒子
                        sw.spawnParticles(ParticleTypes.HEART,
                                entity.getX(), entity.getY() + 1.0, entity.getZ(),
                                20,
                                0.5, 1.0, 0.5,
                                0.2);
                    }
                }
            }

            // --- 阶段 C: 达到 5分钟 (关闭雌性，变性完成) ---
            if (progress >= PROTOGYNY_TOTAL_DURATION) { // 6000 tick
                // 1. 关闭雌性特征
                entity.setFemale(false);

                // 2. 移除相关器官
                // 变性完成后，子宫退化消失
                if (entity.hasUterus()) {
                    entity.setHasUterus(false);
                    // 处女膜随子宫结构改变而消失
                    entity.setHasHymen(false);
                    entity.setImperforateHymen(false);
                }

                // 3. 疾病清理
                entity.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getUTERINE_COLD_EFFECT()));
                entity.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getOVARIAN_CANCER_EFFECT()));

                // 4. 重置变性状态
                entity.setUndergoingProtogyny(false);
                entity.setProtogynyProgress(0);

                // 5. 最终结算与奖励
                entity.sendMessage(Text.of("§b彻底的转变完成了！你现在是雄性了。"));

                // 奖励：体型略微再增大一点
                var scaleAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
                if (scaleAttr != null) {
                    // 永久性增加 10% 体型作为“阿尔法雄性”的标志
                    // 注意：需要使用 AttributeModifier 防止重复叠加，这里简化处理直接改Base
                    scaleAttr.setBaseValue(scaleAttr.getBaseValue() * 1.05);
                }

                // 奖励：生命上限提升
                var healthAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.setBaseValue(healthAttr.getBaseValue() + 4.0); // +2心
                    entity.setHealth(entity.getMaxHealth()); // 回满血
                }

                // 奖励：获得 2分钟 力量 II
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60 * 2, 1));
            }
        }
    }

    /**
     * 激素系统主循环
     */
    static <T extends LivingEntity & Pregnant> void hormoneSlowTick(T entity) {
        // 1. 计算理论激素水平
        float targetT = 0.0f; // 睾酮
        float targetE = 0.0f; // 雌激素
        float targetP = 0.0f; // 孕酮

        // --- 男性逻辑 ---
        if (entity.isMale()) {
            if (entity.isOrchiectomy()) {
                // 切除睾丸：睾酮极低，可能略有雌激素（脂肪转化）
                targetT = 5.0f;
                targetE = 15.0f;
            } else {
                // 正常男性：睾酮高
                targetT = 100.0f;
                targetE = 5.0f;

                // 前列腺炎影响：严重时轻微降低睾酮 (痛得没性致)
                if (entity.getProstatitis() > 20 * 60 * 20 * 5) {
                    targetT *= 0.8f;
                }
            }
        }

        // --- 女性逻辑 ---
        if (entity.isFemale()) {
            MenstruationCycle cycle = entity.getMenstruationCycle();

            // 基础激素波动
            if (entity.isPregnant()) {
                // 怀孕：雌激素高，孕酮极高，无发情
                targetE = 80.0f;
                targetP = 100.0f;
                targetT = 5.0f;
            } else if (entity.getBrithControlling() > 0) {
                // 避孕药：锁定激素为平稳状态，无峰值
                targetE = 40.0f;
                targetP = 40.0f;
                targetT = 5.0f;
            } else if (entity.isPCOS()) {
                // 多囊卵巢综合征 (PCOS)：雄激素异常升高，雌激素平坦
                targetT = 35.0f; // 显著高于正常女性
                targetE = 30.0f; // 持续偏低，无排卵峰值
                targetP = 10.0f;
            } else if (!entity.hasUterus()) {
                // 子宫切除，低
                targetE = 20.0f;
                targetT = 5.0f;
            } else {
                // 正常生理周期
                switch (cycle) {
                    case MENSTRUATION: // 月经期
                        targetE = 20.0f; targetP = 5.0f; break;
                    case FOLLICLE:     // 卵泡期 (上升)
                        targetE = 60.0f; targetP = 10.0f; break;
                    case OVULATION:    // 排卵期 (峰值)
                        targetE = 100.0f; targetP = 20.0f; break;
                    case LUTEINIZATION:// 黄体期
                        targetE = 50.0f; targetP = 80.0f; break;
                    default:
                        targetE = 30.0f; break;
                }
                targetT = 10.0f; // 女性基础微量睾酮
            }
        }

        // --- 雌转雄逻辑 (Protogyny) ---
        if (entity.isUndergoingProtogyny()) {
            float progressPercent = (float) entity.getProtogynyProgress() / (float) PROTOGYNY_TOTAL_DURATION;
            // 线性插值：雌激素归零，睾酮升高
            targetE *= (1.0f - progressPercent);
            targetT = Math.max(targetT, 100.0f * progressPercent);
        }

        // --- 双性/扶她逻辑 (Male & Female) ---
        if (entity.isMale() && entity.isFemale()) {
            // 叠加两者，取较高值
            targetT = Math.max(targetT, 80.0f);
            targetE = Math.max(targetE, 80.0f);
        }

        // 2. 更新数值
        entity.setTestosterone(targetT);
        entity.setEstrogen(targetE);
        entity.setProgesterone(targetP);

        // 3. 应用属性修正 (Attribute Modifiers)
        applyHormoneModifiers(entity, targetT, targetE);

        // 4. 吸引力与INeko互动
        handleAttractionSlowTick(entity);
    }

    /**
     * 应用属性修正
     * T -> 攻击力, 血量
     * E -> 速度, 幸运
     */
    private static void applyHormoneModifiers(LivingEntity entity, float tLevel, float eLevel) {
        // --- 睾酮修正 (Testosterone) ---
        // 只有当 T > 20 (高于正常女性水平) 时才开始提供加成
        // PCOS女性 (T=35) 会获得微量加成，正常男性 (T=100) 获得满额加成
        double tBonusDamage = 0.0;
        double tBonusHealth = 0.0;

        if (tLevel > 20.0f) {
            float effectiveT = tLevel - 20.0f;
            tBonusDamage = effectiveT * 0.02; // 100T -> +1.6 攻击力
            tBonusHealth = effectiveT * 0.1;  // 100T -> +8.0 血量 (4心)
        }

        // 应用攻击力
        var damageAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.removeModifier(TESTOSTERONE_ID);
            if (tBonusDamage > 0) {
                damageAttr.addTemporaryModifier(new EntityAttributeModifier(
                        TESTOSTERONE_ID,  tBonusDamage, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }

        // 应用血量 (注意：修改最大血量时不需要每次都回血，MC会自动处理上限)
        var healthAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(TESTOSTERONE_ID);
            if (tBonusHealth > 0) {
                healthAttr.addTemporaryModifier(new EntityAttributeModifier(
                        TESTOSTERONE_ID, tBonusHealth, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }

        // --- 雌激素修正 (Estrogen) ---
        // E > 40 时提供加成 (排卵期/怀孕/高雌激素)
        double eBonusSpeed = 0.0;
        double eBonusLuck = 0.0;

        if (eLevel > 40.0f) {
            float effectiveE = eLevel - 40.0f;
            eBonusSpeed = effectiveE * 0.0005; // 100E -> +0.03 速度 (相当可观)
            eBonusLuck = effectiveE * 0.02;    // 100E -> +1.2 幸运
        }

        // 应用速度
        var speedAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(ESTROGEN_ID);
            if (eBonusSpeed > 0) {
                speedAttr.addTemporaryModifier(new EntityAttributeModifier(
                        ESTROGEN_ID,  eBonusSpeed, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }

        // 应用幸运
        var luckAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_LUCK);
        if (luckAttr != null) {
            luckAttr.removeModifier(ESTROGEN_ID);
            if (eBonusLuck > 0) {
                luckAttr.addTemporaryModifier(new EntityAttributeModifier(
                        ESTROGEN_ID, eBonusLuck, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    /**
     * 计算当前实体的总吸引力评分 (0 ~ 100+)
     */
    default float getAttractionScore() {
        float score = 0.0f;

        // 1. 激素基础分
        // 无论是极高的雄激素还是极高的雌激素，都是性吸引力的来源
        score += Math.max(getTestosterone(), getEstrogen()) * 0.5f;

        // 2. 排卵期加成 (费洛蒙爆发)
        if (getMenstruationCycle() == MenstruationCycle.OVULATION) {
            score += 30.0f;
        }

        // 3. PCOS 惩罚 (吸引力降低，但因为上面T高加了攻击力，这里做平衡)
        if (isPCOS()) {
            score -= 20.0f;
        }

        // 4. 卫生状况影响 (费洛蒙 vs 恶臭)
        // 只有轻微的尿意/便意可能是某种特殊的"费洛蒙" (癖好加成)
        // 但如果已经失禁 (SOILED/WET)，则大幅扣分
        if (this instanceof LivingEntity living) {
            if (living.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getSMEARY_EFFECT())) ||
                    living.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getAIDS_EFFECT())) ||
                    living.hasStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getSYPHILIS_EFFECT()))) {
                score -= 50.0f; // 有病或脏了，没人喜欢
            } else {
                // 轻微味道加成
                if (getUrination() > 20*60*20*0.5 && getUrination() < 20*60*20*1.5) {
                    score += 10.0f;
                }
            }
        }

        return Math.max(0, score);
    }

    /**
     * 处理吸引力逻辑：吸引周围的 INeko
     */
    private static <T extends LivingEntity & Pregnant> void handleAttractionSlowTick(T entity) {

        float myScore = entity.getAttractionScore();
        if (myScore < 40.0f) return; // 魅力太低，没人理

        // 扫描周围 16 格的生物
        World world = entity.getWorld();
        List<MobEntity> nearbyEntities = world.getEntitiesByClass(
                MobEntity.class,
                entity.getBoundingBox().expand(16.0),
                e -> e != entity && e instanceof INeko
        );

        for (MobEntity target : nearbyEntities) {
            // 只要是 Neko 就被吸引
            if (target instanceof INeko) {
                // 让 Neko看向你
                target.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entity.getEyePos());

                // 如果吸引力极高 (排卵期/高激素)，让它们靠近
                if (myScore > 80.0f) {
                    target.getNavigation().startMovingTo(entity, 0.3); // 以正常速度靠近

                    // 极低概率发出爱心 (1/10)
                    if (entity.getRandom().nextInt(10) == 0 && world instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.HEART,
                                target.getX(), target.getY() + 1.0, target.getZ(),
                                1, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }
    }

    static <T extends LivingEntity & Pregnant> void cataractTick(T entity) {
        int current = entity.getCataract();
        int increase = 0;
        World world = entity.getWorld();
        net.minecraft.util.math.BlockPos pos = entity.getBlockPos();

        // --- 1. 紫外线诱因 (光照) ---
        // 判定：白天 + 露天 + 亮度高
        if (world.isDay() && world.getLightLevel(net.minecraft.world.LightType.SKY, pos) >= 14 && world.isSkyVisible(pos)) {
            // 检查是否有头部装备 (视为墨镜/帽子)
            ItemStack headStack = entity.getEquippedStack(EquipmentSlot.HEAD);
            if (headStack.isEmpty()) {
                // 无保护，UV 伤害积累
                if (entity.getRandom().nextInt(100) == 0) increase++;
            }
        }

        // --- 2. 并发症诱因 ---
        if (entity.isPCOS()) {
            if (entity.getRandom().nextInt(300) == 0) increase++;
        }

        // --- 3. 年龄/自然老化诱因 ---
        // 极其缓慢的自然增长
        if (entity.getRandom().nextInt(1000) == 0) {
            increase++;
        }

        // --- 4. 高亮度眩光惩罚 (逻辑层) ---
        // 如果到了中期(Stage 2)，且直视阳光或高亮环境，偶尔给予反胃
        if (current > CATARACT_STAGE_2) {
            if (world.getLightLevel(pos) > 12) {
                if (entity.getRandom().nextInt(1200) == 0) { // 1分钟一次
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 5));
                }
            }
        }

        if (increase > 0) {
            entity.setCataract(current + increase);
        }
    }

    /**
     * 黄体破裂诱因检测 Tick
     * 负责检测实体的高危行为（疾跑、坠落、骑乘）并掷骰子触发破裂
     */
    static <T extends LivingEntity & Pregnant> void corpusLuteumTriggerTick(T entity) {
        if (entity.getCorpusLuteumRupture() > 0) return; // 已经破裂就不检测了
        if (!entity.isFemale() || !entity.hasUterus()) return;
        if (entity.getMenstruationCycle() != MenstruationCycle.LUTEINIZATION) return;

        // 1. 高处坠落冲击判定
        if (entity.fallDistance > 3.0f) {
            if (entity.getRandom().nextInt(150) == 0) { // 概率较高
                entity.ruptureCorpusLuteum("高处坠落冲击");
            }
        }
        // 2. 剧烈运动判定 (疾跑)
        if (entity.isSprinting()) {
            if (entity.getRandom().nextInt(8000) == 0) {
                entity.ruptureCorpusLuteum("剧烈运动");
            }
        }
        // 3. 颠簸判定 (骑乘)
        if (entity.hasVehicle()) {
            if (entity.getRandom().nextInt(5000) == 0) {
                entity.ruptureCorpusLuteum("剧烈颠簸");
            }
        }
    }

    /**
     * 黄体破裂症状演变 Tick
     * 负责处理内出血积累、休克、以及轻症的自愈逻辑
     */
    static <T extends LivingEntity & Pregnant> void corpusLuteumRuptureTick(T entity) {
        int current = entity.getCorpusLuteumRupture();
        if (current <= 0) return;

        boolean isSevere = entity.isSevereCorpusLuteumRupture();
        // 判定是否处于静养状态（睡觉、潜行，或速度极慢且没疾跑没骑乘）
        boolean isResting = entity.isSleeping() || entity.isSneaking() ||
                (!entity.isSprinting() && !entity.hasVehicle() && entity.getVelocity().lengthSquared() < 0.01);

        // 1. 出血量变化逻辑
        int delta = 1; // 默认每 tick 出血量增加 1
        if (!isSevere && isResting) {
            delta = -2; // 轻症且静养，身体自我吸收（数值下降，加速愈合）
        } else if (entity.isSprinting() || entity.hasVehicle()) {
            delta = 3;  // 不管轻重症，剧烈运动都会加速出血
        }

        current += delta;

        // 轻症自愈判定
        if (current <= 0) {
            entity.setCorpusLuteumRupture(0);
            entity.setSevereCorpusLuteumRupture(false);
            entity.sendMessage(Text.of("§a经过休息，腹部的隐痛逐渐消失了...（黄体破裂已自愈）"));
            return;
        }

        entity.setCorpusLuteumRupture(current);

        // 2. 并发症：引发流产
        // 现实中黄体主要维持早孕，黄体破裂极易导致流产
        if (entity.isPregnant() && entity.getRandom().nextInt(2000) == 0) {
            entity.sendMessage(Text.of("§c内出血与黄体受损导致了流产..."));
            entity.miscarry();
        }

        // 3. 并发症：直肠刺激征（血液流入道格拉斯窝）
        // 给玩家造成强烈的虚假便意（肛门坠胀感）
        if (entity.getRandom().nextInt(800) == 0) {
            entity.setExcretion(entity.getExcretion() + 20 * 60); // 强行增加1分钟的便意值
            entity.sendMessage(Text.of("§e腹腔积血压迫直肠，传来强烈的坠胀感..."));
        }

        // 4. 症状表现
        if (!isSevere) {
            // 【轻症症状】
            // 偶尔隐痛
            if (entity.getRandom().nextInt(1200) == 0) {
                entity.damage(entity.getDamageSources().magic(), 0.5f);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 5, 0));
            }

            // 轻症作死惩罚：积血多且剧烈活动，有概率恶化为重症
            if (current > 20 * 60 * 3 && (entity.isSprinting() || entity.fallDistance > 2.0f)) {
                if (entity.getRandom().nextInt(200) == 0) {
                    entity.setSevereCorpusLuteumRupture(true);
                    entity.sendMessage(Text.of("§c糟糕！剧烈活动导致黄体破裂口扩大，转为大出血！"));
                    entity.damage(entity.getDamageSources().generic(), 2.0f);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 0));
                }
            }
        } else {
            // 【重症症状】
            // 频繁的内出血伤害
            if (entity.getRandom().nextInt(200) == 0) {
                entity.damage(entity.getDamageSources().magic(), 1.0f);
            }

            // 阶段 A: 失血 3 分钟以上 -> 头晕目眩
            if (current > 20 * 60 * 3) {
                if (entity.getRandom().nextInt(400) == 0) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 15, 0));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20 * 5, 0));
                }
            }

            // 阶段 B: 失血 8 分钟以上 -> 休克昏迷，濒死
            if (current > 20 * 60 * 8) {
                if (entity.getRandom().nextInt(400) == 0) {
                    entity.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getFAINT_EFFECT()), 20 * 30, 0));
                    entity.sendMessage(Text.of("§c由于大量内出血，你陷入了失血性休克..."));
                }
                // 不治疗有极高致死率
                if (entity.getRandom().nextInt(600) == 0) {
                    entity.damage(entity.getDamageSources().generic(), 4.0f);
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
