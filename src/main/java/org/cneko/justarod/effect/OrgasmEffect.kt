package org.cneko.justarod.effect

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.misc.Messaging
import org.cneko.toneko.common.mod.util.TextUtil

/*
在高潮后会疯狂喘气，而且会沉浸在其中，脑子很难正常思考，也很难正常说话，有点晕乎乎的感觉，只想躺在床上
而且呢，还会不自觉的发出娇喘，而且会感觉很热，但总体来说还是很舒服的（不信你可以试试）
还有就是，在门口抽插其实会比在里面更有感觉哦
 */
class OrgasmEffect : StatusEffect(StatusEffectCategory.BENEFICIAL, 0xe9b8b3) {

    companion object{
        val screamTexts = listOf(
            "♡要...要去了",
            "好...好爽喵...",
            "♡哈啊~~",
            "啊... 好过瘾♡",
            "雅蠛蝶...",
            "恩啊啊♡",
            "好多...水...",
            "喷...喷出来呢...",
            "嗯啊♡",
            "好舒服...",
            "为什么... 会变成这样呢...",
            "好满足♡"
            // 救命我写不下去了♡
        )
    }
    // 每tick都会调用一次，直到返回false
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }

    // 这个方法在应用药水效果时的每个tick会被调用。
    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int): Boolean {
        val world = entity.world
        val random: Random = world.random
        // 添加爱心效果
        world.addParticle(
            ParticleTypes.HEART,
            entity.x + (random.nextDouble() * 2 - 1),  // 确保x方向正负概率相等
            entity.y + random.nextDouble() * 2 + 2,  // 保持y方向逻辑
            entity.z + (random.nextDouble() * 2 - 1),  // 确保z方向正负概率相等
            0.0,
            amplifier + 1.5,
            0.0
        )

        if (random.nextBoolean()) {
            // 随机移动玩家的位置，确保正负方向概率相等
            val x: Double = (random.nextDouble() * 2 - 1) * (amplifier + 1) * 2.5
            val z: Double = (random.nextDouble() * 2 - 1) * (amplifier + 1) * 2.5
            entity.move(MovementType.SHULKER_BOX, Vec3d(x * 0.001, amplifier * 0.0003, z * 0.001))
        }

        // 添加水滴效果
        if (random.nextInt(5) == 0) {
            world.addParticle(
                ParticleTypes.RAIN,
                entity.x,
                entity.y,
                entity.z,
                0.0,
                amplifier * 0.003,
                0.0
            )
        }

        if (entity is ServerPlayerEntity && entity is INeko){
            // 1/1000的概率发送淫叫
            if (random.nextInt(1000) == 0) {
                Messaging.modifyAndSendMessageToAll(entity,screamTexts[random.nextInt(screamTexts.size)])
            }
        }
        return super.applyUpdateEffect(entity, amplifier)
    }
}