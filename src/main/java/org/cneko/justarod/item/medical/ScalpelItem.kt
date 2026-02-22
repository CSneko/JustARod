package org.cneko.justarod.item.medical

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ProfileComponent
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.containsEnchantment
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.JRItems
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.rod.addEffect
import org.cneko.toneko.common.mod.util.TickTaskQueue

// 新增实体类导入，用于斩首时判断生物类型并掉落对应头颅
import net.minecraft.entity.mob.SkeletonEntity
import net.minecraft.entity.mob.WitherSkeletonEntity
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.boss.dragon.EnderDragonEntity

// 继承自 MedicalItem
class ScalpelItem(settings: Settings) : MedicalItem(settings.maxCount(1).maxDamage(4)) {

    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            tooltip.add(Text.of("§c使用它可进行子宫切除"))
            tooltip.add(Text.of("§c此操作会永久切除子宫，请谨慎使用！"))
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            tooltip.add(Text.of("§a使用它可以安装子宫"))
        } else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            tooltip.add(Text.of("§c使用它可以进行乳房切除"))
        } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
            tooltip.add(Text.of("§a想练此功，必先自宫"))
        } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
            tooltip.add(Text.of("§c使用它可以进行截肢"))
            tooltip.add(Text.of("§d嗯... 你应该不是病娇吧？"))
        } else if (stack.containsEnchantment(JREnchantments.BEHEADING)) {
            tooltip.add(Text.of("§c使用它可以进行斩首"))
            tooltip.add(Text.of("§d这样做的话... 嗯... 小心点哦~"))
        } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
            tooltip.add(Text.of("§c使用它可进行痔疮切除术"))
            tooltip.add(Text.of("§7彻底解决难言之隐"))
        } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
            tooltip.add(Text.of("§c使用它可进行处女膜切开术"))
            tooltip.add(Text.of("§a用来切除闭锁问题"))
            tooltip.add(Text.of("§7当然你也可以选择放弃处的身份"))
        } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
            tooltip.add(Text.of("§c使用它可进行腹腔镜微创手术"))
            tooltip.add(Text.of("§a用于修补黄体破裂及清理腹腔积血"))
        }
    }

    /**
     * 检查手术刀是否可用的所有先决条件
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        // 通用检查：物品损坏或目标必须存活
        if (stack.damage >= stack.maxDamage) return false // 物品损坏
        if (target.isDead || !target.isAlive) return false

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
            val offHandStack = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
            return !target.hasUterus() && offHandStack.isOf(JRItems.UTERUS) // 目标需要安装，且使用者副手持有子宫
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
        }

        return false // 没有对应附魔，无法使用
    }

    /**
     * 根据失败的条件提供具体消息
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (stack.damage >= stack.maxDamage) return Text.of("§c手术刀已损坏！")
        if (target !is Pregnant && !stack.containsEnchantment(JREnchantments.BEHEADING)) return Text.of("§c只能对可进行此手术的玩家使用！")
        if (target is Pregnant) {
            if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
                if (!target.hasUterus()) return if (user == target) Text.of("§c你已经切除过了！") else Text.of("§c对方已经切除过了！")
            } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
                if (target.hasUterus()) return if (user == target) Text.of("§c你不需要安装子宫！") else Text.of("§c对方不需要安装子宫！")
                val offHandStack = user.getStackInHand(Hand.OFF_HAND)
                if (!offHandStack.isOf(JRItems.UTERUS)) return Text.of("§c你的副手必须持有子宫才能执行此操作！")
            } else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)) {
                return if (user == target) Text.of("§c你没有怀孕！") else Text.of("§c对方没有怀孕")
            } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
                if (target.isOrchiectomy) return if (user == target) Text.of("§c你已经切除过了！") else Text.of("§c对方已经切除过了！")
            } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
                if (target.isAmputated) return if (user == target) Text.of("§c你已经截肢过了！") else Text.of("§c对方已经截肢过了！")
            } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
                return if (user == target) Text.of("§c你没有痔疮！") else Text.of("§c对方没有痔疮！")
            } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
                if (!target.isImperforateHymen && !target.hasHymen()) {
                    return if (user == target) Text.of("§c你没有处女膜！") else Text.of("§c对方没有处女膜！")
                }
            } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
                return if (user == target) Text.of("§c你没有发生黄体破裂或腹腔内出血，无需进行此手术！") else Text.of("§c对方未发生腹腔内出血，无需进行此手术！")
            }
        }

        return Text.of("§c不满足使用条件。") // 默认失败消息
    }

    /**
     * 执行核心效果：改变状态、造成伤害、应用效果等
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        // 先处理斩首
        if (stack.containsEnchantment(JREnchantments.BEHEADING)) {
            if (target.isDead || !target.isAlive) return
            if (target is PlayerEntity) {
                target.damage(target.world.damageSources.generic(), 1000f)
                if (target.random.nextBoolean()) {
                    val head = Items.PLAYER_HEAD.defaultStack
                    head.set(DataComponentTypes.PROFILE, ProfileComponent(target.gameProfile))
                    target.dropStack(head)
                } else {
                    target.dropStack(Items.BONE.defaultStack)
                }
            } else {
                when {
                    target is WitherSkeletonEntity -> target.dropStack(Items.WITHER_SKELETON_SKULL.defaultStack)
                    target is SkeletonEntity -> target.dropStack(Items.SKELETON_SKULL.defaultStack)
                    target is ZombieEntity -> target.dropStack(Items.ZOMBIE_HEAD.defaultStack)
                    target is CreeperEntity -> target.dropStack(Items.CREEPER_HEAD.defaultStack)
                    target is EnderDragonEntity -> target.dropStack(Items.DRAGON_HEAD.defaultStack)
                }
                target.damage(target.world.damageSources.generic(), 1000f)
            }
            consumeItem(user, target, stack, hand)
            return
        }

        // 目标必须实现 Pregnant 接口
        if (target !is Pregnant) return

        // 通用效果：扣血和状态效果
        target.damage(target.world.damageSources.generic(), 10f)
        target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1))
        target.addStatusEffect(StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 1))
        target.addStatusEffect(StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.FAINT_EFFECT), 300, 1))

        // 根据附魔执行特定效果
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            target.setHasUterus(false)
            target.dropStack(ItemStack(JRItems.UTERUS))
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            target.setHasUterus(true)
            val offHandStack = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
            if (offHandStack.isOf(JRItems.UTERUS)) {
                offHandStack.decrement(1)
            }
        } else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)) {
            val pre = target.pregnant
            if (pre > 20*60*20*5) {
                val task = TickTaskQueue()
                target.damage(target.world.damageSources.generic(), 2f)
                for (i in 1..10) {
                    task.addTask(20 * i) {
                        if (!target.isDead) {
                            target.damage(target.world.damageSources.generic(), 2f)
                        }
                    }
                }
                if (target.random.nextInt(5) == 0) {
                    target.isSterilization = true
                    target.addEffect(StatusEffects.NAUSEA, 0, 20 * 15)
                    val complicationMsg = "§c并发症！手术对你造成了永久性损伤！"
                    if (user != target) {
                        user.sendMessage(Text.of("§e并发症发生了..."), false)
                    }
                    target.sendMessage(Text.of(complicationMsg))
                }
            }
            target.pregnant = 0
            target.dropStack(JRItems.MOLE.defaultStack)
        } else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            target.breastCancer = 0
            target.dropStack(Items.CHICKEN.defaultStack)
        } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
            target.isOrchiectomy = true
            val eggs = Items.EGG.defaultStack
            eggs.count = 2
            target.dropStack(eggs)
            target.damage(target.world.damageSources.generic(), 6f)
        } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
            target.isAmputated = true
            target.damage(target.world.damageSources.generic(), 8f)
            target.dropStack(Items.BONE.defaultStack)
        } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
            target.hemorrhoids = 0
            target.dropStack(ItemStack(JRItems.MOLE))
            target.damage(target.world.damageSources.generic(), 4f)
            target.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 1200, 0))
        } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
            target.performHymenotomy()
            target.damage(target.world.damageSources.generic(), 2f)
        } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
            // 调用治愈方法 (cure方法内部会给予瞬间恢复效果，正好抵消上面的通用10点扣血)
            target.cureCorpusLuteumRupture()
            // 掉落清理出来的腹腔积血 (血块)
            target.dropStack(JRItems.MOLE.defaultStack)
        }
    }

    /**
     * 消耗手术刀的耐久度
     */
    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        stack.damage(1, user, EquipmentSlot.MAINHAND)
    }

    /**
     * 获取成功操作后的提示消息
     */
    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己进行了子宫切除！") else Text.of("§a已为对方进行子宫切除！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§c你被进行了子宫切除手术！")
            )
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己安装了子宫！") else Text.of("§a已为对方安装子宫！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你被安装了子宫！")
            )
        } else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功切除了你自己的乳房") else Text.of("§a已为对方进行乳房切除"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你被进行了乳房切除！")
            )
        } else if (stack.containsEnchantment(JREnchantments.ORCHIECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功切除了你自己的魔丸") else Text.of("§a已为对方进行切除魔丸！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你被进行了魔丸切除！")
            )
        } else if (stack.containsEnchantment(JREnchantments.AMPUTATING)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己进行了截肢！") else Text.of("§a已为对方进行截肢！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§c你被进行了截肢手术！")
            )
        } else if (stack.containsEnchantment(JREnchantments.BEHEADING)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己进行了斩首（……）") else Text.of("§a已为对方进行了斩首！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§c你被斩首了！")
            )
        } else if (stack.containsEnchantment(JREnchantments.HEMORRHOIDECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功切除了自己的痔疮！一身轻松！") else Text.of("§a已为对方成功切除痔疮！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你的痔疮被切除了，屁股虽然痛但是很清爽！")
            )
        } else if (stack.containsEnchantment(JREnchantments.HYMENOTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己进行了处女膜切开术！") else Text.of("§a已为对方进行了处女膜切开术！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你的处女膜被切开了！")
            )
        } else if (stack.containsEnchantment(JREnchantments.LAPAROSCOPY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己进行了腹腔镜手术，清除了腹腔积血！") else Text.of("§a已成功为对方进行腹腔镜微创手术，止住了内出血！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你接受了腹腔镜手术，黄体破裂已被修补！")
            )
        }

        return ActionMessages(Text.of("操作完成。"), null)
    }
}