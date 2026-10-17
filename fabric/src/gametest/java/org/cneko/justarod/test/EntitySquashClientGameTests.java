package org.cneko.justarod.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cneko.justarod.client.renderer.EntitySquashAnimation;
import org.cneko.justarod.config.JRConfig;
import org.joml.Matrix4f;

import java.lang.reflect.Proxy;

/** Verify the actual dispatcher transforms across living, player and item renderers. */
public class EntitySquashClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        check(EntitySquashAnimation.heightScale(0, 120, 0.5f) == 0.5f, "Beat 1 squashes");
        check(EntitySquashAnimation.heightScale(20, 120, 0.5f) == 1f, "Beat 2 restores after one second");
        check(EntitySquashAnimation.heightScale(40, 120, 0.5f) == 0.5f, "Beat 3 squashes");
        check(EntitySquashAnimation.heightScale(60, 120, 0.5f) == 1f, "Beat 4 restores");
        check(EntitySquashAnimation.heightScale(80, 120, 0.5f) == 0.5f, "Next bar repeats");
        check(EntitySquashAnimation.heightScale(10, 240, 0.5f) == 1f, "Doubling BPM halves the interval");
        check(EntitySquashAnimation.heightScale(10, 120, 0.5f) == 0.75f, "Transition is smooth");
        check(EntitySquashAnimation.rotationDegrees(0, 120) == 0f, "Rotation starts at zero");
        check(EntitySquashAnimation.rotationDegrees(20, 120) == 90f, "Restore beat still rotates");
        check(EntitySquashAnimation.rotationDegrees(40, 120) == 180f, "Half a bar turns halfway");
        check(EntitySquashAnimation.rotationDegrees(80, 120) == 0f, "One bar completes a full turn");
        check(EntitySquashAnimation.rotationDegrees(10, 240) == 90f, "Rotation follows BPM");
        verifyFrameClock();

        try (var world = context.worldBuilder().create()) {
            context.runOnClient(EntitySquashClientGameTests::verifyRenderers);
        }
    }

    private static void verifyFrameClock() {
        long oldWorld = (1L << 25) + 10;
        var clock = new EntitySquashAnimation.Clock();
        double first = clock.sample(oldWorld, 0.25f, 0L, 120f, false);
        double expected = (((double) oldWorld + 0.25) * 120.0 / 2400.0) % 4.0;
        check(Math.abs(first - expected) < 1e-9, "Old worlds retain fractional-frame precision");
        float lastHeight = EntitySquashAnimation.heightScaleForPhase(first, 0.5f);
        double previous = first;
        for (int frame = 1; frame <= 12; frame++) {
            // World ticks and partial ticks stay fixed; 120 FPS must still yield 120 animation updates.
            double phase = clock.sample(oldWorld, 0.25f, frame * 1_000_000_000L / 120, 120f, false);
            check(Math.abs(phase - previous - 1.0 / 120.0) < 1e-8,
                    "Animation advances evenly on every render frame between game ticks");
            float height = EntitySquashAnimation.heightScaleForPhase(phase, 0.5f);
            check(height != lastHeight && Math.abs(height - lastHeight) < 0.01f,
                    "Compression changes smoothly between successive high-FPS frames");
            previous = phase;
            lastHeight = height;
        }
        double corrected = clock.sample(0, 0f, 200_000_000L, 120f, false);
        check(Math.abs(corrected - first - 0.2) < 1e-9, "Server time corrections do not jump the animation");
        double faster = clock.sample(oldWorld, 0f, 300_000_000L, 240f, false);
        check(Math.abs(faster - corrected - 0.2) < 1e-9, "Changing BPM preserves phase and changes speed");
        check(clock.sample(oldWorld, 0f, 400_000_000L, 240f, true) == faster, "Pausing freezes the frame clock");
        check(clock.sample(oldWorld, 0f, 10_000_000_000L, 240f, true) == faster, "Paused time is never accumulated");
        check(clock.sample(oldWorld, 0f, 10_100_000_000L, 240f, false) == faster, "Resuming has no catch-up jump");
        double resumed = clock.sample(oldWorld, 0f, 10_200_000_000L, 240f, false);
        check(Math.abs(resumed - faster - 0.2) < 1e-9, "Resumed animation continues at the configured tempo");
        clock.reset();
        check(clock.sample(0, 0f, 20_000_000_000L, 120f, false) == 0.0, "A new world resets the clock");

        clock.reset();
        clock.sample(0, 0f, 0L, 120f, false);
        check(clock.sample(0, 0f, 4_000_000_000L, 120f, false) == 0.0, "The frame clock keeps the four-second bar");
    }

    private static void verifyRenderers(Minecraft minecraft) {
        var config = JRConfig.CONFIG_BUILDER.getConfig();
        Object oldEnabled = config.get("fun.entity_squash.enabled");
        Object oldRatio = config.get("fun.entity_squash.compression_ratio");
        Object oldBpm = config.get("fun.entity_squash.bpm");
        try {
            check(!(Boolean) JRConfig.CONFIG_BUILDER.get("fun.entity_squash.enabled").value,
                    "Effect defaults to disabled");
            config.set("fun.entity_squash.compression_ratio", -5f);
            check(JRConfig.getEntitySquashCompressionRatio() == 0.1f, "Compression has a positive lower bound");
            config.set("fun.entity_squash.compression_ratio", Float.NaN);
            check(JRConfig.getEntitySquashCompressionRatio() == 0.5f, "Non-finite ratio falls back to default");
            config.set("fun.entity_squash.bpm", 5000f);
            check(JRConfig.getEntitySquashBpm() == 600f, "Tempo has an upper bound");
            config.set("fun.entity_squash.bpm", "invalid");
            check(JRConfig.getEntitySquashBpm() == 120f, "Invalid tempo falls back to default");
            config.set("fun.entity_squash.compression_ratio", 0.5f);
            config.set("fun.entity_squash.bpm", 120f);

            var cow = EntityType.COW.create(minecraft.level, EntitySpawnReason.COMMAND);
            check(cow != null, "Cow probe can be created");
            cow.tickCount = 137;
            var item = new ItemEntity(minecraft.level, 0, 80, 0, new ItemStack(Items.DIAMOND));
            item.tickCount = 5;
            var dispatcher = minecraft.getEntityRenderDispatcher();
            EntityRenderState[] states = {
                    dispatcher.extractEntity(minecraft.player, 0),
                    dispatcher.extractEntity(cow, 0),
                    dispatcher.extractEntity(item, 0)
            };
            String[] names = {"Player", "Cow", "Dropped item"};
            for (var state : states) {
                state.nameTag = null;
                state.scoreText = null;
            }

            config.set("fun.entity_squash.enabled", false);
            dispatcher.prepare(minecraft.gameRenderer.getMainCamera(), null);
            Matrix4f[] baseline = new Matrix4f[states.length];
            for (int i = 0; i < states.length; i++) baseline[i] = capture(minecraft, states[i]);

            config.set("fun.entity_squash.enabled", true);
            // Use an early beat for a visible non-identity transform.
            long oldTime = minecraft.level.getGameTime();
            minecraft.level.setTimeFromServer((1L << 25) + 10);
            dispatcher.prepare(minecraft.gameRenderer.getMainCamera(), null);
            double ticks = (double) minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float height = EntitySquashAnimation.heightScale(ticks, 120f, 0.5f);
            float rotation = EntitySquashAnimation.rotationDegrees(ticks, 120f);
            minecraft.level.setTimeFromServer(oldTime);
            for (int i = 0; i < states.length; i++) {
                Matrix4f expected = new Matrix4f().translation(7, 11, 13)
                        .rotate(Axis.YP.rotationDegrees(rotation))
                        .scale(1f / height, height, 1f / height).translate(-7, -11, -13).mul(baseline[i]);
                check(capture(minecraft, states[i]).equals(expected, 0.0001f),
                        names[i] + " must use the shared frame scale regardless of entity age");
            }

            config.set("fun.entity_squash.compression_ratio", 1f);
            config.set("fun.entity_squash.enabled", false);
            dispatcher.prepare(minecraft.gameRenderer.getMainCamera(), null);
            config.set("fun.entity_squash.enabled", true);
            minecraft.level.setTimeFromServer(20);
            dispatcher.prepare(minecraft.gameRenderer.getMainCamera(), null);
            rotation = EntitySquashAnimation.rotationDegrees(
                    (double) minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), 120f);
            minecraft.level.setTimeFromServer(oldTime);
            for (int i = 0; i < states.length; i++) {
                Matrix4f expected = new Matrix4f().translation(7, 11, 13)
                        .rotate(Axis.YP.rotationDegrees(rotation)).translate(-7, -11, -13).mul(baseline[i]);
                check(capture(minecraft, states[i]).equals(expected, 0.0001f),
                        "Ratio 1 keeps rotation without squashing " + names[i]);
            }
            config.set("fun.entity_squash.compression_ratio", 0.5f);
            config.set("fun.entity_squash.enabled", false);
            dispatcher.prepare(minecraft.gameRenderer.getMainCamera(), null);
            for (int i = 0; i < states.length; i++) {
                check(capture(minecraft, states[i]).equals(baseline[i], 0.0001f), "Disabling restores " + names[i]);
            }
        } finally {
            config.set("fun.entity_squash.enabled", oldEnabled);
            config.set("fun.entity_squash.compression_ratio", oldRatio);
            config.set("fun.entity_squash.bpm", oldBpm);
            minecraft.getEntityRenderDispatcher().prepare(minecraft.gameRenderer.getMainCamera(), null);
        }
    }

    private static Matrix4f capture(Minecraft minecraft, EntityRenderState state) {
        Matrix4f[] captured = {null};
        SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(), new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("order")) return proxy;
                    if (captured[0] == null && (method.getName().equals("submitModel") || method.getName().equals("submitItem"))) {
                        for (Object arg : args) {
                            if (arg instanceof PoseStack pose) captured[0] = new Matrix4f(pose.last().pose());
                        }
                    }
                    return null;
                });
        PoseStack pose = new PoseStack();
        minecraft.getEntityRenderDispatcher().submit(state, new CameraRenderState(), 7, 11, 13, pose, collector);
        check(captured[0] != null, "Renderer must submit geometry");
        check(pose.last().pose().equals(new Matrix4f(), 0.0001f), "Dispatcher must restore its caller's pose");
        return captured[0];
    }
}
