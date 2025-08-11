package org.cneko.justarod.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public interface BallMouthable {
    default void setBallMouth(int time){}
    default int getBallMouth(){
        return 0;
    }
    default void updateBallMouth(){
        if (getBallMouth() > 1){
            setBallMouth(getBallMouth()-1);
        }
    }

    default void writeBallMouthToNbt(NbtCompound nbt){
        nbt.putInt("BallMouth", getBallMouth());
    }

    default void readBallMouthFromNbt(NbtCompound nbt){
        setBallMouth(nbt.getInt("BallMouth"));
    }

    static <T extends LivingEntity &BallMouthable> void ballMouthTick(T ballMouthable) {
        ballMouthable.updateBallMouth();
        if (ballMouthable.getBallMouth() == 1 && ballMouthable.isSneaking()){
            ballMouthable.setBallMouth(0);
            ballMouthable.sendMessage(Text.of("§a已摘除禁言口罩"));
        }
    }
}
