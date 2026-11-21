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
}
