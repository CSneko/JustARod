package org.cneko.justarod.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;
import org.cneko.justarod.ascaris.AscarisPoseState;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.AscarisActionPayload;

import java.util.List;

/**
 * 蛔虫感染、双药驱虫排出、甜食收服、1格爬行、驱虫药恐惧与双人尾巴缠绕的服务端回归测试。
 */
public class AscarisGameTests {

    private static ServerPlayer survivalPlayer(GameTestHelper h, double x, double z) {
        ServerPlayer p = MockPlayers.serverPlayerInLevel(h);
        for (int dx = 0; dx <= 8; dx++) {
            for (int dz = 0; dz <= 8; dz++) {
                h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(dx, 1, dz)), Blocks.STONE.defaultBlockState());
            }
        }
        Vec3 pos = h.absoluteVec(new Vec3(x, 2, z));
        p.teleportTo(pos.x, pos.y, pos.z);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GameTest(maxTicks = 120)
    public void pagodaCandyExpelsWeakAscarisAndTwoSweetsTameHer(GameTestHelper h) {
        ServerPlayer host = survivalPlayer(h, 4.5, 4.5);
        Pregnant p = (Pregnant) (Object) host;
        p.setAscaris(AscarisInfectionController.STAGE_3_TICKS);
        h.assertTrue(AscarisInfectionController.stageOf(p.getAscaris()) == 3, "Stage 3 infection set");

        boolean started = AscarisInfectionController.administerDewormer(host, AscarisInfectionController.DRUG_PAGODA, host);
        h.assertTrue(started, "Pagoda candy starts expulsion sequence at Stage 3");
        h.assertTrue(AscarisInfectionController.isExpelling(host), "Host is actively expelling");

        h.runAfterDelay(65, () -> {
            h.assertTrue(p.getAscaris() == 0, "Host infection cleared after expulsion");
            List<AscarisEntity> spawned = h.getLevel().getEntitiesOfClass(AscarisEntity.class, host.getBoundingBox().inflate(5.0));
            h.assertTrue(!spawned.isEmpty(), "Expulsion spawns a living Ascaris-chan entity");
            AscarisEntity ascaris = spawned.getFirst();
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.COLLAPSE, "Expelled Ascaris enters collapse pose");
            h.assertTrue(ascaris.getExpelledStage() == AscarisEntity.EXPELLED_PAGODA_WEAK, "Pagoda candy produces gentle weak state");

            // 第一次喂甜食 -> 害羞蜷缩 (curl_up)
            host.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 3));
            ascaris.mobInteract(host, InteractionHand.MAIN_HAND);
            h.assertTrue(!ascaris.isTame(), "First sweet comforts without instant taming");
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.CURL_UP, "First sweet transitions to cautious curl_up");

            // 第二次喂甜食 -> 开心摇摆 + 正式驯服！
            ascaris.mobInteract(host, InteractionHand.MAIN_HAND);
            h.assertTrue(ascaris.isTame() && ascaris.isOwnedBy(host), "Second sweet tames Ascaris-chan");
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.HAPPY, "Taming plays happy celebration animation");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void albendazoleStage3ExpelsCriticalAndStage1CuresDirectly(GameTestHelper h) {
        ServerPlayer early = survivalPlayer(h, 2.5, 4.5);
        ((Pregnant) (Object) early).setAscaris(500);
        AscarisInfectionController.administerDewormer(early, AscarisInfectionController.DRUG_ALBENDAZOLE, early);
        h.assertTrue(((Pregnant) (Object) early).getAscaris() == 0, "Stage 1 albendazole eradicates larvae immediately");

        ServerPlayer severe = survivalPlayer(h, 6.5, 4.5);
        ((Pregnant) (Object) severe).setAscaris(AscarisInfectionController.STAGE_4_TICKS);
        AscarisInfectionController.administerDewormer(severe, AscarisInfectionController.DRUG_ALBENDAZOLE, severe);
        h.runAfterDelay(65, () -> {
            List<AscarisEntity> spawned = h.getLevel().getEntitiesOfClass(AscarisEntity.class, severe.getBoundingBox().inflate(5.0));
            h.assertTrue(!spawned.isEmpty(), "Albendazole expels adult Ascaris at Stage 4");
            AscarisEntity a = spawned.getFirst();
            h.assertTrue(a.getExpelledStage() == AscarisEntity.EXPELLED_ALBENDAZOLE_CRITICAL, "Marked critical after albendazole");
            h.assertTrue(a.getHealth() <= 4.5f, "Albendazole expulsion leaves Ascaris critically weak");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void tailWrapModesCrawlHitboxAndDewormerPhobiaWork(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h, 4.5, 4.5);
        AscarisEntity ascaris = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris entity creates cleanly");
        ascaris.setPos(player.getX() + 1.2, player.getY(), player.getZ());
        ascaris.setTame(true, true);
        ascaris.tame(player);
        h.getLevel().addFreshEntity(ascaris);

        // 1. 爬行动态缩小碰撞箱
        float standH = ascaris.getBbHeight();
        ascaris.setCrawling(true);
        h.assertTrue(ascaris.getBbHeight() < standH * 0.6f, "Crawling shrinks hitbox height to squeeze 1-block gaps");
        ascaris.setCrawling(false);

        // 2. 亲密缠绕缓解宫寒并可通过潜行动作松开
        ((Pregnant) (Object) player).setUterineCold(1000);
        h.assertTrue(ascaris.startTailWrap(player, AscarisEntity.WRAP_AFFECTIONATE), "Affectionate tail_wrap starts");
        h.assertTrue(ascaris.getPoseState() == AscarisPoseState.TAIL_WRAP, "Pose switches to TAIL_WRAP");

        h.runAfterDelay(35, () -> {
            h.assertTrue(((Pregnant) (Object) player).getUterineCold() < 1000, "Affectionate tail wrap warms host and relieves uterine cold");
            ascaris.handleWrapActionFromPlayer(player, AscarisActionPayload.ACTION_RELEASE);
            h.assertTrue(ascaris.getPoseState() != AscarisPoseState.TAIL_WRAP, "Player can gently release affectionate wrap");

            // 3. 寄生缠绕连续挣扎可挣脱并让蛔虫娘蜷缩
            h.assertTrue(ascaris.startTailWrap(player, AscarisEntity.WRAP_PARASITIC), "Parasitic tail_wrap starts");
            for (int i = 0; i < 9; i++) {
                ascaris.handleWrapActionFromPlayer(player, AscarisActionPayload.ACTION_STRUGGLE);
            }
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.CURL_UP, "Struggling free curls Ascaris up");

            // 4. 举着宝塔糖触发驱虫药恐惧
            ascaris.setPoseState(AscarisPoseState.NORMAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getPAGODA_CANDY()));
        });

        h.runAfterDelay(55, () -> {
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.CURL_UP, "Holding pagoda candy scares Ascaris into curl_up");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 130)
    public void yandereDewormerInterceptTightenAndSweetPacifyWork(GameTestHelper h) {
        ServerPlayer owner = survivalPlayer(h, 4.5, 4.5);
        AscarisEntity ascaris = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris created");
        ascaris.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ());
        ascaris.setTame(true, true);
        ascaris.tame(owner);
        ascaris.setObsession(70);
        h.getLevel().addFreshEntity(ascaris);

        // 1. 高执念下举起宝塔糖：不再害怕缩团，而是黑化歪头死盯 (yandere_stare + 暗瞳)
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getPAGODA_CANDY()));
        h.runAfterDelay(10, () -> {
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.YANDERE_STARE,
                    "High obsession turns dewormer phobia into yandere_stare");
            h.assertTrue(ascaris.getEyeVariant() == AscarisEntity.EYE_YANDERE
                    || ascaris.getEyeVariant() == AscarisEntity.EYE_YANDERE_HEART,
                    "Yandere stare activates dark/heart eye texture");

            // 2. 启动病娇缠绕 -> 进入爱心瞳 + 切入 idle -> 连按 3 次挣扎触发 yandere_tighten 反扑！
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.assertTrue(ascaris.startTailWrap(owner, AscarisEntity.WRAP_YANDERE), "Yandere wrap starts");
            h.assertTrue(ascaris.getEyeVariant() == AscarisEntity.EYE_YANDERE_HEART, "Yandere wrap switches to heart pupils");
            h.assertTrue(!ascaris.isPushable(), "Wrapping disables entity push-apart collision");
            ascaris.setPoseState(AscarisPoseState.YANDERE_WRAP_IDLE);
            for (int i = 0; i < 3; i++) {
                ascaris.handleWrapActionFromPlayer(owner, AscarisActionPayload.ACTION_STRUGGLE);
            }
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.YANDERE_TIGHTEN,
                    "3rd struggle triggers yandere_tighten counter-squeeze");

            // 3. 喂一颗甜食瞬间融化黑化 -> 恢复亮瞳 + happy 摇摆 + 执念大幅下降！
            int beforeObs = ascaris.getObsession();
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 2));
            ascaris.mobInteract(owner, InteractionHand.MAIN_HAND);
            h.assertTrue(!ascaris.getPoseState().isWrapping(), "Sweet pacification releases yandere wrap");
            h.assertTrue(ascaris.getPoseState() == AscarisPoseState.HAPPY, "Sweet pacification plays happy animation");
            h.assertTrue(ascaris.getEyeVariant() == AscarisEntity.EYE_DEFAULT, "Sweet pacification restores bright starry eyes");
            h.assertTrue(ascaris.getObsession() < beforeObs, "Sweet pacification reduces obsession");

            // 4. 哈气威慑带击退验证 + 配置关闭病娇开关验证
            ascaris.forceShowcasePose(AscarisPoseState.THREATEN, false);
        });

        h.runAfterDelay(25, () -> {
            h.assertTrue(owner.getDeltaMovement().horizontalDistanceSqr() > 0.02 || owner.distanceToSqr(ascaris) > 2.25,
                    "Threaten hiss applies knockback impulse");
            boolean prevCfg = org.cneko.justarod.config.JRConfig.isAscarisYandereEnabled();
            try {
                org.cneko.justarod.config.JRConfig.CONFIG_BUILDER.setBoolean("ascaris.yandere", false);
                ascaris.setObsession(95);
                ascaris.forceShowcasePose(AscarisPoseState.NORMAL, false);
                h.assertTrue(ascaris.getEyeVariant() == AscarisEntity.EYE_DEFAULT,
                        "Disabling ascaris.yandere forces bright default eyes even at high obsession");
                ascaris.startTailWrap(owner, AscarisEntity.WRAP_YANDERE);
                h.assertTrue(ascaris.getWrapMode() != AscarisEntity.WRAP_YANDERE,
                        "Disabling ascaris.yandere blocks yandere wrap mode");
            } finally {
                org.cneko.justarod.config.JRConfig.CONFIG_BUILDER.setBoolean("ascaris.yandere", prevCfg);
            }
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void hostReentryTerritorialEvictionAndDewormerFlushWork(GameTestHelper h) {
        ServerPlayer host = survivalPlayer(h, 4.5, 4.5);
        AscarisEntity first = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(first != null, "First Ascaris created");
        first.setPos(host.getX() + 1.2, host.getY(), host.getZ());
        first.setTame(true, true);
        first.tame(host);
        first.setCustomName(net.minecraft.network.chat.Component.literal("小粉"));
        first.setAffection(66);
        first.setHunger(55);
        h.getLevel().addFreshEntity(first);

        // 1. 主动收回体内寄宿
        h.assertTrue(org.cneko.justarod.ascaris.AscarisHostController.enterHostBody(first, host, "manual"),
                "First tamed Ascaris enters host body");
        h.assertTrue(org.cneko.justarod.ascaris.AscarisHostController.hasHostedAscaris(host), "Host now carries resident Ascaris");
        h.assertTrue(!first.isAlive(), "Entered Ascaris entity removed from world");

        // 2. 领地意识：吞下野生虫卵胶囊不再安静无视，而是把一条野生小虫直接抽飞弹出体外且不累计感染！
        int beforeCount = h.getLevel().getEntitiesOfClass(AscarisEntity.class, host.getBoundingBox().inflate(6.0)).size();
        AscarisInfectionController.onFinishConsuming(host, new ItemStack(JRItems.Companion.getASCARIS_EGG_CAPSULE()));
        h.assertTrue(((Pregnant) (Object) host).getAscaris() == 0, "Territorial resident prevents wild infection");
        int afterCount = h.getLevel().getEntitiesOfClass(AscarisEntity.class, host.getBoundingBox().inflate(6.0)).size();
        h.assertTrue(afterCount > beforeCount, "Territorial eviction ejects wild mini-ascaris out of host");

        // 3. 领地意识：尝试收进第二只已驯服蛔虫娘时，被原住民强行顶飞并缩成一团！
        AscarisEntity second = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(second != null, "Second Ascaris created");
        second.setPos(host.getX() + 1.2, host.getY(), host.getZ());
        second.setTame(true, true);
        second.tame(host);
        h.getLevel().addFreshEntity(second);
        h.assertTrue(!org.cneko.justarod.ascaris.AscarisHostController.enterHostBody(second, host, "manual"),
                "Second Ascaris rejected by resident territorial instinct");
        h.assertTrue(second.getDeltaMovement().horizontalDistanceSqr() > 0.1,
                "Territorial eviction applies outward knockback to second Ascaris");
        h.assertTrue(second.getPoseState() == AscarisPoseState.CURL_UP,
                "Evicted newcomer curls up after being knocked out");

        // 4. 温柔唤出：完整恢复自定义名字、亲密度与饱食度
        AscarisEntity restored = org.cneko.justarod.ascaris.AscarisHostController.summonFromHostBody(host, false, false);
        h.assertTrue(restored != null && !org.cneko.justarod.ascaris.AscarisHostController.hasHostedAscaris(host),
                "Resident summoned back cleanly");
        h.assertTrue(restored.hasCustomName() && "小粉".equals(restored.getCustomName().getString()),
                "Custom name preserved through body hosting");
        h.assertTrue(restored.getAffection() == 66, "Affection preserved through body hosting");

        // 5. 再次收回并用宝塔糖误服：被驱虫药熏出且降亲密
        h.assertTrue(org.cneko.justarod.ascaris.AscarisHostController.enterHostBody(restored, host, "manual"), "Re-enters cleanly");
        AscarisInfectionController.administerDewormer(host, AscarisInfectionController.DRUG_PAGODA, host);
        h.assertTrue(!org.cneko.justarod.ascaris.AscarisHostController.hasHostedAscaris(host),
                "Dewormer flushes hosted resident out of body");
        h.succeed();
    }

    @GameTest(maxTicks = 80)
    public void cuteScaleAttributeAndChestRidingWithBustBounceWork(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h, 4.5, 4.5);
        AscarisEntity ascaris = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris created");
        ascaris.setPos(player.getX() + 1.5, player.getY(), player.getZ());
        ascaris.setTame(true, true);
        ascaris.tame(player);
        h.getLevel().addFreshEntity(ascaris);

        // 1. 逆生长剂与生长剂现在直接驱动 player.cute_scale，并自动联动 SCALE / 移速 / 相机距离
        var revAgent = JRItems.Companion.getREVERSE_GROWTH_AGENT();
        h.assertTrue(revAgent != null, "Reverse growth agent exists");
        org.cneko.justarod.scale.CuteScaleController.setCuteScaleImmediate(player, 0.40);
        float actualCute = org.cneko.justarod.scale.CuteScaleController.getCuteScale(player);
        var actualAttr = player.getAttribute(org.cneko.justarod.JRAttributes.Companion.getPLAYER_CUTE_SCALE());
        h.assertTrue(Math.abs(actualCute - 0.40f) < 0.02f,
                "PLAYER_CUTE_SCALE updates to 0.40 (actual=" + actualCute + ", attr=" + actualAttr + ", scale=" + player.getScale() + ")");
        h.assertTrue(player.getScale() < 0.50f, "Derived SCALE modifier shrinks player smoothly");
        h.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.CAMERA_DISTANCE) < 3.0,
                "Third-person camera distance pulls closer for tiny player");
        h.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) >= 0.48,
                "Tiny player preserves smooth slab/stair step height");

        // 2. 缩小后空手右键把玩家安放到粉白蛔虫娘的胸口上！
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ascaris.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getVehicle() == ascaris, "Shrunk player mounts onto Ascaris-chan's chest");
        double renderedButtRelY = (player.getY() + 0.587 * player.getScale()) - ascaris.getY();
        h.assertTrue(renderedButtRelY >= 0.80 && renderedButtRelY <= 0.96,
                "Sitting player's rendered seat lands on chest shelf (relY=" + renderedButtRelY + ")");

        // 3. 触发跳跃并验证胸部弹簧产生明显晃动、玩家 Y 同步随胸口起伏！
        double yBefore = player.getY();
        ascaris.handleChestHopFromPassenger(player);
        h.runAfterDelay(4, () -> {
            h.assertTrue(Math.abs(ascaris.getBustBounceY(1.0f)) > 0.02f,
                    "Jumping induces damped spring bust bounce");
            h.assertTrue(Math.abs(player.getY() - yBefore) > 0.05,
                    "Player riding on chest bounces together with Ascaris-chan");
            // 4. 恢复正常体型后自动从胸口放回地面
            org.cneko.justarod.scale.CuteScaleController.setCuteScaleImmediate(player, 1.0);
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue(player.getVehicle() == null, "Growing back to 1.0 auto-dismounts player cleanly from chest");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void singleEggCapsuleAndLastFoodFinishUsingItemWork(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h, 4.5, 4.5);
        Pregnant p = (Pregnant) (Object) player;
        h.assertTrue(p.getAscaris() == 0, "Player starts uninfected");

        // 1. 生存模式吃下唯一 1 粒蛔虫卵胶囊（count 1 -> 0）：必须正常触发 Ⅰ 期感染
        ItemStack firstCapsule = new ItemStack(JRItems.Companion.getASCARIS_EGG_CAPSULE(), 1);
        firstCapsule.finishUsingItem(h.getLevel(), player);
        h.assertTrue(firstCapsule.isEmpty(), "Single capsule is consumed down to empty in survival");
        h.assertTrue(AscarisInfectionController.stageOf(p.getAscaris()) == 1,
                "Eating last egg capsule via finishUsingItem infects player to Stage 1");
        h.assertTrue(player.hasEffect(org.cneko.justarod.effect.JREffects.Companion.getASCARIS_INFECTION_EFFECT()),
                "Ascaris infection effect is applied immediately");

        // 2. 再吃 1 粒单颗胶囊 -> 推进到 Ⅱ 期
        ItemStack secondCapsule = new ItemStack(JRItems.Companion.getASCARIS_EGG_CAPSULE(), 1);
        secondCapsule.finishUsingItem(h.getLevel(), player);
        h.assertTrue(secondCapsule.isEmpty(), "Second single capsule is consumed");
        h.assertTrue(AscarisInfectionController.stageOf(p.getAscaris()) == 2,
                "Second single egg capsule advances infection to Stage 2");

        // 3. Ⅲ 期吃掉手里最后 1 块熟牛排（+8 饱食）：先恢复再偷吃 40%（3 点），10 + 8 - 3 = 15
        p.setAscaris(AscarisInfectionController.STAGE_3_TICKS);
        player.getFoodData().setFoodLevel(10);
        ItemStack lastBeef = new ItemStack(Items.COOKED_BEEF, 1);
        lastBeef.finishUsingItem(h.getLevel(), player);
        h.assertTrue(lastBeef.isEmpty(), "Last cooked beef is consumed");
        h.assertTrue(player.getFoodData().getFoodLevel() == 15,
                "Stage 3 steals nutrition after eating last food stack (actual=" + player.getFoodData().getFoodLevel() + ")");

        h.succeed();
    }

    @GameTest(maxTicks = 80)
    public void ecdysisMoltingMoltItemAndAnthelminticResistanceWork(GameTestHelper h) {
        ServerPlayer owner = survivalPlayer(h, 4.5, 4.5);
        AscarisEntity ascaris = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris created");
        ascaris.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ());
        ascaris.setTame(true, true);
        ascaris.tame(owner);
        h.getLevel().addFreshEntity(ascaris);

        // 1. 饮食风味余韵 + 蜕皮成长周期（Ecdysis）
        int startStage = ascaris.getMoltStage();
        double startMaxHp = ascaris.getMaxHealth();
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOW_BERRIES, 1));
        ascaris.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(ascaris.getFlavorBuff() == 1 && ascaris.getMoltProgress() > 0,
                "Glow berries activate glow night-light flavor buff and advance molt progress");

        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BOTTLE, 1));
        ascaris.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(ascaris.getFlavorBuff() == 2 && ascaris.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION),
                "Honey bottle activates honey-gold flavor buff and Absorption");

        ascaris.advanceMoltProgress(h.getLevel(), 100);
        h.assertTrue(ascaris.getMoltStage() == startStage + 1 && ascaris.getMaxHealth() > startMaxHp,
                "Ecdysis advances molt stage and increases max health");
        var droppedMolts = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                ascaris.getBoundingBox().inflate(4.0),
                it -> it.getItem().is(JRItems.Companion.getASCARIS_MOLT()));
        h.assertTrue(!droppedMolts.isEmpty(), "Ecdysis sheds a fresh Ascaris Molt item");

        // 2. 「粉白虫蜕」右键涂覆耐酸角质护膜：清毒 + 暖腹余温 + 抗性 + 水下呼吸
        owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200, 0));
        owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 200, 0));
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getASCARIS_MOLT(), 1));
        owner.getMainHandItem().use(h.getLevel(), owner, InteractionHand.MAIN_HAND);
        h.assertTrue(!owner.hasEffect(net.minecraft.world.effect.MobEffects.POISON)
                        && !owner.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)
                        && owner.hasEffect(org.cneko.justarod.effect.JREffects.Companion.getWARM_BELLY_EFFECT())
                        && owner.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE)
                        && owner.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING),
                "Ascaris Molt item coats player in acid-resistant cuticle barrier");

        // 3. 宝塔糖（哌嗪）抗药性演化：第1次软倒 -> 第2次抗拒蜷缩 -> 第3次完全免疫；而阿苯达唑仍具强效！
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getPAGODA_CANDY(), 3));
        ascaris.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(ascaris.getPoseState() == AscarisPoseState.COLLAPSE && ascaris.getPiperazineResistance() == 35,
                "1st Pagoda Candy collapses Ascaris and raises Piperazine resistance to 35%");

        ascaris.setPoseState(AscarisPoseState.NORMAL);
        ascaris.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(ascaris.getPoseState() == AscarisPoseState.CURL_UP && ascaris.getPiperazineResistance() == 70,
                "2nd Pagoda Candy is resisted with curl_up and raises resistance to 70%");

        int obsBeforeImmune = ascaris.getObsession();
        ascaris.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(ascaris.getPoseState() != AscarisPoseState.COLLAPSE && ascaris.getObsession() > obsBeforeImmune,
                "3rd Pagoda Candy triggers full Piperazine immunity and raises obsession");

        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getALBENDAZOLE(), 1));
        ascaris.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(ascaris.getPoseState() == AscarisPoseState.COLLAPSE
                        && ascaris.getExpelledStage() == AscarisEntity.EXPELLED_ALBENDAZOLE_CRITICAL,
                "Albendazole bypasses Piperazine resistance due to distinct drug mechanism");

        h.succeed();
    }

    @GameTest(maxTicks = 80)
    public void hostedAcidShieldCombatBindItemFetchAndSlimeEscapeWork(GameTestHelper h) {
        ServerPlayer owner = survivalPlayer(h, 4.5, 4.5);
        AscarisEntity ascaris = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris created");
        ascaris.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ());
        ascaris.setTame(true, true);
        ascaris.tame(owner);
        ascaris.setAffection(50);
        ascaris.setHunger(40);
        h.getLevel().addFreshEntity(ascaris);

        // 1. 战术长尾控场：定身苦力怕并打断引爆！
        var creeper = net.minecraft.world.entity.EntityType.CREEPER.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(creeper != null, "Creeper created");
        creeper.setPos(owner.getX() + 2.5, owner.getY(), owner.getZ());
        creeper.setSwellDir(1);
        h.getLevel().addFreshEntity(creeper);
        h.assertTrue(ascaris.performCombatTailBind(h.getLevel(), creeper), "Combat tail-bind executes cleanly");
        h.assertTrue(creeper.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)
                        && creeper.getSwellDir() < 0,
                "Combat tail-bind immobilizes Creeper and defuses swell");
        creeper.discard();

        // 2. “蛔虫喜钻孔”狭缝卷物小助手：拾取掉落物交回主人背包
        var diamondDrop = new net.minecraft.world.entity.item.ItemEntity(
                h.getLevel(), ascaris.getX() + 0.5, ascaris.getY(), ascaris.getZ(), new ItemStack(Items.DIAMOND, 2));
        h.getLevel().addFreshEntity(diamondDrop);
        h.assertTrue(ascaris.tryFetchNearbyItem(h.getLevel(), diamondDrop)
                        && owner.getInventory().countItem(Items.DIAMOND) >= 2,
                "Ascaris fetches dropped item and hands it to owner");

        // 3. 寄宿耐酸代消化盾：携带寄宿蛔虫娘吃腐肉/毒物自动中和毒素并滋养她
        h.assertTrue(org.cneko.justarod.ascaris.AscarisHostController.enterHostBody(ascaris, owner, "manual"),
                "Ascaris enters owner body");
        owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 200, 0));
        owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200, 0));
        org.cneko.justarod.ascaris.AscarisHostController.onHostFinishedEating(owner, new ItemStack(Items.ROTTEN_FLESH));
        h.assertTrue(!owner.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)
                        && !owner.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                "Hosted Ascaris acid-shield neutralizes Hunger & Poison from unclean food");

        AscarisEntity summoned = org.cneko.justarod.ascaris.AscarisHostController.summonFromHostBody(owner, false, false);
        h.assertTrue(summoned != null && summoned.getHunger() > 40, "Hosted Ascaris gained hunger from neutralized food");

        // 4. 跨系统史莱姆耐酸角质彩蛋：被史莱姆吞下后免疫消化并疯狂扭动把史莱姆撑得打嗝吐出！
        var slime = JREntities.TAMED_SLIME.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(slime != null, "Tamed slime created");
        slime.setPos(owner.getX() + 3.0, owner.getY(), owner.getZ() + 2.0);
        slime.setOwner(owner);
        slime.setTierLevel(3);
        h.getLevel().addFreshEntity(slime);
        h.assertTrue(slime.trySwallow(summoned), "Slime swallows Ascaris-chan");
        h.assertTrue(!slime.hasDigestibleContent(), "Acid-resistant cuticle protects Ascaris-chan from Slime digestion");

        h.runAfterDelay(45, () -> {
            h.assertTrue(summoned.isAlive() && summoned.getVehicle() == null,
                    "Ascaris-chan wriggles furiously and forces Slime to burp her out alive");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void v53ModelBonesAndNewAnimationsWork(GameTestHelper h) {
        // 1. 校验 v53 精简几何（83 骨含自定义 bust 物理 3 骨 / 1,177 方块）与 49 段动画资源
        try (var geoIn = AscarisEntity.class.getResourceAsStream("/assets/justarod/geckolib/models/entity/ascaris.geo.json");
             var animIn = AscarisEntity.class.getResourceAsStream("/assets/justarod/geckolib/animations/entity/ascaris.animation.json")) {
            h.assertTrue(geoIn != null && animIn != null, "v53 geo and animation assets exist on classpath");
            var geoRoot = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(geoIn, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var bones = geoRoot.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
            int totalCubes = 0;
            String bustParent = null, bustLeftParent = null, bustRightParent = null;
            for (var el : bones) {
                var b = el.getAsJsonObject();
                String name = b.get("name").getAsString();
                if (b.has("cubes")) totalCubes += b.getAsJsonArray("cubes").size();
                if ("bust".equals(name)) bustParent = b.get("parent").getAsString();
                if ("bust_left".equals(name)) bustLeftParent = b.get("parent").getAsString();
                if ("bust_right".equals(name)) bustRightParent = b.get("parent").getAsString();
            }
            h.assertTrue(bones.size() == 83 && totalCubes == 1177,
                    "v53 geo keeps 83 bones (incl. 3 custom bust bones) and 1177 cubes (got bones="
                            + bones.size() + ", cubes=" + totalCubes + ")");
            h.assertTrue("body".equals(bustParent) && "bust".equals(bustLeftParent) && "bust".equals(bustRightParent),
                    "Custom bust bone hierarchy intact: body -> bust -> bust_left/bust_right");

            var animRoot = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(animIn, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var anims = animRoot.getAsJsonObject("animations");
            h.assertTrue(anims.size() == 49, "v53 animation bundle contains all 49 animations (got " + anims.size() + ")");
            for (String req : List.of("animation.ascaris.boat", "animation.ascaris.ride",
                    "animation.ascaris.swim", "animation.ascaris.swim_stand", "animation.ascaris.jump",
                    "animation.ascaris.ladder_up", "animation.ascaris.ladder_stillness", "animation.ascaris.ladder_down",
                    "animation.ascaris.sneak", "animation.ascaris.sneaking", "animation.ascaris.hurt")) {
                h.assertTrue(anims.has(req), "Missing expected v53 animation: " + req);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed validating v53 Ascaris assets", e);
        }

        // 2. 验证船只鸭子坐、骑乘生物、跳跃、游泳与爬梯的自动动画解析与姿态命令映射
        ServerPlayer player = survivalPlayer(h, 4.5, 4.5);
        AscarisEntity ascaris = JREntities.ASCARIS.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris created");
        ascaris.setPos(player.getX() + 1.5, player.getY(), player.getZ());
        ascaris.setNoAi(true);
        h.getLevel().addFreshEntity(ascaris);

        var boat = net.minecraft.world.entity.EntityType.CHERRY_BOAT.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(boat != null, "Cherry boat created");
        boat.setPos(ascaris.getX(), ascaris.getY(), ascaris.getZ());
        h.getLevel().addFreshEntity(boat);
        h.assertTrue(ascaris.startRiding(boat, true, true), "Ascaris boards boat cleanly");
        h.assertTrue("animation.ascaris.boat".equals(ascaris.resolveActiveAnimationName(false)),
                "Riding a boat selects v53 W-sit boat animation (got " + ascaris.resolveActiveAnimationName(false) + ")");
        h.assertTrue(Math.abs(ascaris.getVehicleAttachmentPoint(boat).y - 0.58) < 0.02,
                "Boat vehicle attachment point calibrated for W-sit pose");
        ascaris.stopRiding();
        boat.discard();

        var pig = net.minecraft.world.entity.EntityType.PIG.create(h.getLevel(), EntitySpawnReason.COMMAND);
        h.assertTrue(pig != null, "Pig mount created");
        pig.setPos(ascaris.getX(), ascaris.getY(), ascaris.getZ());
        h.getLevel().addFreshEntity(pig);
        h.assertTrue(ascaris.startRiding(pig, true, true), "Ascaris rides living mount cleanly");
        h.assertTrue("animation.ascaris.ride".equals(ascaris.resolveActiveAnimationName(false)),
                "Riding a living mount selects v53 ride animation (got " + ascaris.resolveActiveAnimationName(false) + ")");
        ascaris.stopRiding();
        pig.discard();

        // 跳跃滞空动画判定
        ascaris.setOnGround(false);
        ascaris.setDeltaMovement(0, 0.35, 0);
        h.assertTrue("animation.ascaris.jump".equals(ascaris.resolveActiveAnimationName(false)),
                "Airborne jump selects v53 jump animation");
        ascaris.setDeltaMovement(Vec3.ZERO);
        ascaris.setOnGround(true);

        // 水体游泳 / 踩水判定
        BlockPos waterPos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(waterPos, Blocks.WATER.defaultBlockState());
        ascaris.setPos(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5);
        ascaris.tick();
        ascaris.setDeltaMovement(Vec3.ZERO);
        h.assertTrue("animation.ascaris.swim_stand".equals(ascaris.resolveActiveAnimationName(false)),
                "Stationary in water selects swim_stand (got " + ascaris.resolveActiveAnimationName(false) + ")");
        h.assertTrue("animation.ascaris.swim".equals(ascaris.resolveActiveAnimationName(true)),
                "Moving in water selects swim (got " + ascaris.resolveActiveAnimationName(true) + ")");

        // 爬梯判定
        BlockPos ladderWall = h.absolutePos(new BlockPos(6, 2, 2));
        BlockPos ladderPos = h.absolutePos(new BlockPos(6, 2, 3));
        h.getLevel().setBlockAndUpdate(ladderWall, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(ladderPos, Blocks.LADDER.defaultBlockState());
        ascaris.setPos(ladderPos.getX() + 0.5, ladderPos.getY(), ladderPos.getZ() + 0.5);
        ascaris.tick();
        ascaris.setDeltaMovement(0, 0.12, 0);
        h.assertTrue("animation.ascaris.ladder_up".equals(ascaris.resolveActiveAnimationName(false)),
                "Climbing up ladder selects ladder_up (got " + ascaris.resolveActiveAnimationName(false) + ")");
        ascaris.setDeltaMovement(0, -0.12, 0);
        h.assertTrue("animation.ascaris.ladder_down".equals(ascaris.resolveActiveAnimationName(false)),
                "Climbing down ladder selects ladder_down (got " + ascaris.resolveActiveAnimationName(false) + ")");
        ascaris.setDeltaMovement(Vec3.ZERO);
        h.assertTrue("animation.ascaris.ladder_stillness".equals(ascaris.resolveActiveAnimationName(false)),
                "Holding on ladder selects ladder_stillness (got " + ascaris.resolveActiveAnimationName(false) + ")");

        // 命令姿态解析
        for (String name : List.of("boat", "ride", "swim", "swim_stand", "jump", "sneak", "sneaking",
                "ladder_up", "ladder_stillness", "ladder_down", "hurt", "fly", "elytra")) {
            AscarisPoseState st = AscarisPoseState.byName(name);
            h.assertTrue(st != null, "Pose name resolves cleanly: " + name);
            ascaris.setPoseState(st);
            h.assertTrue(("animation.ascaris." + name).equals(ascaris.resolveActiveAnimationName(false)),
                    "Explicit pose " + name + " maps to animation.ascaris." + name);
        }

        h.succeed();
    }

    @GameTest(maxTicks = 100)
    public void tamedFatalProtectionCrossDistanceRetreatDormantAndExtremeKillsWork(GameTestHelper h) {
        ServerPlayer owner = survivalPlayer(h, 2.5, 4.5);
        var level = h.getLevel();
        var server = level.getServer();
        var regionData = org.cneko.justarod.body.BodyRegionData.get(server);

        // 1. 已驯服蛔虫娘在远距离（> 18 格）遭遇致命常规伤害 -> 跨距离紧急钻回主人体内避难，完整保留体内世界槽位！
        AscarisEntity farAscaris = JREntities.ASCARIS.create(level, EntitySpawnReason.COMMAND);
        h.assertTrue(farAscaris != null, "Far Ascaris created");
        farAscaris.setPos(owner.getX() + 20.0, owner.getY(), owner.getZ());
        farAscaris.setTame(true, true);
        farAscaris.tame(owner);
        level.addFreshEntity(farAscaris);

        java.util.UUID origRegionOwner = farAscaris.getBodyRegionOwnerUuid();
        long origSlot = regionData.regionFor(origRegionOwner);

        farAscaris.hurtServer(level, farAscaris.damageSources().generic(), 120.0f);
        h.assertTrue(org.cneko.justarod.ascaris.AscarisHostController.hasHostedAscaris(owner),
                "Fatal normal damage at long distance retreats tamed Ascaris into owner's body");
        h.assertTrue(regionData.currentRegion(origRegionOwner) != null && regionData.currentRegion(origRegionOwner) == origSlot,
                "Emergency retreat preserves her inner body world slot");

        // 2. 唤出后（autoEnterCooldown > 0 阻止立刻钻回）遭遇致命常规伤害 -> 原地进入安全重伤休眠态，清火/清怪仇恨，连续 100 伤害打不死！
        AscarisEntity summoned = org.cneko.justarod.ascaris.AscarisHostController.summonFromHostBody(owner, false, false);
        h.assertTrue(summoned != null && summoned.isAlive(), "Summoned Ascaris back out");
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        h.assertTrue(zombie != null, "Zombie created");
        zombie.setPos(summoned.getX() + 1.5, summoned.getY(), summoned.getZ());
        zombie.setTarget(summoned);
        level.addFreshEntity(zombie);

        summoned.igniteForSeconds(5);
        summoned.hurtServer(level, summoned.damageSources().mobAttack(zombie), 100.0f);
        h.assertTrue(summoned.isAlive() && summoned.isDormantDowned() && summoned.getPoseState() == AscarisPoseState.COLLAPSE,
                "When retreat is unavailable, lethal normal hit puts tamed Ascaris into safe dormant collapse");
        h.assertTrue(!summoned.isOnFire() && zombie.getTarget() != summoned,
                "Entering safe dormant state clears fire and drops mob aggro");

        // 休眠期间再次吃高额常规伤害：完全免疫，体内世界丝毫不受影响！
        boolean hurtWhileDormant = summoned.hurtServer(level, summoned.damageSources().mobAttack(zombie), 100.0f);
        h.assertTrue(!hurtWhileDormant && summoned.isAlive() && summoned.isDormantDowned(),
                "Safe dormant Ascaris is immune to follow-up normal damage");
        h.assertTrue(regionData.currentRegion(origRegionOwner) != null && regionData.currentRegion(origRegionOwner) == origSlot,
                "Body world slot stays completely intact while dormant");
        zombie.discard();

        // 3. 多种唤醒方式：空手轻抚 2 次唤醒 -> 再次休眠 -> 喂甜食立刻唤醒 -> 再次休眠 -> 体内抢救唤醒
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        summoned.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(summoned.isDormantDowned() && summoned.getHealth() >= 5.5f,
                "First gentle headpat heals dormant Ascaris");
        summoned.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(!summoned.isDormantDowned() && summoned.getPoseState() != AscarisPoseState.COLLAPSE,
                "Second gentle headpat wakes dormant Ascaris");

        summoned.setAutoEnterCooldown(200);
        summoned.hurtServer(level, summoned.damageSources().generic(), 100.0f);
        h.assertTrue(summoned.isDormantDowned(), "Enters dormant again on lethal normal hit");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 1));
        summoned.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(!summoned.isDormantDowned() && summoned.getHealth() >= summoned.getMaxHealth() * 0.44f,
                "Sweet treat immediately wakes dormant Ascaris with healthy recovery");

        summoned.setAutoEnterCooldown(200);
        summoned.hurtServer(level, summoned.damageSources().generic(), 100.0f);
        h.assertTrue(summoned.isDormantDowned(), "Enters dormant before internal rescue test");
        h.assertTrue(summoned.recoverFromInternalRescue() && !summoned.isDormantDowned(),
                "Internal body rescue wakes dormant Ascaris");

        // 4. 极端死亡路径：阿苯达唑可击穿休眠保护进入危重毒发并致死；/kill (genericKill) 可直接击杀休眠个体
        summoned.setAutoEnterCooldown(200);
        summoned.hurtServer(level, summoned.damageSources().generic(), 100.0f);
        h.assertTrue(summoned.isDormantDowned(), "Dormant before Albendazole test");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getALBENDAZOLE(), 1));
        summoned.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(!summoned.isDormantDowned()
                        && summoned.getExpelledStage() == AscarisEntity.EXPELLED_ALBENDAZOLE_CRITICAL,
                "Albendazole pierces dormant protection into critical toxicity");
        summoned.hurtServer(level, summoned.damageSources().magic(), 10.0f);
        h.assertTrue(!summoned.isAlive(), "Albendazole-critical tamed Ascaris can truly die");

        AscarisEntity killTarget = JREntities.ASCARIS.create(level, EntitySpawnReason.COMMAND);
        h.assertTrue(killTarget != null, "Second tamed Ascaris created");
        killTarget.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ());
        killTarget.setTame(true, true);
        killTarget.tame(owner);
        killTarget.setAutoEnterCooldown(200);
        level.addFreshEntity(killTarget);
        killTarget.hurtServer(level, killTarget.damageSources().generic(), 100.0f);
        h.assertTrue(killTarget.isDormantDowned(), "Second Ascaris enters dormant");
        killTarget.hurtServer(level, killTarget.damageSources().genericKill(), Float.MAX_VALUE);
        h.assertTrue(!killTarget.isAlive(), "/kill (genericKill) cleanly kills dormant tamed Ascaris");

        // 5. 未驯服野生蛔虫娘仍会正常被常规致命伤击杀
        AscarisEntity wild = JREntities.ASCARIS.create(level, EntitySpawnReason.COMMAND);
        h.assertTrue(wild != null, "Wild Ascaris created");
        wild.setPos(owner.getX() + 2.5, owner.getY(), owner.getZ());
        level.addFreshEntity(wild);
        wild.hurtServer(level, wild.damageSources().generic(), 100.0f);
        h.assertTrue(!wild.isAlive(), "Untamed Ascaris still dies normally to regular lethal damage");

        h.succeed();
    }
}
