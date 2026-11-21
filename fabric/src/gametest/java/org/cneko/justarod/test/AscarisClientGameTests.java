package org.cneko.justarod.test;

import com.geckolib.constant.DataTickets;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;
import org.cneko.justarod.ascaris.AscarisPoseState;
import org.cneko.justarod.client.ascaris.AscarisRenderer;
import org.cneko.justarod.client.gui.AbdomenScanScreen;
import org.cneko.justarod.client.gui.ScanType;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.cneko.justarod.packet.XRayScanScreenPayload;

/**
 * 实机客户端测试：验证 GeckoLib 5.5 完整加载粉白蛔虫娘 v48（77骨/19动画），
 * 并在真实游戏画面中渲染站立、趴姿、尾巴缠绕、活体饥饿条和动态腹部 B 超截屏。
 */
public class AscarisClientGameTests implements FabricClientGameTest {

    private static final String[] ALL_26_ANIMATIONS = {
            "animation.ascaris.rest",
            "animation.ascaris.idle",
            "animation.ascaris.blink",
            "animation.ascaris.wave",
            "animation.ascaris.happy",
            "animation.ascaris.look_around",
            "animation.ascaris.sit",
            "animation.ascaris.prone",
            "animation.ascaris.curl_up",
            "animation.ascaris.curl_idle",
            "animation.ascaris.tail_wrap",
            "animation.ascaris.tail_wrap_idle",
            "animation.ascaris.collapse",
            "animation.ascaris.collapse_idle",
            "animation.ascaris.sleep",
            "animation.ascaris.walk",
            "animation.ascaris.run",
            "animation.ascaris.sit_chair",
            "animation.ascaris.crawl",
            "animation.ascaris.yandere_stare",
            "animation.ascaris.yandere_wrap",
            "animation.ascaris.yandere_wrap_idle",
            "animation.ascaris.yandere_tighten",
            "animation.ascaris.stalk",
            "animation.ascaris.threaten",
            "animation.ascaris.bed_watch"
    };

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Object oldSquash = context.computeOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().get("fun.entity_squash.enabled"));
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().set("fun.entity_squash.enabled", false));
            var server = world.getServer();
            int id = server.computeOnServer(game -> {
                var level = game.overworld();
                var player = game.getPlayerList().getPlayers().getFirst();
                for (int x = -6; x <= 6; x++) {
                    for (int z = -6; z <= 6; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                    }
                }
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(0.5, 80, -3.2);
                player.setYRot(0f);
                player.setXRot(10f);
                ((Pregnant) (Object) player).setAscaris(AscarisInfectionController.STAGE_3_TICKS);

                AscarisEntity ascaris = JREntities.ASCARIS.create(level, EntitySpawnReason.COMMAND);
                check(ascaris != null, "Ascaris entity can be created");
                ascaris.setPos(0.5, 80, 0.2);
                ascaris.setYRot(180f);
                ascaris.setYBodyRot(180f);
                ascaris.setYHeadRot(180f);
                ascaris.setTame(true, true);
                ascaris.tame(player);
                ascaris.setNoAi(true);
                level.addFreshEntity(ascaris);
                return ascaris.getId();
            });

            context.waitFor(mc -> mc.level != null && mc.level.getEntity(id) instanceof AscarisEntity);

            // 1. 校验 GeckoLib 5.5 模型烘焙 + 全部 19 个动画存在 + 渲染状态抽取正常
            context.runOnClient(mc -> {
                var ascaris = (AscarisEntity) mc.level.getEntity(id);
                var renderer = (AscarisRenderer) mc.getEntityRenderDispatcher().getRenderer(ascaris);
                var model = renderer.getGeoModel();
                for (String anim : ALL_26_ANIMATIONS) {
                    check(model.getBakedAnimation(ascaris, anim) != null, "Ascaris v49 animation loaded: " + anim);
                }
                var state = (AscarisRenderer.State) mc.getEntityRenderDispatcher().extractEntity(ascaris, 0.5f);
                var baked = model.getBakedModel(model.getModelResource(state));
                check(!baked.isMissingno(),
                        "Ascaris v49 83-bone geo model baked cleanly without missingno fallback");
                check(baked.getBone("bust").isPresent(), "Split bust bone present in baked model");
                check(baked.getBone("bust_left").isPresent(), "Split bust_left bone present in baked model");
                check(baked.getBone("bust_right").isPresent(), "Split bust_right bone present in baked model");
                check(state.hasGeckolibData(DataTickets.ANIMATABLE_MANAGER), "AnimatableManager extracted");
                check(state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES) != null, "Animation controllers extracted");
                check(model.getTextureResource(state).getPath().endsWith("ascaris.png"), "Default texture selected");
            });

            context.getInput().lookAt(0, 12);
            // 触发病娇凝视（无高光暗瞳 + yandere_stare）截屏
            server.runOnServer(game -> {
                var a = (AscarisEntity) game.overworld().getEntity(id);
                if (a != null) {
                    a.setObsession(70);
                    a.forceShowcasePose(AscarisPoseState.YANDERE_STARE, false);
                }
            });
            context.waitTicks(18);
            context.runOnClient(mc -> {
                var a = (AscarisEntity) mc.level.getEntity(id);
                var renderer = (AscarisRenderer) mc.getEntityRenderDispatcher().getRenderer(a);
                var state = (AscarisRenderer.State) mc.getEntityRenderDispatcher().extractEntity(a, 0.5f);
                check(renderer.getGeoModel().getTextureResource(state).getPath().endsWith("ascaris_yandere.png"),
                        "Yandere stare switches to ascaris_yandere.png");
            });
            context.takeScreenshot("ascaris-yandere-stare");

            // 触发害怕 >~< 蜷缩（curl_up）截屏
            server.runOnServer(game -> {
                var a = (AscarisEntity) game.overworld().getEntity(id);
                if (a != null) {
                    a.setObsession(10);
                    a.forceShowcasePose(AscarisPoseState.CURL_UP, false);
                }
            });
            context.waitTicks(30);
            context.takeScreenshot("ascaris-scared-curl");

            // 触发病娇缠绕（爱心暗瞳 + 深玫暗红 HUD + 漂浮低语）截屏
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                var a = (AscarisEntity) game.overworld().getEntity(id);
                if (a != null) {
                    a.setNoAi(false);
                    a.setObsession(90);
                    a.forceShowcasePose(AscarisPoseState.NORMAL, false);
                    a.startTailWrap(player, AscarisEntity.WRAP_YANDERE);
                }
            });
            context.waitTicks(55);
            context.runOnClient(mc -> {
                var a = (AscarisEntity) mc.level.getEntity(id);
                var renderer = (AscarisRenderer) mc.getEntityRenderDispatcher().getRenderer(a);
                var state = (AscarisRenderer.State) mc.getEntityRenderDispatcher().extractEntity(a, 0.5f);
                check(renderer.getGeoModel().getTextureResource(state).getPath().endsWith("ascaris_yandere_heart.png"),
                        "Yandere wrap switches to ascaris_yandere_heart.png");
            });
            context.takeScreenshot("ascaris-yandere-wrap");

            // 打开优化后的动态腹部 B 超截屏（确认上下读数条在视窗内 100% 可见）
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                var a = (AscarisEntity) game.overworld().getEntity(id);
                if (a != null) a.releaseTailWrap(false);
                ServerPlayNetworking.send(player, new XRayScanScreenPayload(ScanType.ABDOMEN, player.getId()));
            });
            context.waitFor(mc -> mc.screen instanceof AbdomenScanScreen);
            context.waitTicks(15);
            context.takeScreenshot("ascaris-abdomen-ultrasound-v2");
            context.runOnClient(mc -> mc.setScreen(null));

            // 缩小玩家至 0.25x 并坐到粉白蛔虫娘胸口上（前 3/4 平视第三人称截屏，验证胸口落点）
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                var a = (AscarisEntity) game.overworld().getEntity(id);
                if (a != null) {
                    a.setNoAi(true);
                    a.forceShowcasePose(AscarisPoseState.NORMAL, false);
                    a.setYRot(180f);
                    a.setYBodyRot(180f);
                    a.setYHeadRot(180f);
                    org.cneko.justarod.scale.CuteScaleController.setCuteScaleImmediate(player, 0.25);
                    a.mountShrunkPlayerOnChest(player);
                }
            });
            context.runOnClient(mc -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                if (mc.player != null) {
                    mc.player.setYRot(152f);
                    mc.player.setXRot(4f);
                }
            });
            context.waitTicks(24);
            context.takeScreenshot("ascaris-chest-ride");
            context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        } finally {
            context.runOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().set("fun.entity_squash.enabled", oldSquash));
        }
    }
}
