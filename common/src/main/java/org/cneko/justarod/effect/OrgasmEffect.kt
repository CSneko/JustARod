package org.cneko.justarod.effect

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import net.minecraft.util.RandomSource
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.misc.Messaging
import org.cneko.toneko.common.mod.util.TextUtil

/*
在高潮后会疯狂喘气，而且会沉浸在其中，脑子很难正常思考，也很难正常说话，有点晕乎乎的感觉，只想躺在床上
而且呢，还会不自觉的发出娇喘，而且会感觉很热，但总体来说还是很舒服的（不信你可以试试）
还有就是，在门口抽插其实会比在里面更有感觉哦
 */
class OrgasmEffect : MobEffect(MobEffectCategory.BENEFICIAL, 0xe9b8b3) {

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
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    // 这个方法在应用药水效果时的每个tick会被调用。
    override fun applyEffectTick(world: net.minecraft.server.level.ServerLevel, entity: LivingEntity, amplifier: Int): Boolean {
        val random: RandomSource = world.random
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

        // 发抖只由服务端做（效果 tick 客户端也会跑，如果客户端也随机挪位置，
        // 就会和服务端同步的位置打架，玩家会感觉像被胶粘住一样走不动、被反复拽来拽去）
        if (!world.isClientSide && random.nextBoolean()) {
            // 发抖幅度封顶，不然放大器大了（sqrt(speed)）直接起飞，跳得老高
            val amp = amplifier.coerceAtMost(10)
            // 随机移动玩家的位置，确保正负方向概率相等
            val x: Double = (random.nextDouble() * 2 - 1) * (amp + 1) * 2.5
            val z: Double = (random.nextDouble() * 2 - 1) * (amp + 1) * 2.5
            // 上下也要随机（原来只往上飘，飞起来会被越带越高）
            val y: Double = (random.nextDouble() * 2 - 1) * amp * 0.0003
            entity.move(MoverType.SHULKER_BOX, Vec3(x * 0.001, y, z * 0.001))
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

        if (entity is ServerPlayer && entity is INeko){
            // 1/1000的概率发送淫叫
            if (random.nextInt(1000) == 0) {
                Messaging.modifyAndSendMessageToAll(entity,screamTexts[random.nextInt(screamTexts.size)])
            }
        }
        return super.applyEffectTick(world, entity, amplifier)
    }
}