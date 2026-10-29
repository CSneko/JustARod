package org.cneko.justarod.test;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.cneko.justarod.client.renderer.empathy.EmpathyAnimation;
import org.cneko.justarod.client.renderer.empathy.FumoDollRenderer;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.empathy.EmpathyDollEvents;
import org.cneko.justarod.empathy.EmpathyReaction;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.EmpathyDollEntity;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.item.EmpathyDollItem;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.EmpathyReactionPayload;
import org.joml.Matrix4f;

import java.lang.reflect.Proxy;

public class EmpathyDollClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static EmpathyDollEntity doll(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player) {
        return level.getEntitiesOfClass(EmpathyDollEntity.class, player.getBoundingBox().inflate(8)).getFirst();
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runOnServer(game -> {
                var level = game.overworld();
                for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                var player = game.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(0.5, 80, -2.5);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FEATHER));
                var stack = new ItemStack(JRItems.Companion.getEMPATHY_DOLL());
                EmpathyDollItem.bind(stack, player);
                var doll = new EmpathyDollEntity(JREntities.EMPATHY_DOLL, level);
                doll.setDollStack(stack);
                doll.setPos(0.5, 80, 0.5);
                doll.setYRot(180);
                level.addFreshEntity(doll);
            });
            context.waitFor(mc -> !mc.level.getEntitiesOfClass(EmpathyDollEntity.class, mc.player.getBoundingBox().inflate(8)).isEmpty()
                    && mc.player.getMainHandItem().is(Items.FEATHER));
            context.getInput().lookAt(0, 22);
            context.waitTicks(3);
            context.runOnClient(mc -> {
                var doll = doll(mc.level, mc.player);
                check(mc.player.getUUID().equals(EmpathyDollItem.boundPlayer(doll.getDollStack())), "The bound skin profile is synchronized");
                check(mc.getEntityRenderDispatcher().getRenderer(doll) instanceof FumoDollRenderer, "The doll has its own Fumo renderer");
                var state = (FumoDollRenderer.State) mc.getEntityRenderDispatcher().extractEntity(doll, 0);
                check(state.skin != null && state.height == 1, "The doll resolves a skin and starts uncompressed");
                var stackState = new net.minecraft.client.renderer.item.ItemStackRenderState();
                mc.getItemModelResolver().updateForNonLiving(stackState, doll.getDollStack(), ItemDisplayContext.GUI, doll);
                check(!stackState.isEmpty(), "The doll item model resolves in the inventory");
                int[] parts = {0};
                var collector = collector((method, args) -> { if (method.equals("submitModelPart")) parts[0]++; });
                stackState.submit(new PoseStack(), collector, 0xF000F0, 0, 0);
                check(parts[0] > 0, "The carried item renders the Fumo geometry rather than a missing-model cube");
            });
            context.takeScreenshot("empathy-fumo-doll");
            context.getInput().holdMouse(1);
            context.waitFor(mc -> EmpathyAnimation.reaction(mc.player, 0) != null);
            context.getInput().releaseMouse(1);
            context.runOnClient(mc -> check(EmpathyAnimation.reaction(mc.player, 0).kind() == EmpathyReaction.TICKLE,
                    "Real feather input reaches the server and returns a reaction packet"));
            context.waitTicks(45);

            float initialHealth = context.computeOnClient(mc -> mc.player.getHealth());
            for (var kind : EmpathyReaction.values()) {
                if (kind == EmpathyReaction.END_ROD) context.runOnClient(mc -> mc.particleEngine.clearParticles());
                server.runOnServer(game -> {
                    var p = game.getPlayerList().getPlayers().getFirst();
                    EmpathyDollEvents.clear(p);
                    float health = p.getHealth();
                    p.invulnerableTime = 0;
                    check(EmpathyDollEvents.trigger(doll(p.level(), p), p, kind), "Server starts " + kind);
                    float damage = health - p.getHealth();
                    check(Math.abs(damage - (kind == EmpathyReaction.PRICK ? 0.5f : 0)) < 0.001f,
                            "Only cactus causes a half-point of real damage");
                });
                context.waitFor(mc -> EmpathyAnimation.reaction(mc.player, 0) != null
                        && EmpathyAnimation.reaction(mc.player, 0).kind() == kind);
                context.runOnClient(mc -> {
                    var renderer = (AvatarRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
                    var state = (AvatarRenderState) renderer.createRenderState(mc.player, 0);
                    var model = (net.minecraft.client.model.player.PlayerModel) renderer.getModel();
                    model.setupAnim(state);
                    if (kind == EmpathyReaction.COLD)
                        check(model.rightArm.xRot < -0.5f && model.leftArm.xRot < -0.5f, "Cold draws both arms inward");
                    if (kind == EmpathyReaction.TICKLE)
                        check(Math.abs(model.rightArm.zRot) + Math.abs(model.leftArm.zRot) > 0.5f, "Tickling animates both arms");
                    check(mc.player.getHealth() >= initialHealth - 0.5f, "Only the tiny cactus injury reduces health");
                });
            }
            context.waitFor(mc -> mc.player.hasEffect(JREffects.Companion.getORGASM_EFFECT()));
            context.getInput().lookAt(0, 0);
            context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(8);
            context.runOnClient(mc -> check(Integer.parseInt(mc.particleEngine.countParticles()) >= 6,
                    "Orgasm's broadcast particles are instantiated on the connected client"));
            context.takeScreenshot("empathy-end-rod-orgasm-hearts");
            context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
            context.waitTicks(65);
            context.runOnClient(mc -> {
                check(EmpathyAnimation.reaction(mc.player, 0) == null, "Reactions expire on the client");
                verifyCompression(mc);
            });
            server.runOnServer(game -> {
                var p = game.getPlayerList().getPlayers().getFirst();
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            });
            context.waitFor(mc -> mc.player.getMainHandItem().isEmpty());
            context.getInput().lookAt(0, 22);
            long previousSqueeze = -1;
            for (int click = 0; click < 3; click++) {
                final long previous = previousSqueeze;
                context.getInput().holdMouse(0);
                context.waitTicks(1);
                context.getInput().releaseMouse(0);
                context.waitFor(mc -> {
                    var reaction = EmpathyAnimation.reaction(mc.player, 0);
                    return reaction != null && reaction.kind() == EmpathyReaction.SQUEEZE && reaction.startedAt() > previous
                            && Math.abs(doll(mc.level, mc.player).squeezeHeight(0)
                            - EmpathyReaction.squeezeHeight(reaction.age(0))) < 0.0001f;
                });
                long startedAt = context.computeOnClient(mc -> EmpathyAnimation.reaction(mc.player, 0).startedAt());
                if (previous >= 0) check(startedAt - previous < EmpathyReaction.SQUEEZE.duration,
                        "Real rapid clicks restart both synchronized animations before the previous squeeze finishes");
                previousSqueeze = startedAt;
                context.waitTicks(3);
            }
            context.waitTicks(EmpathyReaction.SQUEEZE.duration + 2);
            context.runOnClient(mc -> check(EmpathyAnimation.reaction(mc.player, 0) == null
                    && doll(mc.level, mc.player).squeezeHeight(0) == 1, "Rapid clicking leaves no stuck compression"));
            server.runOnServer(game -> {
                var p = game.getPlayerList().getPlayers().getFirst();
                EmpathyDollEvents.clear(p);
                p.setGameMode(GameType.SPECTATOR);
                check(!EmpathyDollEvents.trigger(doll(p.level(), p), p, EmpathyReaction.COLD), "A real spectator cannot trigger reactions");
                p.setGameMode(GameType.SURVIVAL);
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                p.setShiftKeyDown(true);
                doll(p.level(), p).interact(p, InteractionHand.MAIN_HAND, net.minecraft.world.phys.Vec3.ZERO);
            });
            context.waitFor(mc -> mc.level.getEntitiesOfClass(EmpathyDollEntity.class, mc.player.getBoundingBox().inflate(8)).isEmpty());
        }
    }

    private static void verifyCompression(Minecraft mc) {
        var config = JRConfig.CONFIG_BUILDER.getConfig();
        Object old = config.get("fun.entity_squash.enabled");
        try {
            config.set("fun.entity_squash.enabled", false);
            var dispatcher = mc.getEntityRenderDispatcher();
            dispatcher.prepare(mc.gameRenderer.getMainCamera(), null);
            var state = dispatcher.extractEntity(mc.player, 0);
            Matrix4f baseline = capture(mc, state);
            long now = mc.level.getGameTime();
            EmpathyAnimation.accept(new EmpathyReactionPayload(mc.player.getId(), EmpathyReaction.SQUEEZE.ordinal(), now - 3));
            Matrix4f compressed = capture(mc, state);
            check(!baseline.equals(compressed, 0.01f), "The actual player renderer compresses the target");
            config.set("fun.entity_squash.enabled", true);
            dispatcher.prepare(mc.gameRenderer.getMainCamera(), null);
            capture(mc, state); // Also asserts push/pop balance with the existing global effect enabled.
            EmpathyAnimation.accept(new EmpathyReactionPayload(mc.player.getId(), -1, 0));
            config.set("fun.entity_squash.enabled", false);
            dispatcher.prepare(mc.gameRenderer.getMainCamera(), null);
            check(capture(mc, state).equals(baseline, 0.0001f), "Clearing restores the original player transform");
        } finally {
            config.set("fun.entity_squash.enabled", old);
            EmpathyAnimation.accept(new EmpathyReactionPayload(mc.player.getId(), -1, 0));
            mc.getEntityRenderDispatcher().prepare(mc.gameRenderer.getMainCamera(), null);
        }
    }

    private interface Capture { void accept(String method, Object[] args); }
    private static SubmitNodeCollector collector(Capture capture) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                    if (method.getName().equals("order")) return proxy;
                    capture.accept(method.getName(), args);
                    return null;
                });
    }

    private static Matrix4f capture(Minecraft mc, EntityRenderState state) {
        Matrix4f[] matrix = {null};
        var collector = collector((method, args) -> {
            if (matrix[0] == null && method.equals("submitModel"))
                for (Object arg : args) if (arg instanceof PoseStack pose) matrix[0] = new Matrix4f(pose.last().pose());
        });
        PoseStack poses = new PoseStack();
        mc.getEntityRenderDispatcher().submit(state, new CameraRenderState(), 0, 0, 0, poses, collector);
        check(poses.isEmpty(), "All render transformations balance their pose stack");
        check(matrix[0] != null, "The player submits its model");
        return matrix[0];
    }
}
