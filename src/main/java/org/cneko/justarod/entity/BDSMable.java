package org.cneko.justarod.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

    default void writeBDSMToNbt(NbtCompound nbt){
        nbt.putInt("BallMouth", getBallMouth());
        nbt.putInt("ElectricShock", getElectricShock());
        nbt.putInt("Bundled", getBundled());
        nbt.putInt("EyePatch", getEyePatch());
        nbt.putInt("Earplug", getEarplug());
    }

    default void readBDSMFromNbt(NbtCompound nbt){
        setBallMouth(nbt.getInt("BallMouth"));
        setElectricShock(nbt.getInt("ElectricShock"));
        setBundled(nbt.getInt("Bundled"));
        setEyePatch(nbt.getInt("EyePatch"));
        setEarplug(nbt.getInt("Earplug"));
    }

    static <T extends LivingEntity & BDSMable> void ballMouthTick(T ballMouthable) {
        ballMouthable.updateBallMouth();
        if (ballMouthable.getBallMouth() == 1 && ballMouthable.isSneaking()){
            ballMouthable.setBallMouth(0);
            ballMouthable.sendMessage(Text.of("§a已摘除禁言口罩"));
        }
    }
    static <T extends LivingEntity & BDSMable> void electricShockTick(T electricShockable) {
        electricShockable.updateElectricShock();

        if (electricShockable.getElectricShock() > 0) {
            // 添加电击状态效果
            electricShockable.addStatusEffect(
                    new StatusEffectInstance(
                            Registries.STATUS_EFFECT.getEntry(ToNekoEffects.NEKO_EFFECT),
                            20, // 1秒刷新一次
                            0,
                            true,
                            false
                    )
            );

            // 1/200受伤
            if (electricShockable.getRandom().nextInt(200) == 0) {
                electricShockable.damage(
                        electricShockable.getWorld().getDamageSources().magic(),
                        0.1F
                );
            }

            // 播放电击音效
            if (electricShockable.getRandom().nextInt(20)==0) {
                electricShockable.getWorld().playSound(
                        null,
                        electricShockable.getX(),
                        electricShockable.getY(),
                        electricShockable.getZ(),
                        net.minecraft.sound.SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                        electricShockable.getSoundCategory(),
                        0.6F,
                        1.5F + electricShockable.getWorld().random.nextFloat() * 0.4F
                );
            }

            // 生成火花粒子
            for (int i = 0; i < 3; i++) {
                double offsetX = (electricShockable.getWorld().random.nextDouble() - 0.5) * 0.5;
                double offsetY = electricShockable.getWorld().random.nextDouble() * 1.2;
                double offsetZ = (electricShockable.getWorld().random.nextDouble() - 0.5) * 0.5;
                ServerWorld world = (ServerWorld) electricShockable.getWorld();
                world.spawnParticles(
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
        if (electricShockable.getElectricShock() == 1 && electricShockable.isSneaking()) {
            electricShockable.setElectricShock(0);
            electricShockable.sendMessage(Text.of("§a已解除电击器"));
        }
    }

    static <T extends LivingEntity & BDSMable> void bundledTick(T bundled) {
        bundled.updateBundled();
        if (bundled.getBundled() == 1 && bundled.isSneaking()) {
            bundled.setBundled(0);
            bundled.sendMessage(Text.of("§a已解除束缚"));
        }
        if (bundled.getBundled() > 0) {
            // 添加束缚状态效果
            bundled.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            20,
                            10,
                            true,
                            false
                    )
            );
            bundled.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.WEAKNESS,
                            20,
                            10,
                            true,
                            false
                    )
            );
            bundled.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.MINING_FATIGUE,
                            20,
                            10,
                            true,
                            false
                    )
            );
            bundled.addStatusEffect(
                    new StatusEffectInstance(
                            Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getJUMP_NERF_EFFECT()),
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
        if (eyePatchable.getEyePatch() == 1 && eyePatchable.isSneaking()) {
            eyePatchable.setEyePatch(0);
            eyePatchable.sendMessage(Text.of("§a已摘除眼罩"));
        }
        if (eyePatchable.getEyePatch() > 0) {
            // 添加眼罩状态效果
            eyePatchable.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.BLINDNESS, 40, // 2秒刷新一次
                            0,
                            true,
                            false
                    )
            );
        }
    }

    static <T extends LivingEntity & BDSMable> void earplugTick(T earplugable) {
        earplugable.updateEarplug();
        if (earplugable.getEarplug() == 1 && earplugable.isSneaking()) {
            earplugable.setEarplug(0);
            earplugable.sendMessage(Text.of("§a已摘除耳塞"));
        }
    }

}
