package org.cneko.justarod.item.bio

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Holder
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.DyeColor
import net.minecraft.world.InteractionHand
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.getEnchantmentLevel
import org.cneko.justarod.entity.Pregnant
import java.util.*
import kotlin.jvm.optionals.getOrDefault
import kotlin.random.Random

class ParthenogenesisCatalystItem(properties: Properties) : Item(properties) {
    // 这是一个创造物品，不消耗耐久，堆叠为 1
    override fun hasGlint(stack: ItemStack): Boolean = true // 自带闪烁效果看起来更高级

    override fun useOnEntity(stack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        // --- 服务端逻辑 ---
        if (user.level().isClientSide) return InteractionResult.SUCCESS

        // 1. 检查是否有子宫或者是否是原版可繁殖动物
        val isCustomPregnant = entity is Pregnant && entity.hasUterus()
        val isVanillaBreedingAnimal = isVanillaBreedingAnimal(entity)

        if (!isCustomPregnant && !isVanillaBreedingAnimal) {
            user.sendSystemMessage(Component.literal("§c目标不能进行孤雌生殖。"), true)
            return InteractionResult.FAIL
        }

        // 2. 检查是否已经怀孕（仅对自定义生物）
        if (isCustomPregnant) {
            val pregnantEntity = entity as Pregnant
            if (pregnantEntity.isPregnant) {
                user.sendSystemMessage(Component.literal("§c目标已经怀孕了。"), true)
                return InteractionResult.FAIL
            }
        }

        // 3. 获取附魔等级，计算变异系数
        val meiosisLevel = stack.getEnchantmentLevel(user.level(), JREnchantments.MEIOSIS)
        val variance = if (meiosisLevel > 0) 0.1f * meiosisLevel else 0.0f

        // 4. 处理不同类型的实体
        if (isCustomPregnant) {
            handleCustomPregnantEntity(user, entity as Pregnant, variance, meiosisLevel)
        } else {
            handleVanillaAnimal(user, entity as Animal, variance, meiosisLevel)
        }

        return InteractionResult.SUCCESS
    }

    /**
     * 处理自定义怀孕生物
     */
    private fun handleCustomPregnantEntity(user: Player, entity: Pregnant, variance: Float, meiosisLevel: Int) {
        // B. 设置接口数据
        entity.setParthenogenesisVariance(variance)
        entity.setChildrenType((entity as Entity).type) // 孩子类型设为同种

        // C. 触发怀孕逻辑
        entity.tryPregnant()

        // D. 根据自身计算产仔数
        val babyCount = entity.calculateBabyCount(entity as LivingEntity)
        entity.setBabyCount(babyCount)

        // E. 反馈
        val varianceText = (variance * 100).toInt()
        val typeText = if (variance > 0) "减数分裂 (变异率: $varianceText%)" else "无性克隆"
        user.sendSystemMessage(Component.literal("§d§l生物体内的卵细胞开始了自我分裂... [$typeText]"), true)
    }

    /**
     * 处理原版动物 - 立即生成后代
     */
    private fun handleVanillaAnimal(user: Player, animal: Animal, variance: Float, meiosisLevel: Int) {
        val world = user.level() as ServerLevel

        // 1. 确定生成多少个幼崽
        val babyCount = 1

        // 2. 生成幼崽
        repeat(babyCount) {
            val baby = createBabyForVanillaAnimal(animal, world, variance)
            if (baby != null) {
                // 设置位置（在母体旁边）
                baby.setPosition(animal.x + (Math.random() - 0.5) * 2, animal.y, animal.z + (Math.random() - 0.5) * 2)
                level().addFreshEntity(baby)
            }
        }

        // 3. 反馈
        user.sendSystemMessage(Component.literal("§d§l${animal.name.string} 产下了后代！"), true)

        // 生成粒子效果（心形）
        val random = animal.random
        for (i in 0 until 7) {
            world.addParticle(
                ParticleTypes.HEART,
                animal.x + (random.nextDouble() * 2 - 1),  // 确保x方向正负概率相等
                animal.y + random.nextDouble() * 2 + 2,  // 保持y方向逻辑
                animal.z + (random.nextDouble() * 2 - 1),  // 确保z方向正负概率相等
                0.0,
                1.5,
                0.0
            )
        }

        // 4. 应用繁殖冷却
        animal.loveTicks = 6000
    }

    /**
     * 为原版动物创建后代，考虑属性变异
     */
    private fun createBabyForVanillaAnimal(parent: Animal, world: ServerLevel, variance: Float): Animal? {
        // 1. 创建幼崽
        val baby = run {
            // 优先使用父实体的 type（即 EntityType）
            val entityType = parent.type
            val created = entityType.create(world)

            created as? Animal
        } ?: return null

        // 2. 设置为幼崽
        baby.setBreedingAge(-24000)

        // 3. 应用属性变异（如果有）
        if (variance > 0) {
            applyVarianceToVanillaBaby(baby, variance)
        }

        // 4. 对于绵羊，随机生成颜色（如果发生变异）
        if (baby is Sheep && variance > 0) {
            val randomColor = DyeColor.byId(baby.random.nextInt(DyeColor.entries.size))
            baby.color = randomColor
        }

        // 5. 对于狐狸，设置为不同的变种（如果发生变异）
        if (baby is Fox && variance > 0) {
            val isSnow = baby.random.nextBoolean()
            baby.variant = if (isSnow) Fox.Type.SNOW else Fox.Type.RED
        }

        // 6. 对于猫，设置为随机品种（如果发生变异）
        if (baby is Cat && parent is Cat && variance > 0) {
            val breedCount = BuiltInRegistries.CAT_VARIANT.count()
            val randomBreed = baby.random.nextInt(breedCount)
            baby.variant = BuiltInRegistries.CAT_VARIANT.getOrThrow(randomBreed).getOrDefault(parent.variant)
        }

        return baby
    }

    /**
     * 为原版动物的幼崽应用属性变异
     */
    private fun applyVarianceToVanillaBaby(baby: Animal, variance: Float) {
        val random = baby.random

        // 1. 随机化成长时间
        val baseGrowthTime = 24000 // 20分钟
        val randomFactor = random.nextFloat() * 2 - 1 // -1.0 到 1.0
        val growthMultiplier = 1.0 + (randomFactor * variance)
        val newGrowthTime = (baseGrowthTime * growthMultiplier).toInt()
        baby.setBreedingAge(-newGrowthTime)

        val attributes: MutableList<Holder<Attribute?>?> = ArrayList<Holder<Attribute?>?>()
        // 始终可选的其他属性
        attributes.add(Attributes.ATTACK_DAMAGE)
        attributes.add(Attributes.MOVEMENT_SPEED)
        attributes.add(Attributes.SCALE)


        // 随机决定额外选择几个属性（0~3个），总变异数为 1~4（因为生命值必选）
        val extraCount = random.nextInt(4) // 0, 1, 2, or 3
        // 打乱并选取 extraCount 个其他属性
        attributes.shuffle(Random)
        val selected: MutableList<Holder<Attribute?>?> = ArrayList<Holder<Attribute?>?>()
        selected.add(Attributes.MAX_HEALTH) // 必选
        selected.addAll(attributes.subList(0, extraCount))
        // 应用变异
        for (attr in selected) {
            applyAttributeVariance(baby, attr, variance)
        }


        // 变异后回满血（因为最大生命值可能已变）
        baby.health = baby.maxHealth

        // 3. 为马类动物应用属性变异
        if (baby is AbstractHorse) {
            applyAttributeVariance(baby, Attributes.MOVEMENT_SPEED, variance)
            applyAttributeVariance(baby, Attributes.JUMP_STRENGTH, variance)
        }



    }

    private fun applyAttributeVariance(
        entity: LivingEntity,
        attribute: Holder<Attribute?>?,
        variance: Float
    ) {
        val instance = entity.getAttribute(attribute)
        if (instance != null) {
            val base = instance.baseValue
            // 生成 -1.0 到 1.0 之间的随机数
            val randomFactor = entity.getRandom().nextDouble() * 2.0 - 1.0
            // 计算倍率：例如 variance=0.1, random=-0.5 => multiplier = 1.0 + (-0.05) = 0.95
            val multiplier = 1.0 + (randomFactor * variance)
            instance.baseValue = base * multiplier
        }
    }

    /**
     * 检查是否是原版可繁殖动物
     */
    private fun isVanillaBreedingAnimal(entity: LivingEntity): Boolean {
        return when (entity) {
            is Animal -> {
                // 检查是否是可以繁殖的动物
                entity is Chicken ||
                        entity is Cow ||
                        entity is Pig ||
                        entity is Sheep ||
                        entity is AbstractHorse ||
                        entity is Fox ||
                        entity is Cat ||
                        (entity is TamableAnimal && (entity as TamableAnimal).owner!=null) // 驯服的狼、猫等
            }
            else -> false
        }
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        tooltip?.add(Component.literal("§7对着拥有生物使用，以进行孤雌生殖。"))
        tooltip?.add(Component.literal("§7默认进行无性克隆，添加减数分裂附魔可使后代属性产生变异"))
        super.appendHoverText(stack, context, tooltip, type)
    }
}