package org.cneko.justarod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.cneko.justarod.effect.JREffects;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;

public interface BDSMable {
    default void setBallMouth(int time){}
    default int getBallMouth(){
        return 0;
    }
    default void updateBallMouth(){
        if (getBallMouth() > 1){
            setBallMouth(getBallMouth()-1);
        }
    }

    default void setElectricShock(int time){}
    default int getElectricShock(){
        return 0;
    }
    default void updateElectricShock(){
        if (getElectricShock() > 1){
            setElectricShock(getElectricShock()-1);
        }
    }

    default void setBundled(int time){
    }
    default int getBundled(){
        return 0;
    }
    default void updateBundled(){
        if (getBundled() > 1){
            setBundled(getBundled()-1);
        }
    }

    default void setEyePatch(int time){}
    default int getEyePatch(){
        return 0;
    }
    default void updateEyePatch(){
        if (getEyePatch() > 1){
            setEyePatch(getEyePatch()-1);
        }
    }

    default void setEarplug(int time){
    }
    default int getEarplug(){
        return 0;
    }
    default void updateEarplug(){
        if (getEarplug() > 1){
            setEarplug(getEarplug()-1);
        }
    }

    default void setHandcuffed(int time){
    }
    default int getHandcuffed(){
        return 0;
    }
    default void updateHandcuffed(){
        if (getHandcuffed() > 1){
            setHandcuffed(getHandcuffed()-1);
        }
    }

    default void setShackled(int time){
    }
    default int getShackled(){
        return 0;
    }
    default void updateShackled(){
        if (getShackled() > 1){
            setShackled(getShackled()-1);
        }
    }

    default void setNoMatingPlz(int time){
    }
    default int getNoMatingPlz(){
        return 0;
    }
    default void updateNoMatingPlz(){
        if (getNoMatingPlz() > 1){
            setNoMatingPlz(getNoMatingPlz()-1);
        }
    }


    default void writeBDSMToNbt(CompoundTag nbt){
        nbt.putInt("BallMouth", getBallMouth());
        nbt.putInt("ElectricShock", getElectricShock());
        nbt.putInt("Bundled", getBundled());
        nbt.putInt("EyePatch", getEyePatch());
        nbt.putInt("Earplug", getEarplug());
        nbt.putInt("Handcuffed", getHandcuffed());
        nbt.putInt("Shackled", getShackled());
        nbt.putInt("NoMatingPlz", getNoMatingPlz());
    }

    default void readBDSMFromNbt(CompoundTag nbt){
        setBallMouth(nbt.getInt("BallMouth"));
        setElectricShock(nbt.getInt("ElectricShock"));
        setBundled(nbt.getInt("Bundled"));
        setEyePatch(nbt.getInt("EyePatch"));
        setEarplug(nbt.getInt("Earplug"));
        setHandcuffed(nbt.getInt("Handcuffed"));
        setShackled(nbt.getInt("Shackled"));
        setNoMatingPlz(nbt.getInt("NoMatingPlz"));
    }

    static <T extends LivingEntity & BDSMable> void ballMouthTick(T ballMouthable) {
        ballMouthable.updateBallMouth();
        if (ballMouthable.getBallMouth() == 1 && ballMouthable.isShiftKeyDown()){
            ballMouthable.setBallMouth(0);
            ballMouthable.sendSystemMessage(Component.nullToEmpty("§a已摘除禁言口罩"));
        }
    }
    static <T extends LivingEntity & BDSMable> void electricShockTick(T electricShockable) {
        electricShockable.updateElectricShock();

        if (electricShockable.getElectricShock() > 0) {
            // 添加电击状态效果
            electricShockable.addEffect(
                    new MobEffectInstance(
                            BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.NEKO_EFFECT),
                            20, // 1秒刷新一次
                            0,
                            true,
                            false
                    )
            );

            // 1/200受伤
            if (electricShockable.getRandom().nextInt(200) == 0) {
                electricShockable.hurt(
                        electricShockable.level().damageSources().magic(),
                        0.1F
                );
            }

            // 播放电击音效
            if (electricShockable.getRandom().nextInt(20)==0) {
                electricShockable.level().playSound(
                        null,
                        electricShockable.getX(),
                        electricShockable.getY(),
                        electricShockable.getZ(),
                        net.minecraft.sounds.SoundEvents.REDSTONE_TORCH_BURNOUT,
                        electricShockable.getSoundSource(),
                        0.6F,
                        1.5F + electricShockable.level().random.nextFloat() * 0.4F
                );
            }

            // 生成火花粒子
            for (int i = 0; i < 3; i++) {
                double offsetX = (electricShockable.level().random.nextDouble() - 0.5) * 0.5;
                double offsetY = electricShockable.level().random.nextDouble() * 1.2;
                double offsetZ = (electricShockable.level().random.nextDouble() - 0.5) * 0.5;
                ServerLevel world = (ServerLevel) electricShockable.level();
                world.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        electricShockable.getX() + offsetX,
                        electricShockable.getY() + offsetY,
                        electricShockable.getZ() + offsetZ,
                        3,
                        0.1, 0.01, 0.1,
                        0.1
                );
            }

        }

        // 解除逻辑
        if (electricShockable.getElectricShock() == 1 && electricShockable.isShiftKeyDown()) {
            electricShockable.setElectricShock(0);
            electricShockable.sendSystemMessage(Component.nullToEmpty("§a已解除电击器"));
        }
    }

    static <T extends LivingEntity & BDSMable> void bundledTick(T bundled) {
        bundled.updateBundled();
        if (bundled.getBundled() == 1 && bundled.isShiftKeyDown()) {
            bundled.setBundled(0);
            bundled.sendSystemMessage(Component.nullToEmpty("§a已解除束缚"));
        }
        if (bundled.getBundled() > 0) {
            // 添加束缚状态效果
            bundled.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            20,
                            10,
                            true,
                            false
                    )
            );
            bundled.addEffect(
                    new MobEffectInstance(
                            MobEffects.WEAKNESS,
                            20,
                            10,
                            true,
                            false
                    )
            );
            bundled.addEffect(
                    new MobEffectInstance(
                            MobEffects.DIG_SLOWDOWN,
                            20,
                            10,
                            true,
                            false
                    )
            );
            bundled.addEffect(
                    new MobEffectInstance(
                            BuiltInRegistries.MOB_EFFECT.getHolder(JREffects.Companion.getJUMP_NERF_EFFECT()),
                            20,
                            10, // 1秒刷新一次
                            true,
                            false
                    )
            );
        }
    }

    static <T extends LivingEntity & BDSMable> void eyePatchTick(T eyePatchable) {
        eyePatchable.updateEyePatch();
        if (eyePatchable.getEyePatch() == 1 && eyePatchable.isShiftKeyDown()) {
            eyePatchable.setEyePatch(0);
            eyePatchable.sendSystemMessage(Component.nullToEmpty("§a已摘除眼罩"));
        }
        if (eyePatchable.getEyePatch() > 0) {
            // 添加眼罩状态效果
            eyePatchable.addEffect(
                    new MobEffectInstance(
                            MobEffects.BLINDNESS, 40, // 2秒刷新一次
                            0,
                            true,
                            false
                    )
            );
        }
    }

    static <T extends LivingEntity & BDSMable> void earplugTick(T earplugable) {
        earplugable.updateEarplug();
        if (earplugable.getEarplug() == 1 && earplugable.isShiftKeyDown()) {
            earplugable.setEarplug(0);
            earplugable.sendSystemMessage(Component.nullToEmpty("§a已摘除耳塞"));
        }
    }

    static <T extends LivingEntity & BDSMable> void handcuffedTick(T handcuffed) {
        handcuffed.updateHandcuffed();
        if (handcuffed.getHandcuffed() == 1 && handcuffed.isShiftKeyDown()) {
            handcuffed.setHandcuffed(0);
            handcuffed.sendSystemMessage(Component.nullToEmpty("§a已解除手铐"));
        }
    }

    static <T extends LivingEntity & BDSMable> void shackledTick(T shackled) {
        shackled.updateShackled();
        if (shackled.getShackled() == 1 && shackled.isShiftKeyDown()) {
            shackled.setShackled(0);
            shackled.sendSystemMessage(Component.nullToEmpty("§a已解除脚镣"));
        }
        if (shackled.getShackled()>0){
            shackled.addEffect(
                    new MobEffectInstance(
                            BuiltInRegistries.MOB_EFFECT.getHolder(JREffects.Companion.getJUMP_NERF_EFFECT()),
                            20,
                            10, // 1秒刷新一次
                            true,
                            false
                    )
            );
            shackled.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            20,
                            10,
                            true,
                            false
                    )
            );
        }
    }

    static <T extends LivingEntity & BDSMable> void noMatingPlzTick(T noMatingPlzAble) {
        noMatingPlzAble.updateNoMatingPlz();
        if (noMatingPlzAble.getNoMatingPlz() == 1 && noMatingPlzAble.isShiftKeyDown()) {
            noMatingPlzAble.setNoMatingPlz(0);
        }

    }

}
