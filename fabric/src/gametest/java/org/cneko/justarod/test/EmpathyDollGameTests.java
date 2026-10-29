package org.cneko.justarod.test;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.empathy.EmpathyDollEvents;
import org.cneko.justarod.empathy.EmpathyReaction;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.EmpathyDollEntity;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.item.EmpathyDollItem;
import org.cneko.justarod.item.JRItems;

import java.util.UUID;

public class EmpathyDollGameTests {
    private record ConnectedPlayer(ServerPlayer player, io.netty.channel.embedded.EmbeddedChannel channel) { }

    private ConnectedPlayer connectedPlayer(GameTestHelper h, double z) {
        var level = h.getLevel();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "EmpathyTest"), false);
        var p = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        var pos = h.absoluteVec(new Vec3(2.5, 12, z));
        p.teleportTo(pos.x, pos.y, pos.z);
        p.setNoGravity(true);
        p.setGameMode(GameType.SURVIVAL);
        return new ConnectedPlayer(p, channel);
    }

    private ServerPlayer player(GameTestHelper h, double z) { return connectedPlayer(h, z).player(); }

    private EmpathyDollEntity doll(GameTestHelper h, ServerPlayer operator, ServerPlayer target) {
        var d = new EmpathyDollEntity(JREntities.EMPATHY_DOLL, h.getLevel());
        d.setPos(operator.position().add(0, 0, 1.5));
        d.setNoGravity(true);
        var stack = new ItemStack(JRItems.Companion.getEMPATHY_DOLL());
        EmpathyDollItem.bind(stack, target);
        d.setDollStack(stack);
        h.getLevel().addFreshEntity(d);
        return d;
    }

    @GameTest
    public void bindPlaceSaveAndRecoverSkinProfile(GameTestHelper h) {
        var p = player(h, 2);
        var item = JRItems.Companion.getEMPATHY_DOLL();
        var stack = new ItemStack(item);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.setShiftKeyDown(true);
        item.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(p.getUUID().equals(EmpathyDollItem.boundPlayer(stack)), "Sneak-use links the holder");
        item.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(EmpathyDollItem.boundPlayer(stack) == null, "Sneak-use unlinks a bound doll");
        UseEntityCallback.EVENT.invoker().interact(p, h.getLevel(), InteractionHand.MAIN_HAND, p, null);
        h.assertTrue(p.getUUID().equals(EmpathyDollItem.boundPlayer(stack)), "Entity interaction binds the target player");
        p.setShiftKeyDown(false);
        BlockPos floor = h.absolutePos(new BlockPos(2, 11, 3));
        h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        var hit = new BlockHitResult(new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5), Direction.UP, floor, false);
        h.assertTrue(item.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit)) == InteractionResult.SUCCESS, "The doll can be placed on a block");
        h.assertTrue(stack.isEmpty(), "Survival placement consumes exactly the carried doll");
        var placed = h.getLevel().getEntitiesOfClass(EmpathyDollEntity.class, p.getBoundingBox().inflate(4)).getFirst();
        var save = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        placed.saveWithoutId(save);
        var restored = new EmpathyDollEntity(JREntities.EMPATHY_DOLL, h.getLevel());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), save.buildResult()));
        h.assertTrue(p.getUUID().equals(EmpathyDollItem.boundPlayer(restored.getDollStack())), "Binding survives entity save/load");
        h.assertTrue(restored.getDollStack().get(DataComponents.PROFILE).partialProfile().equals(p.getGameProfile()), "The complete skin profile is preserved");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setShiftKeyDown(true);
        placed.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(placed.isRemoved(), "Sneak-use removes the placed doll");
        long recovered = p.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(item)
                && p.getUUID().equals(EmpathyDollItem.boundPlayer(s))).count();
        h.assertTrue(recovered == 1, "Pickup returns exactly one doll with its original binding");
        h.succeed();
    }

    @GameTest(maxTicks = 220)
    public void allSixReactionsReachRemotePlayerWithSmallCactusDamage(GameTestHelper h) {
        var operator = player(h, 2);
        var target = player(h, 42);
        var d = doll(h, operator, target);
        float health = target.getHealth();
        double scale = target.getAttributeValue(Attributes.SCALE);
        ItemStack[] tools = {ItemStack.EMPTY, new ItemStack(Items.FEATHER), new ItemStack(Items.ICE),
                new ItemStack(Items.CACTUS), ItemStack.EMPTY, new ItemStack(Items.END_ROD)};
        for (var kind : EmpathyReaction.values()) {
            h.runAfterDelay(kind.ordinal() * 22 + 1, () -> {
                operator.setItemInHand(InteractionHand.MAIN_HAND, tools[kind.ordinal()]);
                target.setOnGround(true);
                if (kind == EmpathyReaction.SQUEEZE) d.skipAttackInteraction(operator);
                else d.interact(operator, InteractionHand.MAIN_HAND, Vec3.ZERO);
                h.assertTrue(EmpathyDollEvents.activeReaction(target) == kind, "Contact starts " + kind + " on the distant target");
                float expectedHealth = health - (kind.ordinal() >= EmpathyReaction.PRICK.ordinal() ? 0.5f : 0);
                h.assertTrue(Math.abs(target.getHealth() - expectedHealth) < 0.001 && target.getAttributeValue(Attributes.SCALE) == scale,
                        "Only cactus deals a small half-point of damage; scale is unchanged");
                if (kind == EmpathyReaction.TICKLE) h.assertTrue(target.getDeltaMovement().y >= 0.32, "Tickling produces a real small hop");
                if (kind == EmpathyReaction.END_ROD) h.assertTrue(target.hasEffect(JREffects.Companion.getORGASM_EFFECT())
                        && target.getEffect(JREffects.Companion.getORGASM_EFFECT()).getDuration() == kind.duration,
                        "End rod contact grants the actual orgasm effect for the reaction duration");
            });
        }
        h.runAfterDelay(195, () -> {
            h.assertTrue(EmpathyDollEvents.activeReaction(target) == null, "The final reaction expires automatically");
            h.assertTrue(d.squeezeHeight(0) == 1, "The doll regains its full height after squeezing");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 45)
    public void sharedCooldownCannotBeBypassedWithAnotherDoll(GameTestHelper h) {
        var operator = player(h, 2);
        var target = player(h, 42);
        var first = doll(h, operator, target);
        var second = doll(h, operator, target);
        h.assertTrue(EmpathyDollEvents.trigger(first, operator, EmpathyReaction.TICKLE), "Initial interaction succeeds");
        h.assertTrue(!EmpathyDollEvents.trigger(second, operator, EmpathyReaction.END_ROD), "A second doll shares the target cooldown");
        h.runAfterDelay(21, () -> {
            h.assertTrue(EmpathyDollEvents.trigger(second, operator, EmpathyReaction.SQUEEZE), "A later interaction replaces the reaction");
            h.assertTrue(EmpathyDollEvents.activeReaction(target) == EmpathyReaction.SQUEEZE, "Reactions replace rather than stack");
            h.assertTrue(!EmpathyDollEvents.trigger(first, operator, EmpathyReaction.COLD), "Replacement also starts cooldown");
            target.setHealth(0);
        });
        h.runAfterDelay(23, () -> {
            h.assertTrue(EmpathyDollEvents.activeReaction(target) == null, "Death clears temporary reactions");
            h.succeed();
        });
    }

    @GameTest
    public void unavailableTargetsAndRemoteOperatorsCannotTrigger(GameTestHelper h) {
        var operator = player(h, 2);
        var target = player(h, 42);
        var d = doll(h, operator, target);
        var offline = d.getDollStack();
        offline.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(UUID.randomUUID()));
        d.setDollStack(offline);
        h.assertTrue(!EmpathyDollEvents.trigger(d, operator, EmpathyReaction.COLD), "Offline targets are rejected");
        var stack = d.getDollStack();
        EmpathyDollItem.bind(stack, target);
        d.setDollStack(stack);
        target.setGameMode(GameType.SPECTATOR);
        h.assertTrue(!EmpathyDollEvents.trigger(d, operator, EmpathyReaction.COLD), "Spectator targets cannot respond");
        target.setGameMode(GameType.SURVIVAL);
        operator.teleportTo(target.getX(), target.getY(), target.getZ());
        h.assertTrue(!EmpathyDollEvents.trigger(d, operator, EmpathyReaction.COLD), "The operator must actually reach the doll");
        h.succeed();
    }

    @GameTest(maxTicks = 35)
    public void consecutiveSqueezesInterruptAndRestartBeforeCooldown(GameTestHelper h) {
        var operator = player(h, 2);
        var connection = connectedPlayer(h, 42);
        var target = connection.player();
        var d = doll(h, operator, target);
        while (connection.channel().readOutbound() != null) { }
        h.assertTrue(EmpathyDollEvents.trigger(d, operator, EmpathyReaction.SQUEEZE), "First squeeze starts");
        for (int tick : new int[]{3, 6, 9}) {
            h.runAfterDelay(tick, () -> {
                h.assertTrue(d.squeezeHeight(0) < 0.5f, "The preceding squeeze is still compressed");
                d.skipAttackInteraction(operator);
                h.assertTrue(d.squeezeHeight(0) == 1 && EmpathyDollEvents.activeReaction(target) == EmpathyReaction.SQUEEZE,
                        "Each new click restarts the doll and player together without waiting for cooldown");
                h.assertTrue(!EmpathyDollEvents.trigger(d, operator, EmpathyReaction.SQUEEZE),
                        "Duplicate callbacks in the same tick do not replay again");
                h.assertTrue(!EmpathyDollEvents.trigger(d, operator, EmpathyReaction.PRICK),
                        "The squeeze exception does not bypass cooldown for other effects");
            });
        }
        h.runAfterDelay(26, () -> {
            h.assertTrue(d.squeezeHeight(0) == 1 && EmpathyDollEvents.activeReaction(target) == null,
                    "Both animations finish relative to the last click");
            int sounds = 0;
            Object packet;
            while ((packet = connection.channel().readOutbound()) != null) {
                if (packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound
                        && sound.getSound().value().location().equals(org.cneko.justarod.sound.JRSounds.DOLL_SQUEEZE.location())) sounds++;
            }
            h.assertTrue(sounds == 4, "Each accepted click plays one sound, without duplicate same-tick sounds; count=" + sounds);
            h.succeed();
        });
    }

    @GameTest(maxTicks = 25)
    public void orgasmEffectItselfSendsHeartPacketsWithoutADoll(GameTestHelper h) {
        var observer = connectedPlayer(h, 2);
        var cow = h.spawnWithNoFreeWill(EntityType.COW, 3, 12, 3);
        cow.setNoGravity(true);
        while (observer.channel().readOutbound() != null) { }
        cow.addEffect(new MobEffectInstance(JREffects.Companion.getORGASM_EFFECT(), 40, 0));
        int initialHearts = 0;
        Object initialPacket;
        while ((initialPacket = observer.channel().readOutbound()) != null) {
            if (initialPacket instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket particles
                    && particles.getParticle().getType() == net.minecraft.core.particles.ParticleTypes.HEART) {
                h.assertTrue(particles.isOverrideLimiter() && particles.alwaysShow(), "The initial burst is not hidden by particle settings");
                initialHearts += particles.getCount();
            }
        }
        h.assertTrue(initialHearts == 40, "Applying orgasm immediately broadcasts a visible burst, even before the first effect tick");
        cow.addEffect(new MobEffectInstance(JREffects.Companion.getORGASM_EFFECT(), 80, 0));
        h.assertTrue(observer.channel().readOutbound() == null, "Refreshing an existing effect does not repeat the initial burst");
        h.runAfterDelay(12, () -> {
            int hearts = 0;
            Object packet;
            while ((packet = observer.channel().readOutbound()) != null) {
                if (packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket particles
                        && particles.getParticle().getType() == net.minecraft.core.particles.ParticleTypes.HEART) {
                    h.assertTrue(particles.isOverrideLimiter() && particles.alwaysShow(), "Sustained hearts also bypass reduced particle settings");
                    hearts += particles.getCount();
                }
            }
            h.assertTrue(hearts >= 100, "Orgasm broadcasts a sustained dense stream, independent of any doll; count=" + hearts);
            h.assertTrue(cow.hasEffect(JREffects.Companion.getORGASM_EFFECT()), "The effect continues ticking after emitting hearts");
            h.succeed();
        });
    }
}
