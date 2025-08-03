package org.cneko.justarod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.RavennEntity;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@SuppressWarnings({"AddedMixinMembersNamePattern", "DataFlowIssue"})
@Mixin(RavennEntity.class)
public class RavennEntityMixin implements Pregnant {
    @Unique
    private int pregnant = 0;
    @Unique
    private EntityType<? > childrenType = ToNekoEntities.RAVENN_ENTITY;
    @Unique
    private int menstruation = 0;
    @Unique
    private int menstruationComfort = 0;
    @Unique
    private boolean sterilization = false;
    @Unique
    private boolean ectopicPregnancy = false;
    @Unique
    private int aids = 0;
    @Unique
    private boolean hydatidiformMole = false;
    @Unique
    private int babyCount = 0;
    @Unique
    private int hpv = 0;
    @Unique
    private boolean immune2HPV = false;
    @Unique
    private boolean isHysterectomy = false;
    @Unique
    private boolean isPCOS = false;
    @Unique
    private int brithControlling = 0;
    @Unique
    private int ovarianCancer = 0;

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
    public int getMenstruation() {
        return menstruation;
    }

    @Override
    public void setMenstruation(int menstruation) {
        this.menstruation = menstruation;
    }

    @Override
    public void setMenstruationComfort(int time) {
        this.menstruationComfort = time;
    }

    @Override
    public int getMenstruationComfort() {
        return menstruationComfort;
    }

    @Override
    public boolean isSterilization() {
        return sterilization;
    }

    @Override
    public void setSterilization(boolean sterilization) {
        this.sterilization = sterilization;
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
    public int getAids() {
        return aids;
    }

    @Override
    public void setAids(int aids) {
        this.aids = aids;
    }

    @Override
    public boolean isHydatidiformMole() {
        return hydatidiformMole;
    }

    @Override
    public void setHydatidiformMole(boolean hydatidiformMole) {
        this.hydatidiformMole = hydatidiformMole;
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
    public void setHPV(int time) {
        this.hpv = time;
    }

    @Override
    public int getHPV() {
        return hpv;
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
    public boolean isHysterectomy() {
        return isHysterectomy;
    }

    @Override
    public void setHysterectomy(boolean hysterectomy) {
        isHysterectomy = hysterectomy;
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
    public int getBrithControlling() {
        return brithControlling;
    }

    @Override
    public void setBrithControlling(int brithControlling) {
        this.brithControlling = brithControlling;
    }

    @Override
    public int getOvarianCancer() {
        return ovarianCancer;
    }

    @Override
    public void setOvarianCancer(int ovarianCancer) {
        this.ovarianCancer = ovarianCancer;
    }

    @Override
    public Entity createBaby() {
        RavennEntity ravenn = (RavennEntity) (Object) this;
        var baby = (Entity) getChildrenType().create(ravenn.getWorld());
        if (baby instanceof MobEntity mob) {
            mob.setBaby(true);
            mob.age = -48000;
            if (baby instanceof INeko neko) {
                neko.addOwner(ravenn.getUuid(), new INeko.Owner(new ArrayList<>(), 0));
            }
        }
        baby.setPos(ravenn.getX(), ravenn.getY(), ravenn.getZ());
        return baby;
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        RavennEntity ravenn = (RavennEntity) (Object) this;
        Pregnant.pregnantTick((LivingEntity & Pregnant)ravenn);
        Pregnant.menstruationTick((LivingEntity & Pregnant)ravenn);
        Pregnant.aidsTick((LivingEntity & Pregnant)ravenn);
        Pregnant.HPVTick((LivingEntity & Pregnant)ravenn);
        Pregnant.ovarianCancerTick((LivingEntity & Pregnant)ravenn);
    }

}
