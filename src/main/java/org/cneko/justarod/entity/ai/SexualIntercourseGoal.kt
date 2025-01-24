package org.cneko.justarod.entity.ai

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Box
import net.minecraft.world.World
import org.cneko.justarod.entity.SeeeeexNekoEntity
import org.cneko.justarod.getNekoInRange
import org.cneko.toneko.common.mod.entities.INeko

class SexualIntercourseGoal(val neko: SeeeeexNekoEntity) : Goal (){
    var target: INeko? = null
    override fun canStart(): Boolean {
        return true
    }

    override fun tick() {
        super.tick()
        if (target != null && neko.canMate(target!!)){
            val world = neko.world
            if (neko.nekoMateGoal.target==null && world is ServerWorld){
                // 性欲下降点
                neko.tryMating(world,target!!)
                neko.decreaseSexualDesire(20)
            }
        }else if (neko.getSexualDesire()>=50){
            val target = neko.world.getNekoInRange(neko,neko.getSexualDesire()* 0.1f)
            if (target.isNotEmpty()){
                this.target = target[0]
            }
        }
        if (target?.entity?.hasStatusEffect(StatusEffects.WEAKNESS) == true){
            target = null
        }

    }


}