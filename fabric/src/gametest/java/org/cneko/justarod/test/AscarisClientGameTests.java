package org.cneko.justarod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.block.ascaris.AscarisChymeBlock;
import org.cneko.justarod.block.ascaris.AscarisExcretoryMembraneBlock;
import org.cneko.justarod.block.ascaris.AscarisNerveCordBlock;
import org.cneko.justarod.body.AscarisAnatomyBuilder;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.AscarisCoelomocyteEntity;
import org.cneko.justarod.body.AscarisNeuralLinkController;
import org.cneko.justarod.body.BodyRegionData;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.AscarisActionPayload;

import java.util.UUID;

/**
 * 粉白蛔虫娘体内真实解剖维度、神经视窗遥控、活体随身小家、风味食糜与病娇逃生机制的客户端端到端实机测试。
 */
public class AscarisClientGameTests implements FabricClientGameTest {

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Object oldSquash = context.computeOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().get("fun.entity_squash.enabled"));
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().set("fun.entity_squash.enabled", false));
            var server = world.getServer();

            // 1. 生成驯服粉白蛔虫娘并从口腔进入她的真实解剖体内维度
            UUID ascarisUuid = server.computeOnServer(game -> {
                var level = game.overworld();
                var player = game.getPlayerList().getPlayers().getFirst();
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                    }
                }
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(0.5, 80, -2.5);

                AscarisEntity ascaris = JREntities.ASCARIS.create(level, EntitySpawnReason.COMMAND);
                check(ascaris != null, "Ascaris entity creates cleanly");
                ascaris.setPos(0.5, 80, 0.2);
                ascaris.setTame(true, true);
                ascaris.tame(player);
                ascaris.setObsession(65);
                ascaris.setNoAi(true);
                level.addFreshEntity(ascaris);

                boolean entered = BodyWorld.enterAscaris(player, ascaris, BodyWorld.MOUTH, false);
                check(entered && player.level().dimension().equals(BodyWorld.DIMENSION),
                        "Player enters Ascaris-chan's anatomical body dimension via mouth");

                long slot = BodyRegionData.get(game).currentRegion(ascaris.getBodyRegionOwnerUuid());
                int cx = BodyRegionData.centerX(slot);
                int cz = BodyRegionData.centerZ(slot);
                var body = game.getLevel(BodyWorld.DIMENSION);
                check(body != null, "Body dimension is loaded");

                // 校验 14 种自定义方块解剖地标与无铃铛
                check(body.getBlockState(new BlockPos(cx, AscarisAnatomyBuilder.FLOOR_Y, cz)).is(JRBlocks.ASCARIS_NERVE_CORD),
                        "Pseudocoelom center marker uses custom nerve cord");
                check(body.getBlockState(new BlockPos(cx, AscarisAnatomyBuilder.FLOOR_Y - 1, cz)).is(Blocks.BEDROCK),
                        "Region sentinel bedrock placed cleanly");
                check(body.getBlockState(new BlockPos(cx + AscarisAnatomyBuilder.NERVE_RING_X, AscarisAnatomyBuilder.GUT_FLOOR_Y + 5, cz))
                        .is(JRBlocks.ASCARIS_NERVE_CORD), "Circumenteric nerve ring uses custom nerve cord");
                check(!body.getBlockState(new BlockPos(cx + AscarisAnatomyBuilder.NERVE_RING_X, AscarisAnatomyBuilder.GUT_FLOOR_Y + 4, cz))
                        .is(Blocks.BELL), "No bell block exists inside Ascaris-chan's body");
                check(body.getBlockState(new BlockPos(cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X, AscarisAnatomyBuilder.FLOOR_Y, cz))
                        .is(JRBlocks.ASCARIS_EXCRETORY_MEMBRANE), "H-shaped excretory bridge uses custom membrane");
                check(body.getBlockState(new BlockPos(cx + 1, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, cz + 4))
                        .is(JRBlocks.ASCARIS_INTESTINAL_MEMBRANE), "Intestine wall uses custom translucent epithelium");
                check(body.getBlockState(new BlockPos(cx, AscarisAnatomyBuilder.FLOOR_Y + 1, cz + 11))
                        .is(JRBlocks.ASCARIS_EGG_SAC), "Paired uterus contains custom egg sacs");
                check(body.getBlockState(new BlockPos(cx + 2, AscarisAnatomyBuilder.FLOOR_Y + 1, cz + 2))
                        .is(JRBlocks.ASCARIS_WARM_CUSHION), "Warm body cushion nook present in pseudocoelom");
                check(body.getBlockState(new BlockPos(cx - 20, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz + 3))
                        .is(JRBlocks.ASCARIS_MICROVILLI), "Fluffy pink microvilli line straight intestine");

                var cells = body.getEntitiesOfClass(AscarisCoelomocyteEntity.class,
                        new AABB(cx - 40, AscarisAnatomyBuilder.FLOOR_Y - 2, cz - 20,
                                cx + 40, AscarisAnatomyBuilder.FLOOR_Y + 20, cz + 20));
                check(cells.size() >= 5, "Living floating coelomocytes spawned inside pseudocoelom");

                // 校验多风味甜食食糜：发光浆果 -> 夜视；蜂蜜瓶 -> 伤害吸收
                BlockPos chymePos = new BlockPos(cx - 6, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz + 2);
                player.getFoodData().setFoodLevel(10);
                body.getBlockState(chymePos).useWithoutItem(body, player,
                        new BlockHitResult(Vec3.atCenterOf(chymePos), Direction.UP, chymePos, false));
                check(body.getBlockState(chymePos).getValue(AscarisChymeBlock.BITES) == 1, "Chyme bite count incremented");
                BodyWorld.onAscarisFedSweet(ascaris, new ItemStack(Items.GLOW_BERRIES));
                check(body.getBlockState(chymePos).getValue(AscarisChymeBlock.FLAVOR) == AscarisChymeBlock.FLAVOR_GLOW,
                        "Feeding glow berries spawns glow-berry flavored chyme");
                var initPhys = BodyRegionData.get(game).physiologyFor(slot);
                check(!initPhys.boluses.isEmpty() && initPhys.pharynxSuctionTicks > 0,
                        "Feeding sweet launches pharynx suction pump and moving digestive chyme bolus");
                body.getBlockState(chymePos).useWithoutItem(body, player,
                        new BlockHitResult(Vec3.atCenterOf(chymePos), Direction.UP, chymePos, false));
                check(player.hasEffect(MobEffects.NIGHT_VISION), "Eating glow-berry chyme grants Night Vision");

                // 校验 H 形排泄管膜排毒净化 & 子宫卵囊采集再生
                BlockPos excPos = new BlockPos(cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X, AscarisAnatomyBuilder.FLOOR_Y, cz);
                AscarisExcretoryMembraneBlock.clearCooldown(player.getUUID());
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
                body.getBlockState(excPos).useWithoutItem(body, player,
                        new BlockHitResult(Vec3.atCenterOf(excPos), Direction.UP, excPos, false));
                check(!player.hasEffect(MobEffects.POISON), "Excretory bridge clears Poison inside body");

                BlockPos eggPos = new BlockPos(cx, AscarisAnatomyBuilder.FLOOR_Y + 1, cz + 11);
                body.getBlockState(eggPos).useWithoutItem(body, player,
                        new BlockHitResult(Vec3.atCenterOf(eggPos), Direction.UP, eggPos, false));
                check(body.getBlockState(eggPos).isAir()
                                && player.getInventory().countItem(JRItems.Companion.getASCARIS_EGG_CAPSULE()) >= 1,
                        "Harvesting uterine egg sac yields egg capsule");
                check(AscarisBodyInteractions.tryRegenerateUterineEggSac(body, slot)
                                && body.getBlockState(eggPos).is(JRBlocks.ASCARIS_EGG_SAC),
                        "Well-fed uterus regenerates egg sac cleanly");

                return ascaris.getUUID();
            });

            context.waitFor(mc -> mc.level != null && mc.level.dimension().equals(BodyWorld.DIMENSION));
            context.runOnClient(mc -> {
                if (mc.player != null) {
                    mc.player.setYRot(-90f);
                    mc.player.setXRot(-2f);
                }
            });
            context.waitTicks(12);
            check(context.computeOnClient(mc -> org.cneko.justarod.client.ascaris.AscarisClientState.hasPhysiology()
                            && org.cneko.justarod.client.ascaris.AscarisClientState.physPressure() > 30
                            && org.cneko.justarod.client.ascaris.AscarisClientState.physTempC() >= 34.0f),
                    "Client receives live hardcore physiology HUD sync inside Ascaris-chan's body");
            context.takeScreenshot("ascaris-anatomy-pharynx-nervering");

            // 2. 切至体温暖窝截屏（验证暖桃色体温微晕 + 暖窝软垫 + 活体体腔小细胞）并验证生存 J 禁用 / 病娇攻击兴奋 / 器官自愈不碰箱子 / 软垫小睡跳夜！
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                var visit = BodyRegionData.get(game).visit(player.getUUID());
                check(visit != null, "Visit active in body dimension");
                int cx = BodyRegionData.centerX(visit.slot());
                int cz = BodyRegionData.centerZ(visit.slot());
                player.teleportTo(cx + 0.5, AscarisAnatomyBuilder.FLOOR_Y + 1, cz + 0.5);
            });
            context.runOnClient(mc -> {
                if (mc.player != null) {
                    mc.player.setYRot(-90f);
                    mc.player.setXRot(-8f);
                }
            });
            context.waitTicks(24);
            context.takeScreenshot("ascaris-anatomy-warm-nook");

            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                var body = game.getLevel(BodyWorld.DIMENSION);
                check(body != null, "Body dimension loaded");
                var visit = BodyRegionData.get(game).visit(player.getUUID());
                check(visit != null && visit.hostObsession() >= 50, "High-obsession yandere visit starts locked");
                int cx = BodyRegionData.centerX(visit.slot());
                int cz = BodyRegionData.centerZ(visit.slot());

                // 2a. 生存模式按 J 退出被禁用
                check(!BodyWorld.requestHotkeyExit(player) && player.level().dimension().equals(BodyWorld.DIMENSION),
                        "Pressing J to exit Ascaris-chan is disabled in Survival mode");

                // 2b. 病娇高执念下口腔自然滑出被拒绝
                BlockPos lipPos = new BlockPos(cx - 42, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz);
                AscarisBodyInteractions.tryNaturalExitAtOrifice(player, lipPos);
                check(player.level().dimension().equals(BodyWorld.DIMENSION),
                        "Yandere Ascaris refuses orifice exit while obsession >= 50%");

                // 2c. 破坏体内组织 + 攻击体腔细胞在病娇模式下反而让执念飙升！
                int obsBeforeViolence = BodyRegionData.get(game).visit(player.getUUID()).hostObsession();
                BlockPos breakWall = new BlockPos(cx + 1, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, cz + 4);
                var wallState = body.getBlockState(breakWall);
                body.setBlockAndUpdate(breakWall, Blocks.AIR.defaultBlockState());
                AscarisBodyInteractions.onAnatomyBlockBroken(player, breakWall, wallState);
                int obsAfterBreak = BodyRegionData.get(game).visit(player.getUUID()).hostObsession();
                check(obsAfterBreak > obsBeforeViolence,
                        "Breaking tissue in yandere mode excites her and increases obsession");
                check(!BodyRegionData.get(game).physiologyFor(visit.slot()).breaches.isEmpty(),
                        "Breaking tissue records hydrostatic tissue breach");

                var cells = body.getEntitiesOfClass(AscarisCoelomocyteEntity.class,
                        new AABB(cx - 40, AscarisAnatomyBuilder.FLOOR_Y - 2, cz - 20,
                                cx + 40, AscarisAnatomyBuilder.FLOOR_Y + 20, cz + 20));
                check(!cells.isEmpty(), "Coelomocytes available for attack reaction check");
                cells.getFirst().hurtServer(body, body.damageSources().playerAttack(player), 1.0f);
                int obsAfterAttack = BodyRegionData.get(game).visit(player.getUUID()).hostObsession();
                check(obsAfterAttack > obsAfterBreak && cells.getFirst().isAlert(),
                        "Attacking coelomocyte in yandere mode increases obsession and alerts immune cells");

                // 2d. 器官温和自愈：放一个玩家箱子在假体腔走廊，用粉白虫蜕右键围咽神经环 -> 破墙愈合且箱子完好无损！
                BlockPos chestPos = new BlockPos(cx + 5, AscarisAnatomyBuilder.FLOOR_Y + 1, cz);
                body.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
                BlockPos ringPos = new BlockPos(cx + AscarisAnatomyBuilder.NERVE_RING_X, AscarisAnatomyBuilder.GUT_FLOOR_Y + 5, cz);
                ItemStack molt = new ItemStack(JRItems.Companion.getASCARIS_MOLT(), 2);
                check(AscarisBodyInteractions.trySelfHealFromNerveRing(player, ringPos, molt),
                        "Using Ascaris molt on nerve ring triggers organ self-healing");
                check(body.getBlockState(breakWall).is(JRBlocks.ASCARIS_INTESTINAL_MEMBRANE),
                        "Organ self-healing restored broken intestinal membrane");
                check(body.getBlockState(chestPos).is(Blocks.CHEST),
                        "Organ self-healing preserved player-placed chest completely untouched");
                check(BodyRegionData.get(game).physiologyFor(visit.slot()).breaches.isEmpty(),
                        "Organ self-healing cleared tracked tissue breaches");

                // 2d.5 卵巢刺激与咽部机械反射验证
                BlockPos ovaryPos = new BlockPos(cx + 26, AscarisAnatomyBuilder.FLOOR_Y + 3, cz + 8);
                check(AscarisBodyInteractions.tryStimulateOvary(player, ovaryPos),
                        "Stimulating coiled ovary advances reproductive cycle");
                BlockPos pharynxStimPos = new BlockPos(cx - 34, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz + 2);
                check(AscarisBodyInteractions.tryStimulatePharynx(player, pharynxStimPos),
                        "Stimulating muscular pharynx triggers swallow/gag reflex arc");

                // 2e. 体温暖窝软垫潜行小睡：跳过主世界午夜、大幅降低执念并赋予「暖腹余温」！
                var overworld = game.overworld();
                overworld.dimensionType().defaultClock().ifPresent(clock ->
                        overworld.clockManager().setTotalTicks(clock, 18000L));
                BlockPos cushionPos = new BlockPos(cx + 2, AscarisAnatomyBuilder.FLOOR_Y + 1, cz + 2);
                check(AscarisBodyInteractions.trySleepInWarmNook(player, cushionPos),
                        "Sneak-sleeping on warm cushion inside body succeeds");
                long morningTod = overworld.getOverworldClockTime() % 24000L;
                check(morningTod < 12000L, "Warm nook sleep skipped Overworld night to morning (tod=" + morningTod + ")");
                check(player.hasEffect(JREffects.Companion.getWARM_BELLY_EFFECT()),
                        "Warm nook sleep granted Warm Belly effect");

                // 再轻抚一次围咽神经环 + 咽部喂蜂蜜瓶 -> 执念彻底降到 50% 以下！
                body.getBlockState(ringPos).useWithoutItem(body, player,
                        new BlockHitResult(Vec3.atCenterOf(ringPos), Direction.DOWN, ringPos, false));
                check(body.getBlockState(ringPos).getValue(AscarisNerveCordBlock.EXCITED),
                        "Soothing nerve ring excites bio-electric pulse");
                BlockPos pharynxWall = new BlockPos(cx - 32, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz + 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BOTTLE, 1));
                check(AscarisBodyInteractions.tryFeedSweetInsidePharynx(player, pharynxWall, player.getMainHandItem()),
                        "Feeding honey bottle inside pharynx succeeds and drops obsession");
                int calmedObs = BodyRegionData.get(game).visit(player.getUUID()).hostObsession();
                check(calmedObs < BodyWorld.YANDERE_LOCK_OBSESSION,
                        "Soothing & sleeping dropped yandere obsession below 50% (now=" + calmedObs + ")");

                // 3. 开启「借她的眼睛看外界」跨维度神经视窗！（单人世界异步加载实体区块后自动附着相机）
                player.teleportTo(cx + AscarisAnatomyBuilder.NERVE_RING_X + 0.5,
                        AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz + 0.5);
                check(AscarisBodyInteractions.tryOpenNeuralWindow(player, ringPos),
                        "Sneak-clicking circumenteric nerve ring starts cross-dimension Neural Eye View");
                check(AscarisNeuralLinkController.isLinked(player.getUUID())
                                && player.gameMode() == GameType.SPECTATOR
                                && player.level().dimension().equals(Level.OVERWORLD),
                        "Neural link enters Overworld spectator camera link");
            });

            context.waitFor(mc -> mc.level != null && mc.level.dimension().equals(Level.OVERWORLD));
            context.waitTicks(18);
            context.takeScreenshot("ascaris-neural-view");

            // 4. 测试神经视窗附着成功 + 体内遥控（坐下 / 钻缝爬行）并断开无缝返回围咽神经环 + 口腔自然滑出！
            int expectedSyncedObs = server.computeOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                check(AscarisNeuralLinkController.isAttached(player.getUUID())
                                && player.getCamera() instanceof AscarisEntity,
                        "Neural link cleanly attached spectator camera to reloaded Overworld Ascaris-chan");
                var ent = game.overworld().getEntity(ascarisUuid);
                check(ent instanceof AscarisEntity, "Ascaris present during neural control");
                AscarisEntity a = (AscarisEntity) ent;

                check(AscarisNeuralLinkController.handleRemoteAction(player, AscarisActionPayload.ACTION_NEURAL_SIT, 0)
                                && a.isOrderedToSit(),
                        "Neural remote sit action makes Overworld Ascaris sit");
                check(AscarisNeuralLinkController.handleRemoteAction(player, AscarisActionPayload.ACTION_NEURAL_CRAWL, 0)
                                && a.isCrawling(),
                        "Neural remote crawl action switches Overworld Ascaris to 1-block crawl");
                check(AscarisNeuralLinkController.handleRemoteAction(player, AscarisActionPayload.ACTION_NEURAL_CRAWL, 0)
                                && !a.isCrawling(),
                        "Neural remote crawl toggle restores standing");
                check(AscarisNeuralLinkController.handleRemoteAction(player, AscarisActionPayload.ACTION_NEURAL_EXIT, 0),
                        "Neural remote exit disconnects cleanly");
                check(!AscarisNeuralLinkController.isLinked(player.getUUID())
                                && player.level().dimension().equals(BodyWorld.DIMENSION)
                                && player.gameMode() == GameType.SURVIVAL,
                        "Disconnecting neural link restores SURVIVAL mode back inside body dimension");

                // 安抚后从口腔三唇自然滑出 -> 顺利回到主世界并获得「暖腹余温」！
                var visit = BodyRegionData.get(game).visit(player.getUUID());
                int obs = visit.hostObsession();
                int cx = BodyRegionData.centerX(visit.slot());
                int cz = BodyRegionData.centerZ(visit.slot());
                BlockPos lipPos = new BlockPos(cx - 42, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz);
                check(AscarisBodyInteractions.tryNaturalExitAtOrifice(player, lipPos)
                                && player.level().dimension().equals(Level.OVERWORLD),
                        "Natural orifice exit succeeds cleanly once yandere obsession < 50%");
                check(player.hasEffect(JREffects.Companion.getWARM_BELLY_EFFECT()),
                        "Voluntary exit grants Warm Belly effect");
                return obs;
            });

            context.waitFor(mc -> mc.level != null && mc.level.dimension().equals(Level.OVERWORLD));
            context.waitTicks(12);

            // 5. 校验重载回主世界后执念同步生效 + 体内穿入器重入 + 外界宝塔糖反向驱虫排出！
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                var ent = game.overworld().getEntity(ascarisUuid);
                check(ent instanceof AscarisEntity, "Ascaris reloaded cleanly in Overworld");
                var a = (AscarisEntity) ent;
                check(a.getObsession() == expectedSyncedObs,
                        "Synced host obsession persisted cleanly onto reloaded Overworld Ascaris (actual="
                                + a.getObsession() + ", expected=" + expectedSyncedObs + ")");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getBODY_INFILTRATOR()));
                check(BodyWorld.enterAscaris(player, a, BodyWorld.ANYWHERE, false)
                                && player.level().dimension().equals(BodyWorld.DIMENSION),
                        "Body infiltrator re-enters Ascaris-chan cleanly");

                // 5a. 硬核药理突破：即便病娇执念 80% 紧锁出口，在体内用宝塔糖触发哌嗪神经麻痹可松弛括约肌并从前 1/3 腹侧阴门滑出！
                var reVisit = BodyRegionData.get(game).visit(player.getUUID());
                check(reVisit != null, "Re-entry visit active");
                BodyWorld.adjustHostStats(game, player, reVisit, 80, 0, 0);
                int rcx = BodyRegionData.centerX(reVisit.slot());
                int rcz = BodyRegionData.centerZ(reVisit.slot());
                ItemStack pagoda = new ItemStack(JRItems.Companion.getPAGODA_CANDY(), 1);
                check(AscarisBodyInteractions.tryAdministerDrugInside(player,
                                new BlockPos(rcx - 32, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, rcz), pagoda),
                        "Administering pagoda candy inside triggers Piperazine paralysis");
                var pState = BodyRegionData.get(game).physiologyFor(reVisit.slot());
                check(pState.isParalyzedByPiperazine() && pState.isSphincterRelaxed(),
                        "Piperazine relaxes sphincters via neuromuscular blockade");
                check(AscarisBodyInteractions.tryNaturalExitAtOrifice(player,
                                new BlockPos(rcx - 16, AscarisAnatomyBuilder.FLOOR_Y + 1, rcz))
                                && player.level().dimension().equals(Level.OVERWORLD),
                        "Piperazine sphincter paralysis allows escaping high-obsession yandere lock via ventral vagina");

                // 清除麻痹后再次穿入以验证外界宝塔糖反向驱虫排出！
                pState.drugType = org.cneko.justarod.body.AscarisPhysiologyState.DRUG_NONE;
                pState.drugTicks = 0;
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getBODY_INFILTRATOR()));
                check(BodyWorld.enterAscaris(player, a, BodyWorld.ANYWHERE, false)
                                && player.level().dimension().equals(BodyWorld.DIMENSION),
                        "Body infiltrator enters again for external dewormer expulsion check");

                int expelled = BodyWorld.expelVisitorsFromAscaris(a, null, false);
                check(expelled == 1 && player.level().dimension().equals(Level.OVERWORLD),
                        "Dewormer on Ascaris-chan expels visitor cleanly back to Overworld");
            });

            context.waitFor(mc -> mc.level != null && mc.level.dimension().equals(Level.OVERWORLD));
            context.waitTicks(6);
        } finally {
            context.runOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().set("fun.entity_squash.enabled", oldSquash));
        }
    }
}
