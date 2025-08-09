package org.cneko.justarod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.*;
import org.cneko.justarod.packet.JRSyncPayload;
import org.cneko.toneko.common.mod.entities.INeko;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

import static org.cneko.justarod.JRAttributes.Companion;

@SuppressWarnings({"AddedMixinMembersNamePattern", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements Powerable, Pregnant {

    @Unique
    private double power = 0;

    @Unique
    private boolean male = false;
    @Unique
    private boolean female = true;
    @Unique
    private int pregnant = 0;
    @Unique
    private short slowTick = 10;
    @Unique
    private EntityType<? > childrenType = null;
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
    @Unique
    private int breastCancer = 0;
    @Unique
    private int syphilis = 0;

    @Override
    public double getPower() {
        return power;
    }

    @Override
    public void setPower(double power) {
        this.power = power;
    }

    @Override
    public boolean canPowerUp() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        return player.getHungerManager().getFoodLevel() >= 3;
    }

    @Override
    public boolean isFemale() {
        return female;
    }

    @Override
    public void setFemale(boolean female) {
        this.female = female;
    }

    @Override
    public boolean isMale() {
        return male;
    }

    @Override
    public void setMale(boolean male) {
        this.male = male;
    }

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
    public void setOvarianCancer(int ovarianCancer) {
        this.ovarianCancer = ovarianCancer;
    }

    @Override
    public int getOvarianCancer() {
        return ovarianCancer;
    }

    @Override
    public int getBreastCancer() {
        return breastCancer;
    }

    @Override
    public void setBreastCancer(int breastCancer) {
        this.breastCancer = breastCancer;
    }

    @Override
    public void setSyphilis(int syphilis) {
        this.syphilis = syphilis;
    }

    @Override
    public int getSyphilis() {
        return syphilis;
    }

    @Override
    public Entity createBaby() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        var baby = (Entity) getChildrenType().create(player.getWorld());
        if (baby instanceof MobEntity mob) {
            mob.setBaby(true);
            mob.age = -48000;
            if (baby instanceof INeko neko) {
                neko.addOwner(player.getUuid(), new INeko.Owner(new ArrayList<>(), 0));
            }
        }
        baby.setPos(player.getX(), player.getY(), player.getZ());
        return baby;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        power = this.readPowerFromNbt(nbt);
        this.readPregnantFromNbt(nbt);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        this.writePowerToNbt(nbt);
        this.writePregnantToNbt(nbt);
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        Powerable.tickPower(player);
        if (slowTick++ >= 10){
            if (player instanceof ServerPlayerEntity sp) {
                // 同步power
                ServerPlayNetworking.send(sp, new JRSyncPayload(getPower(),isFemale(),isMale(),getPregnant(),getSyphilis()));
            }
        }
        if (player.getWorld() instanceof ServerWorld) {
            Pregnant.pregnantTick(player);
            Pregnant.menstruationTick(player);
            Pregnant.aidsTick(player);
            Pregnant.HPVTick(player);
            Pregnant.ovarianCancerTick(player);
            Pregnant.breastCancerTick(player);
            Pregnant.syphilisTick(player);
        }
    }

    @Inject(method = "eatFood",at = @At("HEAD"))
    public void eatFood(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.isOf(Items.MILK_BUCKET)){
            // 如果有HPV且在3天内
            if (this.getHPV() > 0 && this.getHPV() < 20*60*20*3) {
                this.setHPV(0);
                // 移除HPV效果
                ((PlayerEntity)(Object)this).removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getHPV_EFFECT()));
            }
        }
        if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)){
            // 如果有HPV且在6天内
            if (this.getHPV() > 0 && this.getHPV() < 20*60*20*6) {
                this.setHPV(0);
                ((PlayerEntity)(Object)this).removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getHPV_EFFECT()));
            }
        }
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.getReturnValue()
                .add(Companion.getPLAYER_LUBRICATING())
                .add(Companion.getGENERIC_MAX_POWER());
    }

}
