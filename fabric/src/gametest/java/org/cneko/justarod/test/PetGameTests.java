package org.cneko.justarod.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.impl.networking.ChannelInfoHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.PetOwnerNekoEntity;
import org.cneko.justarod.packet.*;
import org.cneko.justarod.pet.PetController;
import org.cneko.justarod.pet.PetState;
import org.cneko.justarod.pet.PetOwnerActivity;
import java.util.UUID;
import static org.cneko.justarod.packet.PetActionPayload.*;

public class PetGameTests {
    private static final String TAMING_ENABLED = "pet.taming_enabled";

    private record Actor(ServerPlayer p, EmbeddedChannel channel) {
        PetMenuPayload menu() {
            PetMenuPayload found = null;
            for (Object packet; (packet = channel.readOutbound()) != null;) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof PetMenuPayload menu)
                    found = menu;
            }
            if (found == null) throw new AssertionError("Server did not send a pet menu");
            return found;
        }
        void act(PetMenuPayload menu, int action) { PetController.handle(p, new PetActionPayload(action, menu.npcId(), menu.token())); }
        PetMenuPayload open(Mob npc) { PetState.of(p).nextMenu = 0; PetController.open(p, npc.getId(), false); return menu(); }
    }

    private Actor actor(GameTestHelper h, double x) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "Pet" + UUID.randomUUID().toString().substring(0, 8)), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channels = ((ChannelInfoHolder) connection).fabric_getPendingChannelsNames(ConnectionProtocol.PLAY);
        channels.add(PetMenuPayload.ID.id()); channels.add(PetStatePayload.ID.id());
        var channel = new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 pos = h.absoluteVec(new Vec3(x, 12, 3.5));
        p.teleportTo(pos.x, pos.y, pos.z); p.setNoGravity(true); p.setGameMode(GameType.SURVIVAL);
        h.onEachTick(p::doTick);
        for (int dx = 0; dx <= 10; dx++) for (int dz = 0; dz <= 9; dz++)
            h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(dx, 11, dz)), Blocks.STONE.defaultBlockState());
        return new Actor(p, channel);
    }
    private Villager villager(GameTestHelper h, double x) {
        var npc = new Villager(EntityType.VILLAGER, h.getLevel());
        npc.setPos(h.absoluteVec(new Vec3(x, 12, 5.5))); npc.setNoAi(true); npc.setNoGravity(true);
        h.getLevel().addFreshEntity(npc); return npc;
    }
    private PetOwnerNekoEntity neko(GameTestHelper h, double x) {
        var npc = new PetOwnerNekoEntity(JREntities.PET_OWNER_NEKO, h.getLevel());
        npc.setPos(h.absoluteVec(new Vec3(x, 12, 5.5))); npc.setNoAi(true); npc.setNoGravity(true);
        h.getLevel().addFreshEntity(npc); return npc;
    }

    private static void withTamingEnabled(Runnable action) {
        boolean old = JRConfig.isPlayerTamingEnabled();
        var config = JRConfig.CONFIG_BUILDER.getConfig();
        config.set(TAMING_ENABLED, true);
        try {
            action.run();
        } finally {
            config.set(TAMING_ENABLED, old);
        }
    }

    private static void bind(ServerPlayer player, Mob npc) {
        withTamingEnabled(() -> {
            if (!PetController.bind(player, npc)) throw new AssertionError("Could not bind the test player");
        });
    }

    @GameTest
    public void disabledTamingBlocksNewOffersAndOutstandingConsent(GameTestHelper h) {
        var a = actor(h, 3.5);
        var npc = villager(h, 3.5);
        var config = JRConfig.CONFIG_BUILDER.getConfig();
        boolean old = JRConfig.isPlayerTamingEnabled();
        try {
            config.set(TAMING_ENABLED, false);
            PetController.handle(a.p, new PetActionPayload(OPEN, npc.getId(), NONE));
            PetController.open(a.p, npc.getId(), true);
            h.assertTrue(PetState.of(a.p).nextMenu == 0, "Disabled taming declines both normal and guaranteed offers");
            h.assertTrue(!PetController.bind(a.p, npc), "Disabled taming blocks direct initial binding");

            config.set(TAMING_ENABLED, true);
            PetController.open(a.p, npc.getId(), true);
            var offer = a.menu();
            config.set(TAMING_ENABLED, false);
            a.p.getFoodData().setFoodLevel(10);
            a.act(offer, ACCEPT);
            h.assertTrue(PetState.of(a.p).owner == null && a.p.getFoodData().getFoodLevel() == 10,
                    "Consent cannot bind or feed the player after taming is switched off");
        } finally {
            config.set(TAMING_ENABLED, old);
        }
        h.succeed();
    }

    @GameTest
    public void tamingRequiresAcceptAndRejectsAnotherPlayersToken(GameTestHelper h) {
        withTamingEnabled(() -> checkRequiredConsent(h));
        h.succeed();
    }

    private void checkRequiredConsent(GameTestHelper h) {
        var a = actor(h, 2.5); var b = actor(h, 3.5); var npc = villager(h, 3.5);
        PetController.open(a.p, npc.getId(), true); var offer = a.menu();
        h.assertTrue(PetState.of(a.p).owner == null, "Fish offer does not silently bind the player");
        b.act(offer, ACCEPT);
        h.assertTrue(PetState.of(b.p).owner == null, "Other players cannot use the actor's token");
        a.act(offer, ACCEPT);
        h.assertTrue(npc.getUUID().equals(PetState.of(a.p).owner), "Accepting the guaranteed attempt establishes ownership");
        a.act(offer, ACCEPT);
        h.assertTrue(PetState.of(a.p).mode == PetState.Mode.FREE, "Replayed accept has no effect");
        PetController.open(b.p, npc.getId(), true); var declined = b.menu(); b.act(declined, DECLINE);
        b.act(declined, ACCEPT);
        h.assertTrue(PetState.of(b.p).owner == null, "Declining invalidates the offered token");
    }

    @GameTest
    public void normalTransferIsConfirmedAndRevalidated(GameTestHelper h) {
        var a = actor(h, 3.5); var first = villager(h, 2.5); var next = villager(h, 5.5);
        bind(a.p, first);
        h.assertTrue(PetController.requestTransfer(a.p, next), "Nearby owner can propose transfer without absurdity");
        var transfer = a.menu();
        h.assertTrue(PetState.of(a.p).owner.equals(first.getUUID()), "Request leaves the original relationship intact");
        a.act(transfer, DECLINE); a.act(transfer, CONFIRM);
        h.assertTrue(PetState.of(a.p).owner.equals(first.getUUID()), "Decline and replay do not change ownership");
        PetController.requestTransfer(a.p, next); transfer = a.menu();
        next.setPos(next.position().add(20, 0, 0)); a.act(transfer, CONFIRM);
        h.assertTrue(PetState.of(a.p).owner.equals(first.getUUID()), "Candidate distance is checked again on confirmation");
        next.setPos(h.absoluteVec(new Vec3(5.5, 12, 5.5)));
        PetController.requestTransfer(a.p, next); transfer = a.menu(); a.act(transfer, CONFIRM);
        h.assertTrue(PetState.of(a.p).owner.equals(next.getUUID()), "Confirmed transfer atomically changes owner");
        h.succeed();
    }

    @GameTest
    public void absurdTradeUsesCatgirlAndCannotBypassConfigOrCreateEmeralds(GameTestHelper h) {
        boolean old = JRConfig.isAbsurdityEnabled();
        try {
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", true);
            var a = actor(h, 3.5); var first = villager(h, 2.5); var next = neko(h, 5.5);
            first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            bind(a.p, first); PetController.requestTransfer(a.p, next);
            var transfer = a.menu();
            h.assertTrue(transfer.absurd(), "Villager to catgirl has the three-emerald theatrical interface");
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", false); a.act(transfer, CONFIRM);
            h.assertTrue(PetState.of(a.p).owner.equals(first.getUUID()), "Disabling config invalidates an already-open absurd trade");
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", true);
            PetController.requestTransfer(a.p, next); transfer = a.menu(); a.act(transfer, CONFIRM);
            h.assertTrue(PetState.of(a.p).owner.equals(next.getUUID()), "Catgirl becomes the new owner after consent");
            h.assertTrue(first.getMainHandItem().is(Items.STICK) && next.getMainHandItem().isEmpty(),
                    "Theatrical equipment never mutates NPC inventories");
            h.assertTrue(a.p.getInventory().getNonEquipmentItems().stream().noneMatch(s -> s.is(Items.EMERALD)),
                    "The theatrical trade creates no player emeralds");
        } finally { JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", old); }
        h.succeed();
    }

    @GameTest
    public void relationshipSavesButOrdersDoNotAndRespawnCopiesOwner(GameTestHelper h) {
        var a = actor(h, 3.5); var npc = villager(h, 3.5);
        bind(a.p, npc); PetState.of(a.p).ownerActionIntervalTicks = 250;
        PetController.order(a.p, PetState.Mode.SIT);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        a.p.saveWithoutId(output);
        var cookie = CommonListenerCookie.createInitial(a.p.getGameProfile(), false);
        var restored = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(PetState.of(restored).owner.equals(npc.getUUID()), "Player save/load preserves the NPC relationship");
        h.assertTrue(PetState.of(restored).mode == PetState.Mode.FREE, "Reloading never restores an active movement order");
        h.assertTrue(PetState.of(restored).ownerActionIntervalTicks == PetOwnerActivity.DEFAULT_ACTION_INTERVAL_TICKS,
                "A new login returns to the default filming pace rather than persisting a temporary setting");
        var respawned = h.getLevel().getServer().getPlayerList().respawn(a.p, false, Entity.RemovalReason.KILLED);
        h.assertTrue(PetState.of(respawned).owner.equals(npc.getUUID()) && PetState.of(respawned).mode == PetState.Mode.FREE,
                "Respawn copies ownership and clears the seated state");
        h.assertTrue(PetState.of(respawned).ownerActionIntervalTicks == 250, "Respawning preserves the selected pace within the session");
        h.succeed();
    }

    @GameTest(maxTicks = 35)
    public void damageAndUnloadedOwnerClearOrdersWithoutDeletingRelationship(GameTestHelper h) {
        var a = actor(h, 3.5); var npc = villager(h, 3.5);
        bind(a.p, npc); PetController.order(a.p, PetState.Mode.SIT);
        a.p.hurtServer(h.getLevel(), a.p.damageSources().cactus(), 1);
        h.runAfterDelay(2, () -> {
            h.assertTrue(PetState.of(a.p).mode == PetState.Mode.FREE, "Damage cancels sitting");
            a.p.hurtTime = 0; PetController.order(a.p, PetState.Mode.FOLLOW);
            npc.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        });
        h.runAfterDelay(5, () -> {
            h.assertTrue(PetState.of(a.p).mode == PetState.Mode.FREE, "Unloading the owner suspends following");
            h.assertTrue(npc.getUUID().equals(PetState.of(a.p).owner), "Chunk unloading does not erase saved ownership");
            PetController.release(a.p);
            h.assertTrue(PetState.of(a.p).owner == null, "The player can release an unavailable owner");
            h.succeed();
        });
    }

    @GameTest
    public void safeRecallRejectsAirWallsWaterAndDamage(GameTestHelper h) {
        var a = actor(h, 3.5); var npc = villager(h, 4.5); bind(a.p, npc);
        Vec3 clear = h.absoluteVec(new Vec3(7.5, 12, 7.5));
        h.assertTrue(PetController.safeDestination(a.p, clear), "Solid clear ground is safe");
        var floor = BlockPos.containing(clear).below();
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.AIR, Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.CACTUS}) {
            h.getLevel().setBlockAndUpdate(floor, block.defaultBlockState());
            h.assertTrue(!PetController.safeDestination(a.p, clear), "Reject unsafe ground: " + block);
        }
        h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(floor.above(), Blocks.WATER.defaultBlockState());
        h.assertTrue(!PetController.safeDestination(a.p, clear), "Reject water");
        h.getLevel().setBlockAndUpdate(floor.above(), Blocks.STONE.defaultBlockState());
        h.assertTrue(!PetController.safeDestination(a.p, clear), "Reject solid walls");
        h.getLevel().setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
        a.p.teleportTo(a.p.getX() + 35, a.p.getY(), a.p.getZ());
        h.assertTrue(PetController.recall(a.p, true) && a.p.distanceToSqr(npc) < 36, "Recall returns to a safe nearby position");
        PetController.cancel(a.p, true);
        h.assertTrue(!PetController.recall(a.p, false), "Cancel also disables automatic recall");
        h.succeed();
    }

    @GameTest
    public void staleTokensAndInteractionCooldownCannotBeReplayed(GameTestHelper h) {
        var a = actor(h, 3.5); var npc = villager(h, 3.5); bind(a.p, npc);
        h.assertTrue(PetController.stroke(a.p, false), "First head pat works");
        h.assertTrue(!PetController.stroke(a.p, false), "Head pats have a real shared cooldown");
        a.p.getFoodData().setFoodLevel(5);
        PetState.of(a.p).nextFeed = 0;
        var menu = a.open(npc); a.act(menu, FEED);
        h.assertTrue(a.p.getFoodData().getFoodLevel() == 9, "Owner fish has an actual food benefit");
        menu = a.open(npc); a.act(menu, FEED);
        h.assertTrue(a.p.getFoodData().getFoodLevel() == 9, "Reopening the menu cannot bypass feeding cooldown");
        menu = a.open(npc); a.p.tickCount += 601; a.act(menu, SIT);
        h.assertTrue(PetState.of(a.p).mode == PetState.Mode.FREE, "Expired menu cannot apply an order");
        PetController.order(a.p, PetState.Mode.SIT);
        a.p.level().getServer().getCommands().performPrefixedCommand(a.p.createCommandSourceStack(), "jarpet cancel");
        h.assertTrue(PetState.of(a.p).mode == PetState.Mode.FREE && PetState.of(a.p).pausedUntil > a.p.tickCount,
                "Ordinary players can cancel actions without operator permissions");
        h.succeed();
    }

    @GameTest
    public void directorIsConfigAndPermissionGated(GameTestHelper h) {
        // The filming adapter can be deleted without changing the permanent gameplay suite.
        if (h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("jar_director") == null) {
            h.succeed(); return;
        }
        boolean old = JRConfig.isAbsurdityEnabled();
        try {
            var a = actor(h, 3.5); var npc = villager(h, 3.5); bind(a.p, npc);
            String command = "jar_director order " + a.p.getScoreboardName() + " sit";
            var server = h.getLevel().getServer();
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", false);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            h.assertTrue(PetState.of(a.p).mode == PetState.Mode.FREE, "Disabled config blocks the director even for server console");
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", true);
            server.getCommands().performPrefixedCommand(a.p.createCommandSourceStack(), command);
            h.assertTrue(PetState.of(a.p).mode == PetState.Mode.FREE, "Ordinary players cannot issue director commands");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            h.assertTrue(PetState.of(a.p).mode == PetState.Mode.SIT, "Enabled director invokes real gameplay for operators");
        } finally { JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", old); }
        h.succeed();
    }

    @GameTest
    public void absurdActorsRespectConsentReleaseAndOriginalAiFlags(GameTestHelper h) {
        boolean old = JRConfig.isAbsurdityEnabled();
        try {
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", true);
            var a = actor(h, 3.5); var npc = villager(h, 3.5); var candidate = neko(h, 5.5);
            npc.setNoAi(false); npc.setOnGround(true); candidate.setOnGround(true);
            h.assertTrue(!PetOwnerActivity.controlAi(npc), "Unrelated villagers keep their normal AI");
            withTamingEnabled(() -> {
                PetController.open(a.p, npc.getId(), true); var offer = a.menu();
                h.assertTrue(PetOwnerActivity.controlAi(npc) && PetState.of(a.p).owner == null,
                        "The feeding actor stays in place while waiting for consent, without taming the player");
                a.act(offer, DECLINE);
            });
            h.assertTrue(!PetOwnerActivity.controlAi(npc), "Declining releases an unbound actor immediately");
            bind(a.p, npc);
            PetController.requestTransfer(a.p, candidate); var transfer = a.menu();
            h.assertTrue(PetOwnerActivity.controlAi(candidate), "The proposed adopter waits for the handover shot");
            a.act(transfer, DECLINE);
            h.assertTrue(!PetOwnerActivity.controlAi(candidate) && PetOwnerActivity.controlAi(npc),
                    "Declining releases the proposed adopter but retains the current owner's stable position");
            PetController.order(a.p, PetState.Mode.SIT);
            h.assertTrue(PetState.of(a.p).nextOwnerAction >= a.p.tickCount + 120,
                    "A manual shot gets six seconds without automatic interruption");
            PetController.cancel(a.p, true);
            h.assertTrue(PetOwnerActivity.controlAi(npc) && PetState.of(a.p).mode == PetState.Mode.FREE,
                    "The actor stays while the player's escape key still cancels their action");
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
            npc.saveWithoutId(output);
            h.assertTrue(!npc.isNoAi() && !output.buildResult().getBooleanOr("NoAI", false),
                    "Filming never writes a disabled AI flag into the NPC save");
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", false);
            h.assertTrue(!PetOwnerActivity.controlAi(npc), "Disabling absurdity releases the AI immediately");
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", true);
            PetController.release(a.p);
            h.assertTrue(!PetOwnerActivity.controlAi(npc), "Releasing ownership immediately releases the actor");
        } finally { JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", old); }
        h.succeed();
    }

    @GameTest(maxTicks = 460)
    public void activeOwnersStayFaceAndPerformWholeCycleWithRealAi(GameTestHelper h) {
        boolean old = JRConfig.isAbsurdityEnabled();
        var rules = h.getLevel().getGameRules();
        var spawnMobs = net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS;
        boolean oldSpawning = rules.get(spawnMobs);
        // The isolated toNeko fixture does not initialize names for naturally spawned NPCs.
        rules.set(spawnMobs, false, h.getLevel().getServer());
        JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", true);
        var a = actor(h, 3.5); var npc = villager(h, 3.5);
        var b = actor(h, 7.5); var cat = neko(h, 7.5);
        npc.setNoAi(false); npc.setNoGravity(false);
        cat.setNoAi(false); cat.setNoGravity(false);
        bind(a.p, npc); bind(b.p, cat);
        PetOwnerActivity.setAutomatic(a.p, true, 3); PetOwnerActivity.setAutomatic(b.p, true, 3);
        a.p.getFoodData().setFoodLevel(5);
        Vec3 anchor = npc.position(), catAnchor = cat.position();
        int[] reactions = new int[4]; // fish, pat, sitting, following
        boolean[] stable = {true}, paused = {true}, facing = {false};
        String[] movementFailure = {""};
        h.onEachTick(() -> {
            // Try both navigation and the villager's Brain while the normal AI is enabled.
            npc.getNavigation().moveTo(anchor.x + 4, anchor.y, anchor.z + 2, 1);
            npc.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                    new net.minecraft.world.entity.ai.memory.WalkTarget(anchor.add(4, 0, 2), 1, 0));
            cat.getNavigation().moveTo(catAnchor.x + 2, catAnchor.y, catAnchor.z + 2, 1);
            if (JRConfig.isAbsurdityEnabled() && (npc.position().distanceToSqr(anchor) >= 0.25
                    || cat.position().distanceToSqr(catAnchor) >= 0.25)) {
                if (stable[0]) movementFailure[0] = " villager=" + npc.position().subtract(anchor)
                        + " cat=" + cat.position().subtract(catAnchor) + " target=" + cat.getTarget()
                        + " grounded=" + npc.onGround() + "/" + cat.onGround()
                        + " hurt=" + npc.hurtTime + "/" + cat.hurtTime;
                stable[0] = false;
            }
            for (Object packet; (packet = a.channel.readOutbound()) != null;) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof PetStatePayload state) {
                    if (state.reaction() == 1) reactions[0]++;
                    if (state.reaction() == 2) reactions[1]++;
                    if (state.mode() == PetState.Mode.SIT.ordinal()) reactions[2]++;
                    if (state.mode() == PetState.Mode.FOLLOW.ordinal()) reactions[3]++;
                }
            }
        });
        int[] countsWhenPaused = new int[2];
        h.runAfterDelay(350, () -> {
            facing[0] = npc.getLookAngle().dot(a.p.getEyePosition().subtract(npc.getEyePosition()).normalize()) > 0.9
                    && cat.getLookAngle().dot(b.p.getEyePosition().subtract(cat.getEyePosition()).normalize()) > 0.9;
            PetOwnerActivity.setAutomatic(a.p, false); PetOwnerActivity.setAutomatic(b.p, false);
            countsWhenPaused[0] = reactions[0]; countsWhenPaused[1] = reactions[1];
        });
        h.runAfterDelay(395, () -> {
            paused[0] = reactions[0] == countsWhenPaused[0] && reactions[1] == countsWhenPaused[1];
            JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", false);
        });
        h.runAfterDelay(435, () -> {
            try {
                h.assertTrue(stable[0], "Villager Brain and catgirl navigation cannot make participating owners wander" + movementFailure[0]);
                h.assertTrue(facing[0], "Both kinds of owner face their own player during the scene");
                h.assertTrue(reactions[0] > 0 && reactions[1] > 0 && reactions[2] > 0 && reactions[3] > 0,
                        "Without commands, the owner performs fish, sit, head pat, stand and follow beats");
                h.assertTrue(a.p.getFoodData().getFoodLevel() > 5, "The proactive feeding beat actually feeds the player");
                h.assertTrue(paused[0], "Pausing proactive interaction keeps actors steady without more fish or pat events");
                h.assertTrue(!npc.isNoAi() && !cat.isNoAi(), "Both actors retain their original AI flags");
                h.assertTrue(npc.position().distanceToSqr(anchor) > 0.1 && cat.position().distanceToSqr(catAnchor) > 0.1,
                        "Turning off absurdity lets both owners navigate normally again");
                h.succeed();
            } finally {
                JRConfig.CONFIG_BUILDER.setBoolean("fun.absurdity", old);
                rules.set(spawnMobs, oldSpawning, h.getLevel().getServer());
            }
        });
    }
}
