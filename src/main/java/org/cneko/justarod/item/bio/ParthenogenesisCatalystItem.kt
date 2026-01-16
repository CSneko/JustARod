package org.cneko.justarod.item.bio

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.Tameable
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.passive.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Hand
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.getEnchantmentLevel
import org.cneko.justarod.entity.Pregnant
import java.util.*
import kotlin.jvm.optionals.getOrDefault
import kotlin.random.Random

class ParthenogenesisCatalystItem(settings: Settings) : Item(settings) {
    // 这是一个创造物品，不消耗耐久，堆叠为 1
    override fun hasGlint(stack: ItemStack): Boolean = true // 自带闪烁效果看起来更高级

    override fun useOnEntity(stack: ItemStack, user: PlayerEntity, entity: LivingEntity, hand: Hand): ActionResult {
        // --- 服务端逻辑 ---
        if (user.world.isClient) return ActionResult.SUCCESS

        // 1. 检查是否有子宫或者是否是原版可繁殖动物
        val isCustomPregnant = entity is Pregnant && entity.hasUterus()
        val isVanillaBreedingAnimal = isVanillaBreedingAnimal(entity)

        if (!isCustomPregnant && !isVanillaBreedingAnimal) {
            user.sendMessage(Text.of("§c目标不能进行孤雌生殖。"), true)
            return ActionResult.FAIL
        }

        // 2. 检查是否已经怀孕（仅对自定义生物）
        if (isCustomPregnant) {
            val pregnantEntity = entity as Pregnant
            if (pregnantEntity.isPregnant) {
                user.sendMessage(Text.of("§c目标已经怀孕了。"), true)
                return ActionResult.FAIL
            }
        }

        // 3. 获取附魔等级，计算变异系数
        val meiosisLevel = stack.getEnchantmentLevel(user.world, JREnchantments.MEIOSIS)
        val variance = if (meiosisLevel > 0) 0.1f * meiosisLevel else 0.0f

        // 4. 处理不同类型的实体
        if (isCustomPregnant) {
            handleCustomPregnantEntity(user, entity as Pregnant, variance, meiosisLevel)
        } else {
            handleVanillaAnimal(user, entity as AnimalEntity, variance, meiosisLevel)
        }

        return ActionResult.SUCCESS
    }

    /**
     * 处理自定义怀孕生物
     */
    private fun handleCustomPregnantEntity(user: PlayerEntity, entity: Pregnant, variance: Float, meiosisLevel: Int) {
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
        user.sendMessage(Text.of("§d§l生物体内的卵细胞开始了自我分裂... [$typeText]"), true)
    }

    /**
     * 处理原版动物 - 立即生成后代
     */
    private fun handleVanillaAnimal(user: PlayerEntity, animal: AnimalEntity, variance: Float, meiosisLevel: Int) {
        val world = user.world as ServerWorld

        // 1. 确定生成多少个幼崽
        val babyCount = 1

        // 2. 生成幼崽
        repeat(babyCount) {
            val baby = createBabyForVanillaAnimal(animal, world, variance)
            if (baby != null) {
                // 设置位置（在母体旁边）
                baby.setPosition(animal.x + (Math.random() - 0.5) * 2, animal.y, animal.z + (Math.random() - 0.5) * 2)
                world.spawnEntity(baby)
            }
        }

        // 3. 反馈
        user.sendMessage(Text.of("§d§l${animal.name.string} 产下了后代！"), true)

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
    private fun createBabyForVanillaAnimal(parent: AnimalEntity, world: ServerWorld, variance: Float): AnimalEntity? {
        // 1. 创建幼崽
        val baby = run {
            // 优先使用父实体的 type（即 EntityType）
            val entityType = parent.type
            val created = entityType.create(world)

            created as? AnimalEntity
        } ?: return null

        // 2. 设置为幼崽
        baby.setBreedingAge(-24000)

        // 3. 应用属性变异（如果有）
        if (variance > 0) {
            applyVarianceToVanillaBaby(baby, variance)
        }

        // 4. 对于绵羊，随机生成颜色（如果发生变异）
        if (baby is SheepEntity && variance > 0) {
            val randomColor = DyeColor.byId(baby.random.nextInt(DyeColor.entries.size))
            baby.color = randomColor
        }

        // 5. 对于狐狸，设置为不同的变种（如果发生变异）
        if (baby is FoxEntity && variance > 0) {
            val isSnow = baby.random.nextBoolean()
            baby.variant = if (isSnow) FoxEntity.Type.SNOW else FoxEntity.Type.RED
        }

        // 6. 对于猫，设置为随机品种（如果发生变异）
        if (baby is CatEntity && parent is CatEntity && variance > 0) {
            val breedCount = Registries.CAT_VARIANT.count()
            val randomBreed = baby.random.nextInt(breedCount)
            baby.variant = Registries.CAT_VARIANT.getEntry(randomBreed).getOrDefault(parent.variant)
        }

        return baby
    }

    /**
     * 为原版动物的幼崽应用属性变异
     */
    private fun applyVarianceToVanillaBaby(baby: AnimalEntity, variance: Float) {
        val random = baby.random

        // 1. 随机化成长时间
        val baseGrowthTime = 24000 // 20分钟
        val randomFactor = random.nextFloat() * 2 - 1 // -1.0 到 1.0
        val growthMultiplier = 1.0 + (randomFactor * variance)
        val newGrowthTime = (baseGrowthTime * growthMultiplier).toInt()
        baby.setBreedingAge(-newGrowthTime)

        val attributes: MutableList<RegistryEntry<EntityAttribute?>?> = ArrayList<RegistryEntry<EntityAttribute?>?>()
        // 始终可选的其他属性
        attributes.add(EntityAttributes.GENERIC_ATTACK_DAMAGE)
        attributes.add(EntityAttributes.GENERIC_MOVEMENT_SPEED)
        attributes.add(EntityAttributes.GENERIC_SCALE)


        // 随机决定额外选择几个属性（0~3个），总变异数为 1~4（因为生命值必选）
        val extraCount = random.nextInt(4) // 0, 1, 2, or 3
        // 打乱并选取 extraCount 个其他属性
        attributes.shuffle(Random)
        val selected: MutableList<RegistryEntry<EntityAttribute?>?> = ArrayList<RegistryEntry<EntityAttribute?>?>()
        selected.add(EntityAttributes.GENERIC_MAX_HEALTH) // 必选
        selected.addAll(attributes.subList(0, extraCount))
        // 应用变异
        for (attr in selected) {
            applyAttributeVariance(baby, attr, variance)
        }


        // 变异后回满血（因为最大生命值可能已变）
        baby.health = baby.maxHealth

        // 3. 为马类动物应用属性变异
        if (baby is AbstractHorseEntity) {
            applyAttributeVariance(baby, EntityAttributes.GENERIC_MOVEMENT_SPEED, variance)
            applyAttributeVariance(baby, EntityAttributes.GENERIC_JUMP_STRENGTH, variance)
        }



    }

    private fun applyAttributeVariance(
        entity: LivingEntity,
        attribute: RegistryEntry<EntityAttribute?>?,
        variance: Float
    ) {
        val instance = entity.getAttributeInstance(attribute)
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
            is AnimalEntity -> {
                // 检查是否是可以繁殖的动物
                entity is ChickenEntity ||
                        entity is CowEntity ||
                        entity is PigEntity ||
                        entity is SheepEntity ||
                        entity is AbstractHorseEntity ||
                        entity is FoxEntity ||
                        entity is CatEntity ||
                        (entity is Tameable && (entity as Tameable).owner!=null) // 驯服的狼、猫等
            }
            else -> false
        }
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        tooltip?.add(Text.of("§7对着拥有生物使用，以进行孤雌生殖。"))
        tooltip?.add(Text.of("§7默认进行无性克隆，添加减数分裂附魔可使后代属性产生变异"))
        super.appendTooltip(stack, context, tooltip, type)
    }
}