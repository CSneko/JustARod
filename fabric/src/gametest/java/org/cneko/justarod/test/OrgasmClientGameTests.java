package org.cneko.justarod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.HeartParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import org.cneko.justarod.effect.JREffects;

import java.util.Map;
import java.util.function.Predicate;

/** Count actual HEART instances and check the camera frustum, including the minimal particle setting. */
public class OrgasmClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static long hearts(Minecraft mc, Predicate<Particle> filter) {
        try {
            var field = ParticleEngine.class.getDeclaredField("particles");
            field.setAccessible(true);
            var groups = (Map<?, ?>) field.get(mc.particleEngine);
            return groups.values().stream().map(value -> (ParticleGroup<?>) value)
                    .flatMap(group -> group.getAll().stream())
                    .filter(particle -> particle instanceof HeartParticle && particle.isAlive() && filter.test(particle)).count();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot inspect live client heart particles", e);
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runOnServer(game -> {
                var level = game.overworld();
                for (int x = -8; x <= 45; x++) for (int z = -8; z <= 8; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                game.getPlayerList().getPlayers().getFirst().teleportTo(0.5, 80, 0.5);
            });
            context.waitFor(mc -> mc.player.getY() > 79);
            ParticleStatus previous = context.computeOnClient(mc -> mc.options.particles().get());
            try {
                context.getInput().lookAt(0, 0);
                context.runOnClient(mc -> {
                    mc.options.particles().set(ParticleStatus.MINIMAL);
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    mc.particleEngine.clearParticles();
                });
                server.runOnServer(game -> {
                    var player = game.getPlayerList().getPlayers().getFirst();
                    // Like /effect ... true: hide the normal potion bubbles, keep orgasm's own visual feedback.
                    player.addEffect(new MobEffectInstance(JREffects.Companion.getORGASM_EFFECT(), 200, 0, false, false));
                });
                context.waitTicks(12);
                context.runOnClient(mc -> {
                    long count = hearts(mc, particle -> true);
                    check(count >= 100, "Orgasm produces a dense cloud even on MINIMAL; actual hearts=" + count);
                    long visible = hearts(mc, p -> mc.gameRenderer.getMainCamera().getCullFrustum().isVisible(p.getBoundingBox()));
                    check(visible >= 20, "Many hearts are in the third-person camera frustum; visible=" + visible);
                });
                context.takeScreenshot("orgasm-many-hearts-minimal-third-person");

                context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
                context.waitTicks(8);
                context.runOnClient(mc -> {
                    long visible = hearts(mc, p -> mc.gameRenderer.getMainCamera().getCullFrustum().isVisible(p.getBoundingBox()));
                    check(visible >= 8, "The player's own hearts also enter the first-person view; visible=" + visible);
                });
                context.takeScreenshot("orgasm-many-hearts-minimal-first-person");

                server.runOnServer(game -> {
                    var cow = EntityType.COW.create(game.overworld(), EntitySpawnReason.COMMAND);
                    cow.setNoAi(true);
                    cow.setPos(40.5, 80, 0.5);
                    game.overworld().addFreshEntity(cow);
                    cow.addEffect(new MobEffectInstance(JREffects.Companion.getORGASM_EFFECT(), 80, 0));
                });
                context.waitTicks(12);
                context.runOnClient(mc -> check(hearts(mc, p -> p.getBoundingBox().getCenter().x > 30) >= 100,
                        "The observer receives the same dense hearts beyond the normal 32-block particle limit"));
            } finally {
                context.runOnClient(mc -> {
                    mc.options.particles().set(previous);
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                });
            }
        }
    }
}
