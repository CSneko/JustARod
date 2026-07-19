package org.cneko.justarod.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.cneko.justarod.entity.Pregnant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"AddedMixinMembersNamePattern", "DataFlowIssue"})
@Mixin(WitherBoss.class)
public class WitherEntityMixin implements Pregnant {

    // ==================== 性别 ====================
    @Unique
    private boolean male = false;
    @Unique
    private boolean female = false;

    // ==================== 怀孕 ====================
    @Unique
    private int pregnant = 0;
    @Unique
    private EntityType<?> childrenType = EntityType.WITHER;
    @Unique
    private int babyCount = 0;
    @Unique
    private boolean ectopicPregnancy = false;
    @Unique
    private boolean hydatidiformMole = false;

    // ==================== 避孕/绝育 ====================
    @Unique
    private boolean sterilization = false;
    @Unique
    private int brithControlling = 0;

    // ==================== 子宫 ====================
    @Unique
    private boolean hasUterus = false;
    @Unique
    private boolean isPCOS = false;
    @Unique
    private int uterineCold = 0;

    // ==================== 月经 ====================
    @Unique
    private int menstruationComfort = 0;
    @Unique
    private int ovarianClock = 0;
    @Unique
    private float uterineThickness = 0.0f;
    @Unique
    private Pregnant.MenstruationCycle currentCycle = Pregnant.MenstruationCycle.NONE;

    // ==================== 性病 ====================
    @Unique
    private int aids = 0;
    @Unique
    private boolean immune2Aids = false;
    @Unique
    private int hpv = 0;
    @Unique
    private boolean immune2HPV = false;
    @Unique
    private int syphilis = 0;
    @Unique
    private int urethritis = 0;

    // ==================== 癌症 ====================
    @Unique
    private int ovarianCancer = 0;
    @Unique
    private int breastCancer = 0;

    // ==================== 排泄 ====================
    @Unique
    private int excretion = 0;
    @Unique
    private int urination = 0;
    @Unique
    private int urinaryIncontinence = 0;

    // ==================== 男性器官 ====================
    @Unique
    private boolean orchiectomy = false;
    @Unique
    private int prostatitis = 0;

    // ==================== 其他病症 ====================
    @Unique
    private int hemorrhoids = 0;
    @Unique
    private int cataract = 0;
    @Unique
    private int paronychia = 0;
    @Unique
    private boolean nailRemoved = false;
    @Unique
    private int nailRegrowTime = 0;
    @Unique
    private boolean amputated = false;

    // ==================== 处女膜 ====================
    @Unique
    private boolean hasHymen = false;
    @Unique
    private boolean imperforateHymen = false;

    // ==================== 孤雌生殖 ====================
    @Unique
    private float parthenogenesisVariance = 0.0f;

    // ==================== 雌转雄 ====================
    @Unique
    private boolean protogynyEnabled = false;
    @Unique
    private boolean isUndergoingProtogyny = false;
    @Unique
    private int protogynyProgress = 0;

    // ==================== 黄体破裂 ====================
    @Unique
    private int corpusLuteumRupture = 0;
    @Unique
    private boolean severeCorpusLuteumRupture = false;

    // ==================== 泌乳 ====================
    @Unique
    private float milk = 0.0f;
    @Unique
    private int mastitis = 0;
    @Unique
    private int lactationStimulation = 0;

    // ==================== 激素 ====================
    @Unique
    private float endoE2 = 0.0f;
    @Unique
    private float endoP = 0.0f;
    @Unique
    private float endoT = 0.0f;
    @Unique
    private float exoE2 = 0.0f;
    @Unique
    private float exoP = 0.0f;
    @Unique
    private float exoT = 0.0f;
    @Unique
    private float exoBlocker = 0.0f;
    @Unique
    private int hrtMtfProgress = 0;
    @Unique
    private int hrtFtmProgress = 0;
    @Unique
    private int vaginalAtrophy = 0;

    // ==================== Gender ====================
    @Override
    public boolean isMale() {
        return male;
    }

    @Override
    public void setMale(boolean male) {
        this.male = male;
    }

    @Override
    public boolean isFemale() {
        return female;
    }

    @Override
    public void setFemale(boolean female) {
        this.female = female;
    }

    // ==================== Pregnancy ====================
    @Override
    public int getPregnant() {
        return pregnant;
    }

    @Override
    public void setPregnant(int pregnant) {
        this.pregnant = pregnant;
    }

    @Override
    public EntityType<?> getChildrenType() {
        return childrenType;
    }

    @Override
    public void setChildrenType(EntityType<?> childrenType) {
        this.childrenType = childrenType;
    }

    @Override
    public int getBabyCount() {
        return babyCount;
    }

    @Override
    public void setBabyCount(int babyCount) {
        this.babyCount = babyCount;
    }

    @Override
    public boolean isEctopicPregnancy() {
        return ectopicPregnancy;
    }

    @Override
    public void setEctopicPregnancy(boolean ectopicPregnancy) {
        this.ectopicPregnancy = ectopicPregnancy;
    }

    @Override
    public boolean isHydatidiformMole() {
        return hydatidiformMole;
    }

    @Override
    public void setHydatidiformMole(boolean hydatidiformMole) {
        this.hydatidiformMole = hydatidiformMole;
    }

    // ==================== Sterilization / Birth Control ====================
    @Override
    public boolean isSterilization() {
        return sterilization;
    }

    @Override
    public void setSterilization(boolean sterilization) {
        this.sterilization = sterilization;
    }

    @Override
    public int getBrithControlling() {
        return brithControlling;
    }

    @Override
    public void setBrithControlling(int brithControlling) {
        this.brithControlling = brithControlling;
    }

    // ==================== Uterus ====================
    @Override
    public boolean hasUterus() {
        return hasUterus;
    }

    @Override
    public void setHasUterus(boolean hasUterus) {
        this.hasUterus = hasUterus;
    }

    @Override
    public boolean isPCOS() {
        return isPCOS;
    }

    @Override
    public void setPCOS(boolean PCOS) {
        isPCOS = PCOS;
    }

    @Override
    public int getUterineCold() {
        return uterineCold;
    }

    @Override
    public void setUterineCold(int uterineCold) {
        this.uterineCold = uterineCold;
    }

    // ==================== Menstruation ====================
    @Override
    public int getMenstruationComfort() {
        return menstruationComfort;
    }

    @Override
    public void setMenstruationComfort(int menstruationComfort) {
        this.menstruationComfort = menstruationComfort;
    }

    @Override
    public int getOvarianClock() {
        return ovarianClock;
    }

    @Override
    public void setOvarianClock(int ovarianClock) {
        this.ovarianClock = ovarianClock;
    }

    @Override
    public float getUterineThickness() {
        return uterineThickness;
    }

    @Override
    public void setUterineThickness(float uterineThickness) {
        this.uterineThickness = uterineThickness;
    }

    @Override
    public Pregnant.MenstruationCycle getCurrentCycle() {
        return currentCycle;
    }

    @Override
    public void setCurrentCycle(Pregnant.MenstruationCycle currentCycle) {
        this.currentCycle = currentCycle;
    }

    // ==================== STDs ====================
    @Override
    public int getAids() {
        return aids;
    }

    @Override
    public void setAids(int aids) {
        this.aids = aids;
    }

    @Override
    public boolean isImmune2Aids() {
        return immune2Aids;
    }

    @Override
    public void setImmune2Aids(boolean immune2Aids) {
        this.immune2Aids = immune2Aids;
    }

    @Override
    public int getHPV() {
        return hpv;
    }

    @Override
    public void setHPV(int hpv) {
        this.hpv = hpv;
    }

    @Override
    public boolean isImmune2HPV() {
        return immune2HPV;
    }

    @Override
    public void setImmune2HPV(boolean immune2HPV) {
        this.immune2HPV = immune2HPV;
    }

    @Override
    public int getSyphilis() {
        return syphilis;
    }

    @Override
    public void setSyphilis(int syphilis) {
        this.syphilis = syphilis;
    }

    @Override
    public int getUrethritis() {
        return urethritis;
    }

    @Override
    public void setUrethritis(int urethritis) {
        this.urethritis = urethritis;
    }

    // ==================== Cancer ====================
    @Override
    public int getOvarianCancer() {
        return ovarianCancer;
    }

    @Override
    public void setOvarianCancer(int ovarianCancer) {
        this.ovarianCancer = ovarianCancer;
    }

    @Override
    public int getBreastCancer() {
        return breastCancer;
    }

    @Override
    public void setBreastCancer(int breastCancer) {
        this.breastCancer = breastCancer;
    }

    // ==================== Excretion ====================
    @Override
    public int getExcretion() {
        return excretion;
    }

    @Override
    public void setExcretion(int excretion) {
        this.excretion = excretion;
    }

    @Override
    public int getUrination() {
        return urination;
    }

    @Override
    public void setUrination(int urination) {
        this.urination = urination;
    }

    @Override
    public int getUrinaryIncontinence() {
        return urinaryIncontinence;
    }

    @Override
    public void setUrinaryIncontinence(int urinaryIncontinence) {
        this.urinaryIncontinence = urinaryIncontinence;
    }

    // ==================== Male Organs ====================
    @Override
    public boolean isOrchiectomy() {
        return orchiectomy;
    }

    @Override
    public void setOrchiectomy(boolean orchiectomy) {
        this.orchiectomy = orchiectomy;
    }

    @Override
    public int getProstatitis() {
        return prostatitis;
    }

    @Override
    public void setProstatitis(int prostatitis) {
        this.prostatitis = prostatitis;
    }

    // ==================== Other Conditions ====================
    @Override
    public int getHemorrhoids() {
        return hemorrhoids;
    }

    @Override
    public void setHemorrhoids(int hemorrhoids) {
        this.hemorrhoids = hemorrhoids;
    }

    @Override
    public int getCataract() {
        return cataract;
    }

    @Override
    public void setCataract(int cataract) {
        this.cataract = cataract;
    }

    @Override
    public int getParonychia() {
        return paronychia;
    }

    @Override
    public void setParonychia(int paronychia) {
        this.paronychia = paronychia;
    }

    @Override
    public boolean isNailRemoved() {
        return nailRemoved;
    }

    @Override
    public void setNailRemoved(boolean nailRemoved) {
        this.nailRemoved = nailRemoved;
    }

    @Override
    public int getNailRegrowTime() {
        return nailRegrowTime;
    }

    @Override
    public void setNailRegrowTime(int nailRegrowTime) {
        this.nailRegrowTime = nailRegrowTime;
    }

    @Override
    public boolean isAmputated() {
        return amputated;
    }

    @Override
    public void setAmputated(boolean amputated) {
        this.amputated = amputated;
    }

    // ==================== Hymen ====================
    @Override
    public boolean hasHymen() {
        return hasHymen;
    }

    @Override
    public void setHasHymen(boolean hasHymen) {
        this.hasHymen = hasHymen;
    }

    @Override
    public boolean isImperforateHymen() {
        return imperforateHymen;
    }

    @Override
    public void setImperforateHymen(boolean imperforateHymen) {
        this.imperforateHymen = imperforateHymen;
    }

    // ==================== Parthenogenesis ====================
    @Override
    public float getParthenogenesisVariance() {
        return parthenogenesisVariance;
    }

    @Override
    public void setParthenogenesisVariance(float parthenogenesisVariance) {
        this.parthenogenesisVariance = parthenogenesisVariance;
    }

    // ==================== Protogyny ====================
    @Override
    public boolean isProtogynyEnabled() {
        return protogynyEnabled;
    }

    @Override
    public void setProtogynyEnabled(boolean protogynyEnabled) {
        this.protogynyEnabled = protogynyEnabled;
    }

    @Override
    public boolean isUndergoingProtogyny() {
        return isUndergoingProtogyny;
    }

    @Override
    public void setUndergoingProtogyny(boolean isUndergoingProtogyny) {
        this.isUndergoingProtogyny = isUndergoingProtogyny;
    }

    @Override
    public int getProtogynyProgress() {
        return protogynyProgress;
    }

    @Override
    public void setProtogynyProgress(int protogynyProgress) {
        this.protogynyProgress = protogynyProgress;
    }

    // ==================== Corpus Luteum Rupture ====================
    @Override
    public int getCorpusLuteumRupture() {
        return corpusLuteumRupture;
    }

    @Override
    public void setCorpusLuteumRupture(int corpusLuteumRupture) {
        this.corpusLuteumRupture = corpusLuteumRupture;
    }

    @Override
    public boolean isSevereCorpusLuteumRupture() {
        return severeCorpusLuteumRupture;
    }

    @Override
    public void setSevereCorpusLuteumRupture(boolean severeCorpusLuteumRupture) {
        this.severeCorpusLuteumRupture = severeCorpusLuteumRupture;
    }

    // ==================== Lactation ====================
    @Override
    public float getMilk() {
        return milk;
    }

    @Override
    public void setMilk(float milk) {
        this.milk = milk;
    }

    @Override
    public int getMastitis() {
        return mastitis;
    }

    @Override
    public void setMastitis(int mastitis) {
        this.mastitis = mastitis;
    }

    @Override
    public int getLactationStimulation() {
        return lactationStimulation;
    }

    @Override
    public void setLactationStimulation(int lactationStimulation) {
        this.lactationStimulation = lactationStimulation;
    }

    // ==================== Hormones ====================
    @Override
    public float getEndoE2() {
        return endoE2;
    }

    @Override
    public void setEndoE2(float endoE2) {
        this.endoE2 = endoE2;
    }

    @Override
    public float getEndoP() {
        return endoP;
    }

    @Override
    public void setEndoP(float endoP) {
        this.endoP = endoP;
    }

    @Override
    public float getEndoT() {
        return endoT;
    }

    @Override
    public void setEndoT(float endoT) {
        this.endoT = endoT;
    }

    @Override
    public float getExoE2() {
        return exoE2;
    }

    @Override
    public void setExoE2(float exoE2) {
        this.exoE2 = exoE2;
    }

    @Override
    public float getExoP() {
        return exoP;
    }

    @Override
    public void setExoP(float exoP) {
        this.exoP = exoP;
    }

    @Override
    public float getExoT() {
        return exoT;
    }

    @Override
    public void setExoT(float exoT) {
        this.exoT = exoT;
    }

    @Override
    public float getExoBlocker() {
        return exoBlocker;
    }

    @Override
    public void setExoBlocker(float exoBlocker) {
        this.exoBlocker = exoBlocker;
    }

    @Override
    public int getHrtMtfProgress() {
        return hrtMtfProgress;
    }

    @Override
    public void setHrtMtfProgress(int hrtMtfProgress) {
        this.hrtMtfProgress = hrtMtfProgress;
    }

    @Override
    public int getHrtFtmProgress() {
        return hrtFtmProgress;
    }

    @Override
    public void setHrtFtmProgress(int hrtFtmProgress) {
        this.hrtFtmProgress = hrtFtmProgress;
    }

    @Override
    public int getVaginalAtrophy() {
        return vaginalAtrophy;
    }

    @Override
    public void setVaginalAtrophy(int vaginalAtrophy) {
        this.vaginalAtrophy = vaginalAtrophy;
    }

    // ==================== createBaby ====================
    @Override
    public Entity createBaby() {
        WitherBoss self = (WitherBoss) (Object) this;
        Entity baby = getChildrenType().create(self.level());
        if (baby instanceof Mob mob) {
            mob.setBaby(true);
        }
        if (baby != null) {
            baby.setPosRaw(self.getX(), self.getY(), self.getZ());
        }
        return baby;
    }

    // ==================== Tick ====================
    @Inject(method = "mobTick", at = @At("HEAD"))
    public void mobTick(CallbackInfo ci) {
        WitherBoss self = (WitherBoss) (Object) this;
        Pregnant.pregnantTick((LivingEntity & Pregnant) self);
        Pregnant.aidsTick((LivingEntity & Pregnant) self);
        Pregnant.HPVTick((LivingEntity & Pregnant) self);
        Pregnant.ovarianCancerTick((LivingEntity & Pregnant) self);
        Pregnant.breastCancerTick((LivingEntity & Pregnant) self);
        Pregnant.syphilisTick((LivingEntity & Pregnant) self);
    }

    // ==================== NBT Persistence ====================
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        this.readPregnantFromNbt(nbt);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void addAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        this.writePregnantToNbt(nbt);
    }
}
