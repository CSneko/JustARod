package org.cneko.justarod.entity.ai

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.effect.StatusEffects
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
        if (target != null && target!!.entity.hasStatusEffect(StatusEffects.WEAKNESS)){
            neko.nekoMateGoal.setTarget(target)
        }else if (neko.getSexualDesire()>50){
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