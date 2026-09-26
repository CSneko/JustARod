package org.cneko.justarod.item.medical
import org.cneko.justarod.JRIds

import org.cneko.justarod.spawnItemAtLocation

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.containsEnchantment
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.JRItems
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Items
import org.cneko.justarod.entity.Pregnant
import org.cneko.toneko.common.mod.util.TickTaskQueue

// 新增实体类导入，用于斩首时判断生物类型并掉落对应头颅
import net.minecraft.world.entity.monster.skeleton.Skeleton
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.boss.enderdragon.EnderDragon

// 继承自 MedicalItem
class ScalpelItem(properties: Properties) : MedicalItem(JRIds.itemProps("scalpel").stacksTo(1).durability(4)) {

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            adder.accept(Component.literal("§c使用它可进行子宫切除"))
            adder.accept(Component.literal("§c此操作会永久切除子宫，请谨慎使用！"))
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            adder.accept(Component.literal("§a使用它可以安装子宫"))
        } else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            adder.accept(Component.literal("§c使用它可以进行乳房切除"))
        } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
            adder.accept(Component.literal("§a想练此功，必先自宫"))
        } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
            adder.accept(Component.literal("§c使用它可以进行截肢"))
            adder.accept(Component.literal("§d嗯... 你应该不是病娇吧？"))
        } else if (stack.containsEnchantment(JREnchantments.BEHEADING)) {
            adder.accept(Component.literal("§c使用它可以进行斩首"))
            adder.accept(Component.literal("§d这样做的话... 嗯... 小心点哦~"))
        } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
            adder.accept(Component.literal("§c使用它可进行痔疮切除术"))
            adder.accept(Component.literal("§7彻底解决难言之隐"))
        } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
            adder.accept(Component.literal("§c使用它可进行处女膜切开术"))
            adder.accept(Component.literal("§a用来切除闭锁问题"))
            adder.accept(Component.literal("§7当然你也可以选择放弃处的身份"))
        } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
            adder.accept(Component.literal("§c使用它可进行腹腔镜微创手术"))
            adder.accept(Component.literal("§a用于修补黄体破裂及清理腹腔积血"))
        } else if (stack.containsEnchantment(JREnchantments.CATARACT_SURGERY)) {
            adder.accept(Component.literal("§c使用它可进行白内障手术（晶状体置换）"))
            adder.accept(Component.literal("§a撕开浑浊的晶状体，换上一片崭新的人工晶体"))
        }
    }

    /**
     * 检查手术刀是否可用的所有先决条件
     */
    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        // 通用检查：物品损坏或目标必须存活
        if (stack.damageValue >= stack.maxDamage) return false // 物品损坏
        if (target.isDeadOrDying() || !target.isAlive) return false

        // 如果是斩首，允许对任何存活的实体使用（不必实现 Pregnant）
        if (stack.containsEnchantment(JREnchantments.BEHEADING)) {
            return true
        }

        // 其余手术仍要求目标实现 Pregnant 接口
        if (target !is Pregnant) return false // 目标必须是实现了Pregnant接口

        // 根据附魔检查特定条件
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            return target.hasUterus() // 目标尚未切除子宫
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            val offhandStack = user.getItemInHand(if (hand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND)
            return !target.hasUterus() && offhandStack.`is`(JRItems.UTERUS) // 目标需要安装，且使用者副手持有子宫
        } else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)) {
            return target.pregnant > 0 && target.isFemale
        } else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            return target.isFemale
        } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
            return target.isMale && !target.isOrchiectomy
        } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
            return !target.isAmputated
        } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
            return target.hemorrhoids > 0
        } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
            return target.isImperforateHymen || target.hasHymen()
        } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
            return target.corpusLuteumRupture > 0
        } else if (stack.containsEnchantment(JREnchantments.CATARACT_SURGERY)) {
            return target.cataract > 0
        }

        return false // 没有对应附魔，无法使用
    }

    /**
     * 根据失败的条件提供具体消息
     */
    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        if (stack.damageValue >= stack.maxDamage) return Component.literal("§c手术刀已损坏！")
        if (target !is Pregnant && !stack.containsEnchantment(JREnchantments.BEHEADING)) return Component.literal("§c只能对可进行此手术的玩家使用！")
        if (target is Pregnant) {
            if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
                if (!target.hasUterus()) return if (user == target) Component.literal("§c你已经切除过了！") else Component.literal("§c对方已经切除过了！")
            } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
                if (target.hasUterus()) return if (user == target) Component.literal("§c你不需要安装子宫！") else Component.literal("§c对方不需要安装子宫！")
                val offhandStack = user.getItemInHand(InteractionHand.OFF_HAND)
                if (!offhandStack.`is`(JRItems.UTERUS)) return Component.literal("§c你的副手必须持有子宫才能执行此操作！")
            } else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)) {
                return if (user == target) Component.literal("§c你没有怀孕！") else Component.literal("§c对方没有怀孕")
            } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
                if (target.isOrchiectomy) return if (user == target) Component.literal("§c你已经切除过了！") else Component.literal("§c对方已经切除过了！")
            } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
                if (target.isAmputated) return if (user == target) Component.literal("§c你已经截肢过了！") else Component.literal("§c对方已经截肢过了！")
            } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
                return if (user == target) Component.literal("§c你没有痔疮！") else Component.literal("§c对方没有痔疮！")
            } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
                if (!target.isImperforateHymen && !target.hasHymen()) {
                    return if (user == target) Component.literal("§c你没有处女膜！") else Component.literal("§c对方没有处女膜！")
                }
            } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
                return if (user == target) Component.literal("§c你没有发生黄体破裂或腹腔内出血，无需进行此手术！") else Component.literal("§c对方未发生腹腔内出血，无需进行此手术！")
            } else if (stack.containsEnchantment(JREnchantments.CATARACT_SURGERY)) {
                return if (user == target) Component.literal("§c你的眼睛还清澈得很，不需要换晶状体！") else Component.literal("§c对方没有白内障，无需进行此手术！")
            }
        }

        return Component.literal("§c不满足使用条件。") // 默认失败消息
    }

    /**
     * 执行核心效果：改变状态、造成伤害、应用效果等
     */
    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        // 先处理斩首
        if (stack.containsEnchantment(JREnchantments.BEHEADING)) {
            if (target.isDeadOrDying() || !target.isAlive) return
            if (target is Player) {
                target.hurt(target.level().damageSources().generic(), 1000f)
                if (target.random.nextBoolean()) {
                    val head = Items.PLAYER_HEAD.defaultInstance
                    head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(target.gameProfile))
                    target.spawnItemAtLocation(head)
                } else {
                    target.spawnItemAtLocation(Items.BONE.defaultInstance)
                }
            } else {
                when {
                    target is WitherSkeleton -> target.spawnItemAtLocation(Items.WITHER_SKELETON_SKULL.defaultInstance)
                    target is Skeleton -> target.spawnItemAtLocation(Items.SKELETON_SKULL.defaultInstance)
                    target is Zombie -> target.spawnItemAtLocation(Items.ZOMBIE_HEAD.defaultInstance)
                    target is Creeper -> target.spawnItemAtLocation(Items.CREEPER_HEAD.defaultInstance)
                    target is EnderDragon -> target.spawnItemAtLocation(Items.DRAGON_HEAD.defaultInstance)
                }
                target.hurt(target.level().damageSources().generic(), 1000f)
            }
            consumeItem(user, target, stack, hand)
            return
        }

        // 目标必须实现 Pregnant 接口
        if (target !is Pregnant) return

        // 通用效果：扣血和状态效果
        target.hurt(target.level().damageSources().generic(), 10f)
        target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 600, 1))
        target.addEffect(MobEffectInstance(MobEffects.MINING_FATIGUE, 600, 1))
        target.addEffect(MobEffectInstance(JREffects.FAINT_EFFECT, 300, 1))

        // 根据附魔执行特定效果
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            target.setHasUterus(false)
            target.spawnItemAtLocation(ItemStack(JRItems.UTERUS))
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            target.setHasUterus(true)
            val offhandStack = user.getItemInHand(if (hand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND)
            if (offhandStack.`is`(JRItems.UTERUS)) {
                offhandStack.shrink(1)
            }
        } else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)) {
            val pre = target.pregnant
            if (pre > 20*60*20*5) {
                val task = TickTaskQueue()
                target.hurt(target.level().damageSources().generic(), 2f)
                for (i in 1..10) {
                    task.addTask(20 * i) {
                        if (!target.isDeadOrDying()) {
                            target.hurt(target.level().damageSources().generic(), 2f)
                        }
                    }
                }
                if (target.random.nextInt(5) == 0) {
                    target.isSterilization = true
                    // 修复：原写法 addEffect(CONFUSION, 0, 20*15) 会被解析成"时长0、等级300"，
                    // 且依赖的 rod.addEffect 扩展在 mojmap 迁移后无法解析；改用标准 MobEffectInstance
                    target.addEffect(MobEffectInstance(
                        MobEffects.NAUSEA, 20 * 15, 0
                    ))
                    val complicationMsg = "§c并发症！手术对你造成了永久性损伤！"
                    if (user != target) {
                        (user as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§e并发症发生了..."))
                    }
                    (target as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal(complicationMsg))
                }
            }
            target.pregnant = 0
            target.spawnItemAtLocation(JRItems.MOLE.defaultInstance)
        } else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            target.breastCancer = 0
            target.spawnItemAtLocation(Items.CHICKEN.defaultInstance)
        } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
            target.isOrchiectomy = true
            val eggs = Items.EGG.defaultInstance
            eggs.count = 2
            target.spawnItemAtLocation(eggs)
            target.hurt(target.level().damageSources().generic(), 6f)
        } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
            target.isAmputated = true
            target.hurt(target.level().damageSources().generic(), 8f)
            target.spawnItemAtLocation(Items.BONE.defaultInstance)
        } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
            target.hemorrhoids = 0
            target.spawnItemAtLocation(ItemStack(JRItems.MOLE))
            target.hurt(target.level().damageSources().generic(), 4f)
            target.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 1200, 0))
        } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
            target.performHymenotomy()
            target.hurt(target.level().damageSources().generic(), 2f)
        } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
            // 调用治愈方法 (cure方法内部会给予瞬间恢复效果，正好抵消上面的通用10点扣血)
            target.cureCorpusLuteumRupture()
            // 掉落清理出来的腹腔积血 (血块)
            target.spawnItemAtLocation(JRItems.MOLE.defaultInstance)
        } else if (stack.containsEnchantment(JREnchantments.CATARACT_SURGERY)) {
            // 调用治愈方法 (cureCataract 内部会给予短暂的术后畏光失明，符合手术设定)
            target.cureCataract()
            // 掉落被摘除的浑浊晶状体
            target.spawnItemAtLocation(JRItems.MOLE.defaultInstance)
        }
    }

    /**
     * 消耗手术刀的耐久度
     */
    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND)
    }

    /**
     * 获取成功操作后的提示消息
     */
    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功为自己进行了子宫切除！") else Component.literal("§a已为对方进行子宫切除！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§c你被进行了子宫切除手术！")
            )
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功为自己安装了子宫！") else Component.literal("§a已为对方安装子宫！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§a你被安装了子宫！")
            )
        } else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功切除了你自己的乳房") else Component.literal("§a已为对方进行乳房切除"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§a你被进行了乳房切除！")
            )
        } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功切除了你自己的魔丸") else Component.literal("§a已为对方进行切除魔丸！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§a你被进行了魔丸切除！")
            )
        } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功为自己进行了截肢！") else Component.literal("§a已为对方进行截肢！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§c你被进行了截肢手术！")
            )
        } else if (stack.containsEnchantment(JREnchantments.BEHEADING)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功为自己进行了斩首（……）") else Component.literal("§a已为对方进行了斩首！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§c你被斩首了！")
            )
        } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功切除了自己的痔疮！一身轻松！") else Component.literal("§a已为对方成功切除痔疮！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§a你的痔疮被切除了，屁股虽然痛但是很清爽！")
            )
        } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功为自己进行了处女膜切开术！") else Component.literal("§a已为对方进行了处女膜切开术！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§a你的处女膜被切开了！")
            )
        } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功为自己进行了腹腔镜手术，清除了腹腔积血！") else Component.literal("§a已成功为对方进行腹腔镜微创手术，止住了内出血！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§a你接受了腹腔镜手术，黄体破裂已被修补！")
            )
        } else if (stack.containsEnchantment(JREnchantments.CATARACT_SURGERY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Component.literal("§a你成功为自己置换了人工晶状体，眼前的世界重新变得清晰！") else Component.literal("§a已成功为对方进行白内障手术，换上了崭新的人工晶状体！"),
                targetSuccessMessage = if (isSelf) null else Component.literal("§a你接受了白内障手术，眼睛又能看清了！")
            )
        }

        return ActionMessages(Component.literal("操作完成。"), null)
    }
}