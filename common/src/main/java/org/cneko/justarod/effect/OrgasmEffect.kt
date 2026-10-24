package org.cneko.justarod.effect

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import net.minecraft.util.RandomSource
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.misc.Messaging

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

    override fun onEffectAdded(entity: LivingEntity, amplifier: Int) {
        super.onEffectAdded(entity, amplifier)
        val world = entity.level() as? ServerLevel ?: return
        // Only the first application bursts; refreshing an existing effect must not burst every tick.
        sendHearts(world, entity, 40 + amplifier.coerceIn(0, 4) * 8)
    }

    private fun sendHearts(world: ServerLevel, entity: LivingEntity, count: Int) {
        // Emit around the head, not inside the camera: near-plane hearts otherwise fill the whole screen.
        // Rotate the four clusters with the entity so one always lies in front of the first-person view.
        val yaw = Math.toRadians(entity.yRot.toDouble())
        repeat(4) { side ->
            val angle = yaw + side * Math.PI / 2
            world.sendParticles(
                ParticleTypes.HEART, true, true,
                entity.x - kotlin.math.sin(angle) * 1.1, entity.eyeY + 0.1,
                entity.z + kotlin.math.cos(angle) * 1.1,
                count / 4 + if (side < count % 4) 1 else 0,
                0.12, 0.28, 0.12, 0.025
            )
        }
    }

    // 这个方法在应用药水效果时的每个tick会被调用。
    override fun applyEffectTick(world: net.minecraft.server.level.ServerLevel, entity: LivingEntity, amplifier: Int): Boolean {
        val random: RandomSource = world.random
        // Force the defining effect through MINIMAL particle settings and the normal 32-block limit.
        // Keep hearts near eye height as well as above the head so first-person players see them too.
        if (entity.tickCount % 2 == 0) {
            sendHearts(world, entity, 20 + amplifier.coerceIn(0, 4) * 4)
        }

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
