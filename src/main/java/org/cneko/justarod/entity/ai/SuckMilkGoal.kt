package org.cneko.justarod.entity.ai

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.Pregnant
import org.cneko.toneko.common.mod.entities.NekoEntity
import java.util.EnumSet

class SuckMilkGoal(private val baby: NekoEntity) : Goal() {
    private var targetMother: LivingEntity? = null
    private var suckTick = 0

    init {
        this.flags = EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)
    }

    override fun canUse(): Boolean {
        // 只有幼年，且饥饿或血量不满时才找奶
        if (!baby.isBaby) return false
        if (baby.health >= baby.maxHealth && baby.random.nextInt(50) != 0) return false

        // 扫描附近 16 格内所有带有 Pregnant 接口且有奶的实体（有奶就是娘）
        val potentialMothers = baby.level().getEntitiesOfClass(
            LivingEntity::class.java,
            baby.boundingBox.inflate(16.0)
        ) { entity ->
            entity != baby && entity is Pregnant && entity.milk > 50.0f
        }

        if (potentialMothers.isEmpty()) return false

        // 找到奶水最多的那个
        targetMother = potentialMothers.maxByOrNull { (it as Pregnant).milk }
        return targetMother != null
    }

    override fun canContinueToUse(): Boolean {
        if (targetMother == null || !targetMother!!.isAlive) return false
        val mother = targetMother as Pregnant
        return baby.isBaby && mother.milk > 0f && baby.distanceToSqr(targetMother!!) < 256.0
    }

    override fun start() {
        suckTick = 0
    }

    override fun tick() {
        if (targetMother == null) return
        val mother = targetMother as Pregnant

        baby.lookGoal.Flag.lookAt(targetMother, 30.0f, 30.0f)

        // 距离大于 2 格，走过去
        if (baby.distanceToSqr(targetMother!!) > 4.0) {
            baby.navigation.moveTo(targetMother, 0.6)
        } else {
            // 贴贴喝奶阶段
            baby.navigation.stop()
            suckTick++

            // 每隔 1 秒 (20 tick) 吸一次
            if (suckTick % 20 == 0) {
                val extracted = mother.extractMilk(100.0f) // 每次吸 100 奶量
                if (extracted > 0) {
                    // 幼崽获得治疗
                    baby.heal(2.0f)
                    // 加速成长 (原版机制：减少幼年期负数 tick)
                    baby.age += 600 // 加速半分钟

                    // 冒出爱心粒子
                    val world = baby.world
                    if (world is ServerLevel) {
                        level().sendParticles(
                            ParticleTypes.HEART,
                            targetMother!!.x, targetMother!!.y + 1.0, targetMother!!.z,
                            1, 0.3, 0.5, 0.3, 0.0
                        )
                    }

                    // ====== 疾病传播逻辑 ======
                    // 1. 艾滋病 (AIDS) 垂直传播
                    if (mother.aids > 0 && baby is Pregnant) {
                        if (baby.aids == 0 && baby.random.nextInt(10) == 0) {
                            baby.aids = 1 // 幼崽染上艾滋
                        }
                    }
                    // 2. 梅毒 (Syphilis) 传播
                    if (mother.syphilis > 0 && baby is Pregnant) {
                        if (baby.syphilis == 0 && baby.random.nextInt(10) == 0) {
                            baby.syphilis = 1
                        }
                    }
                    // 3. 喝了严重乳腺炎的毒奶 -> 拉肚子/中毒
                    if (mother.mastitis > 20 * 60 * 10) {
                        baby.addEffect(MobEffectInstance(MobEffects.POISON, 20 * 5, 0))
                        baby.addEffect(MobEffectInstance(MobEffects.CONFUSION, 20 * 10, 0))
                    }
                }
            }
        }
    }

    override fun stop() {
        targetMother = null
        suckTick = 0
    }
}