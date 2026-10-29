package org.cneko.justarod.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.slime.SlimeAbility;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

import java.util.UUID;

/** Integration tests with real entities, ticking, registries and the registered Mixins. */
public class SlimeGameTests {
    private TamedSlimeEntity movingSlime(GameTestHelper h) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(x, 1, z, Blocks.STONE);
        TamedSlimeEntity slime = slime(h);
        slime.setTierLevel(3);
        slime.setHealth(slime.getMaxHealth());
        slime.setNoAi(false);
        slime.setCommand(TamedSlimeEntity.COMMAND_FOLLOW);
        return slime;
    }

    @GameTest(maxTicks = 70)
    public void enteringWithoutInputStopsPreviousMovement(GameTestHelper h) {
        TamedSlimeEntity slime = movingSlime(h);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        slime.hopToward(slime.getX(), slime.getY(), slime.getZ() + 5, 1, false);
        slime.setDeltaMovement(0, 0, 0.2);
        owner.setLastClientInput(Input.EMPTY);
        h.assertTrue(slime.swallowOwner(owner), "Enter as driver with no input");
        Vec3 start = slime.position();
        h.runAfterDelay(5, () -> {
            h.assertTrue(slime.position().subtract(start).horizontalDistanceSqr() < 0.001,
                    "Entering without input must stop horizontal movement, not carry the old velocity");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 70)
    public void releasingDriverInputStopsHorizontalMovement(GameTestHelper h) {
        TamedSlimeEntity slime = movingSlime(h);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        h.assertTrue(slime.swallowOwner(owner), "Enter as driver");
        Vec3 start = slime.position();
        owner.setLastClientInput(new Input(true, false, false, false, false, false, false));
        h.runAfterDelay(8, () -> {
            h.assertTrue(slime.position().subtract(start).horizontalDistanceSqr() > 0.01, "Forward input must still move the slime");
            owner.setLastClientInput(Input.EMPTY);
            Vec3 stopped = slime.position();
            h.runAfterDelay(5, () -> {
                h.assertTrue(slime.position().subtract(stopped).horizontalDistanceSqr() < 0.001,
                        "Releasing movement input must stop the slime even during a hop");
                h.succeed();
            });
        });
    }

    @GameTest(maxTicks = 70)
    public void captiveOwnerDoesNotKeepWalkingWithNoPrey(GameTestHelper h) {
        TamedSlimeEntity slime = movingSlime(h);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        slime.hopToward(slime.getX(), slime.getY(), slime.getZ() + 5, 1, false);
        slime.setDeltaMovement(0, 0, 0.2);
        h.assertTrue(slime.trySwallow(owner), "Passive entry remains captivity");
        h.assertTrue(slime.isCaptive(owner), "Player is not a driver");
        h.runAfterDelay(20, () -> {
            Vec3 stopped = slime.position();
            h.runAfterDelay(20, () -> {
                h.assertTrue(slime.position().subtract(stopped).horizontalDistanceSqr() < 0.001,
                        "AI with a captive owner and no prey must not keep walking");
                h.succeed();
            });
        });
    }

    @GameTest(maxTicks = 120)
    public void captiveOwnerStillAllowsAiHunting(GameTestHelper h) {
        TamedSlimeEntity slime = movingSlime(h);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        h.assertTrue(slime.trySwallow(owner), "Owner enters as captive");
        Mob prey = h.spawnWithNoFreeWill(EntityType.CHICKEN, 6, 2, 3);
        h.succeedWhen(() -> {
            h.assertTrue(prey.getVehicle() == slime, "Captive passengers must not prevent AI from approaching and swallowing real prey");
            h.assertTrue(slime.isCaptive(owner), "Hunting does not grant the captive owner driving");
        });
    }

    @GameTest(maxTicks = 60)
    public void idleMovementGuardStillAllowsFollowingOwner(GameTestHelper h) {
        TamedSlimeEntity slime = movingSlime(h);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        owner.teleportTo(slime.getX() + 9, slime.getY(), slime.getZ());
        Vec3 start = slime.position();
        h.runAfterDelay(15, () -> {
            h.assertTrue(slime.getX() > start.x + 0.1, "Real follow-owner AI must still move towards the owner");
            h.succeed();
        });
    }

    private TamedSlimeEntity slime(GameTestHelper h) {
        TamedSlimeEntity slime = h.spawn(JREntities.TAMED_SLIME, 3, 2, 3);
        slime.setTierLevel(4);
        slime.setHealth(slime.getMaxHealth());
        slime.setCommand(TamedSlimeEntity.COMMAND_STAY);
        slime.setNoAi(true);
        return slime;
    }

    private CompoundTag save(GameTestHelper h, TamedSlimeEntity slime) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        slime.addAdditionalSaveData(out);
        return out.buildResult();
    }

    private void load(GameTestHelper h, TamedSlimeEntity slime, CompoundTag tag) {
        slime.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
    }

    private ServerPlayer survivalPlayer(GameTestHelper h) {
        var level = h.getLevel();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "slime-survival-test"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    private void seedCaptivityAge(GameTestHelper h, TamedSlimeEntity slime, Player player, int age) {
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        out.list("Players", net.minecraft.core.UUIDUtil.CODEC).add(player.getUUID());
        out.list("Ages", com.mojang.serialization.Codec.INT).add(age);
        slime.captivity().load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
    }

    @GameTest(maxTicks = 60)
    public void levelFiveRevokesExistingDriverAndCannotBeManaged(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        h.assertTrue(slime.swallowOwner(owner) && slime.isDriver(owner), "Before level 5 the owner can drive");
        slime.setTierLevel(5);
        h.assertTrue(slime.isUncontrolled() && slime.isCaptive(owner), "Upgrade converts existing driver to captive");
        h.assertTrue(slime.getControllingPassenger() == null, "No controlling passenger at level 5");
        h.assertFalse(slime.canManage(owner) || slime.triggerAbility(owner) || slime.triggerDevour(owner), "Owner loses command and ability permissions");
        h.assertFalse(slime.storage().isStruggleImmune(owner.getUUID()), "Former driver can struggle");
        owner.setLastClientInput(new Input(false, false, false, false, false, true, false));
        h.runAfterDelay(15, () -> {
            h.assertTrue(owner.getVehicle() == slime, "Sneaking must not release a level 5 captive");
            h.assertTrue(owner.getHealth() == owner.getMaxHealth(), "The player remains unharmed during grace");
            h.assertTrue(slime.captivity().age(owner.getUUID()) >= 15, "Captivity timer runs independently of catalysts");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void levelFiveAutonomouslyDevoursOwnerAndOtherPlayers(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        slime.setTierLevel(5);
        slime.setNoAi(false);
        slime.setCommand(TamedSlimeEntity.COMMAND_STAY);
        ServerPlayer owner = survivalPlayer(h);
        ServerPlayer other = survivalPlayer(h);
        slime.setOwner(owner);
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        owner.teleportTo(slime.getX() + 3, slime.getY(), slime.getZ());
        other.teleportTo(slime.getX() - 3, slime.getY(), slime.getZ());
        h.runAfterDelay(6, () -> {
            h.assertTrue(slime.isDevouring(), "Level 5 automatically starts enlargement: tick=" + slime.tickCount + ", tier=" + slime.getTierLevel() + ", cooldown=" + slime.getDevourCooldown() + ", hunt=" + slime.canHunt(owner) + ", distance=" + slime.distanceToSqr(owner) + ", players=" + h.getLevel().getEntitiesOfClass(Player.class, slime.getBoundingBox().inflate(6)).size());
            h.assertTrue(slime.getDevourCooldown() > 180 && slime.getDevourCooldown() <= 200, "Devour cooldown is 10 seconds");
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(slime.isCaptive(owner) && slime.isCaptive(other), "Both owner and other players are swallowed as captives");
            h.assertTrue(owner.getHealth() == owner.getMaxHealth() && other.getHealth() == other.getMaxHealth(), "No immediate player digestion");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 160)
    public void levelFiveGraceThenSlowDamageAndSavedProgress(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        slime.setTierLevel(5);
        slime.setDirectDigestion(true);
        ServerPlayer player = survivalPlayer(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(slime.trySwallow(player), "Capture player");
        float health = player.getHealth();
        seedCaptivityAge(h, slime, player, JRConfig.getSlimePlayerGraceTicks() - 5);
        h.runAfterDelay(4, () -> {
            h.assertTrue(player.getHealth() == health && !slime.captivity().isDigesting(player), "Grace period does not damage or digest players");
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(slime.captivity().isDigesting(player) && player.getHealth() == health, "Digestion begins without an instant kill");
            int age = slime.captivity().age(player.getUUID());
            load(h, slime, save(h, slime));
            h.assertTrue(slime.captivity().age(player.getUUID()) == age, "Saving and loading preserves the grace/digestion clock");
        });
        h.runAfterDelay(130, () -> {
            h.assertTrue(player.isAlive() && player.getHealth() == health - JRConfig.getSlimePlayerDamage(), "One gradual damage pulse; natural regeneration cannot cancel digestion: health=" + player.getHealth() + ", age=" + slime.captivity().age(player.getUUID()));
            slime.applyStruggle(player, slime.struggleThreshold());
            h.assertTrue(player.getVehicle() == null && !slime.storage().containsEntity(player.getUUID()), "Struggling remains possible during digestion");
            h.assertTrue(slime.captivity().age(player.getUUID()) == 0, "Escape clears the digestion clock");
            h.assertFalse(slime.canHunt(player), "A short escape protection prevents immediate recapture");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 200)
    public void shulkerGathersAboveHeadDropsAndSwallows(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        CompoundTag tag = save(h, slime);
        CompoundTag ability = new CompoundTag();
        ability.putInt("Ability", SlimeAbility.SHULKER.id());
        tag.put("AbsorbedAbility", ability);
        load(h, slime, tag);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        h.assertTrue(slime.swallowOwner(owner), "Drive below level 5");
        Mob prey = h.spawn(EntityType.COW, 6, 2, 3);
        h.assertTrue(slime.triggerAbility(owner), "Begin lift and gather");
        h.runAfterDelay(60, () -> {
            h.assertTrue(prey.getY() > slime.getY() + slime.getBbHeight(), "Prey visibly gathers above the slime head");
            h.assertTrue(prey.position().subtract(slime.position()).horizontalDistanceSqr() < 0.5, "Prey is concentrated over the slime");
            h.assertTrue(prey.getVehicle() == null, "Prey is not swallowed before the drop");
        });
        h.runAfterDelay(170, () -> {
            h.assertTrue(prey.getVehicle() == slime, "Dropping prey is swallowed through the enlargement animation");
            h.assertFalse(prey.isNoGravity(), "Original gravity is restored");
            h.assertTrue(slime.releaseEntity(prey.getUUID()), "Can release gathered prey");
            h.assertFalse(prey.isNoAi(), "Release restores AI from before lifting");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 130)
    public void acquireOnlyAfterDigestionAndKeepStoredItems(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        slime.setDirectDigestion(true);
        slime.storage().addItem(new ItemStack(Items.DIAMOND, 3), slime.tier().internalSpace());
        Mob prey = h.spawn(EntityType.CREEPER, 4, 2, 3);
        h.assertTrue(slime.trySwallow(prey), "Creeper should be swallowed");
        int used = slime.storage().usedSpace();
        h.runAfterDelay(30, () -> {
            h.assertTrue(slime.getAbility() == SlimeAbility.NONE, "Swallowing alone must not grant an ability");
            h.assertTrue(slime.storage().usedSpace() == used, "Digesting prey must still reserve its space");
            h.assertTrue(slime.storage().containsEntity(prey.getUUID()), "Digesting prey remains in the ledger");
        });
        h.runAfterDelay(90, () -> {
            h.assertTrue(prey.isDeadOrDying() || prey.isRemoved(), "Creeper should finish digestion");
            h.assertTrue(slime.getAbility() == SlimeAbility.CREEPER, "Successful digestion grants creeper ability");
            h.assertTrue(slime.storage().items().getFirst().getCount() == 3, "Automatic digestion must preserve stored diamonds");
            h.assertFalse(slime.storage().containsEntity(prey.getUUID()), "Settlement returns prey space");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 140)
    public void releaseCancelsDigestionAndRestoresAi(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        slime.setDirectDigestion(true);
        Mob prey = h.spawn(EntityType.CREEPER, 4, 2, 3);
        h.assertTrue(slime.trySwallow(prey), "Swallow");
        h.assertFalse(slime.releaseEntity(UUID.randomUUID()), "Arbitrary world entity UUIDs must be rejected");
        h.runAfterDelay(25, () -> {
            h.assertTrue(prey.isNoAi(), "Contained mob AI is disabled");
            h.assertTrue(slime.releaseEntity(prey.getUUID()), "Release real contained prey");
            h.assertFalse(prey.isNoAi(), "Release restores original AI");
        });
        h.runAfterDelay(110, () -> {
            h.assertTrue(prey.isAlive(), "Released prey must survive the old digestion deadline");
            h.assertTrue(slime.getAbility() == SlimeAbility.NONE, "Cancelled digestion must not award an ability");
            h.assertTrue(slime.storage().containedEntities().isEmpty(), "Released prey is not reattached");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 140)
    public void interruptedDigestionResumesWithoutChargingTwice(GameTestHelper h) {
        TamedSlimeEntity original = slime(h);
        original.setDirectDigestion(true);
        Mob prey = h.spawn(EntityType.CREEPER, 4, 2, 3);
        h.assertTrue(original.trySwallow(prey), "Swallow");
        h.runAfterDelay(30, () -> {
            float health = original.getHealth();
            CompoundTag saved = save(h, original);
            original.discard();
            TamedSlimeEntity restored = slime(h);
            load(h, restored, saved);
            h.assertTrue(prey.startRiding(restored, true, true), "Restore vanilla passenger relationship");
            h.assertTrue(restored.isDirectDigestion(), "Per-slime mode survives load");
            h.assertTrue(restored.storage().containsEntity(prey.getUUID()), "Pending prey UUID survives load");
            h.runAfterDelay(40, () -> {
                h.assertTrue(prey.isAlive(), "Saved remaining duration must not settle too early");
                h.assertTrue(restored.getHealth() == health, "Resuming must not charge the health cost again");
            });
            h.runAfterDelay(60, () -> {
                h.assertTrue(restored.getAbility() == SlimeAbility.CREEPER, "Resume the pending digestion, not a new full step");
                h.assertFalse(restored.storage().containsEntity(prey.getUUID()), "No leaked ledger entry");
                h.succeed();
            });
        });
    }

    @GameTest(maxTicks = 120)
    public void catalystModeDoesNotStartAutomatically(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        slime.setDirectDigestion(false);
        Mob prey = h.spawn(EntityType.SHULKER, 4, 2, 3);
        h.assertTrue(slime.trySwallow(prey), "Swallow shulker");
        h.runAfterDelay(20, () -> {
            h.assertFalse(slime.isDecomposing(), "Catalyst mode must wait");
            h.assertTrue(slime.addDecomposeTask(), "One catalyst batch starts digestion");
            h.assertFalse(slime.addDecomposeTask(), "Cannot add another batch while busy");
        });
        h.runAfterDelay(105, () -> {
            h.assertTrue(slime.getAbility() == SlimeAbility.SHULKER, "Catalyst digestion also grants abilities");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void digestionShrinksOnlyActivePreyAndReleaseRestoresScale(GameTestHelper h) {
        var slime = slime(h);
        slime.setDirectDigestion(true);
        var first = h.spawnWithNoFreeWill(EntityType.COW, 5, 2, 3);
        first.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(0.8);
        first.refreshDimensions();
        double originalWidth = first.getBbWidth();
        var second = h.spawnWithNoFreeWill(EntityType.COW, 6, 2, 3);
        h.assertTrue(slime.trySwallow(first) && slime.trySwallow(second), "Swallow two prey");
        int reserved = slime.storage().usedSpace();
        h.runAfterDelay(30, () -> {
            h.assertTrue(first.getScale() < 0.8 && first.getScale() > 0.4, "Active prey gradually shrinks");
            h.assertTrue(first.getBbWidth() < originalWidth, "Real collision dimensions shrink with the model");
            h.assertTrue(second.getScale() == 1, "Waiting prey does not shrink early");
            h.assertTrue(slime.storage().usedSpace() == reserved, "Shrinking does not prematurely free reserved space");
            slime.releaseEntity(first.getUUID());
            h.assertTrue(Math.abs(first.getScale() - 0.8) < 0.001 && Math.abs(first.getBbWidth() - originalWidth) < 0.001,
                    "Escape restores original scale without overwriting another scale modifier");
            slime.releaseAllEntities();
            h.succeed();
        });
    }

    @GameTest(maxTicks = 150)
    public void floatingPlayersMoveWithinBodyAndPreserveCapacity(GameTestHelper h) {
        var slime = slime(h);
        slime.setTierLevel(5);
        var player = survivalPlayer(h);
        var other = survivalPlayer(h);
        slime.setOwner(player);
        h.assertTrue(slime.trySwallow(player) && slime.trySwallow(other), "Two captives enter");
        int reserved = slime.storage().usedSpace();
        float originalEyeHeight = player.getEyeHeight();
        Vec3 slimeStart = slime.position();
        h.runAfterDelay(5, () -> {
            Vec3 start = slime.captivity().offset(player);
            player.setYRot(0);
            player.setLastClientInput(new Input(true, false, false, false, true, false, false));
            h.runAfterDelay(20, () -> {
                Vec3 moved = slime.captivity().offset(player);
                h.assertTrue(moved.z > start.z + 0.5 && moved.y > start.y + 0.4, "Input moves the player horizontally and upward inside the body");
                h.assertTrue(slime.position().subtract(slimeStart).horizontalDistanceSqr() < 0.001, "Captive input does not drive the slime");
                h.assertTrue(Math.abs(slime.captivity().offset(other).z) < 0.5, "Another captive has independent relative position");
            });
        });
        h.runAfterDelay(45, () -> {
            int duration = (int) Math.ceil(player.getHealth() / JRConfig.getSlimePlayerDamage()) * JRConfig.getSlimePlayerDamageInterval();
            seedCaptivityAge(h, slime, player, JRConfig.getSlimePlayerGraceTicks() + duration / 2);
        });
        h.runAfterDelay(90, () -> {
            player.setLastClientInput(Input.EMPTY);
            h.assertTrue(player.getScale() < 0.6 && player.getEyeHeight() < originalEyeHeight * 0.65, "Player size and first-person eye height shrink during digestion");
            h.assertTrue(slime.storage().usedSpace() == reserved, "Original captive volume remains reserved");
            var box = player.getBoundingBox();
            var body = slime.getBoundingBox();
            h.assertTrue(box.minX >= body.minX && box.maxX <= body.maxX && box.minY >= body.minY && box.maxY <= body.maxY
                    && box.minZ >= body.minZ && box.maxZ <= body.maxZ, "Holding movement cannot cross the body walls or ceiling");
            Vec3 offset = slime.captivity().offset(player);
            load(h, slime, save(h, slime));
            h.assertTrue(slime.captivity().offset(player).distanceToSqr(offset) < 0.001, "Save/load preserves floating position");
            slime.applyStruggle(player, slime.struggleThreshold());
            h.assertTrue(player.getVehicle() == null && player.getScale() == 1, "Struggle releases player and restores normal scale");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 130)
    public void digestionAbsorbsDropsAndXpWithoutTakingNearbyLoot(GameTestHelper h) {
        var slime = slime(h);
        slime.setOwner(survivalPlayer(h));
        slime.setDirectDigestion(true);
        var cow = h.spawnWithNoFreeWill(EntityType.COW, 5, 2, 3);
        var unrelated = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), slime.getX(), slime.getY(), slime.getZ(), new ItemStack(Items.DIAMOND));
        unrelated.setNeverPickUp();
        h.getLevel().addFreshEntity(unrelated);
        var oldOrb = new net.minecraft.world.entity.ExperienceOrb(h.getLevel(), slime.getX(), slime.getY(), slime.getZ(), 11);
        h.getLevel().addFreshEntity(oldOrb);
        h.assertTrue(slime.trySwallow(cow), "Swallow cow for real death loot");
        h.runAfterDelay(110, () -> {
            h.assertTrue(slime.storage().items().stream().anyMatch(stack -> stack.is(Items.BEEF)), "Vanilla prey drops enter storage");
            h.assertTrue(slime.getDigestedExperience() > 0, "Vanilla kill experience is absorbed");
            h.assertTrue(!unrelated.isRemoved() && unrelated.getItem().is(Items.DIAMOND) && !oldOrb.isRemoved(), "Existing nearby items and experience are not vacuumed up");
            int xp = slime.getDigestedExperience();
            load(h, slime, save(h, slime));
            h.assertTrue(slime.getDigestedExperience() == xp, "Absorbed experience survives save/load");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 50)
    public void slowPlayerDigestionCollectsLootAndKeepsOverflow(GameTestHelper h) {
        var slime = slime(h);
        slime.setTierLevel(5);
        var player = survivalPlayer(h);
        player.setHealth(1);
        player.experienceLevel = 5;
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.GOLD_INGOT));
        h.assertTrue(slime.trySwallow(player), "Capture player");
        while (slime.storage().freeSpace(slime.tier().internalSpace()) > 0) slime.storage().addItem(new ItemStack(Items.STONE), slime.tier().internalSpace());
        seedCaptivityAge(h, slime, player, JRConfig.getSlimePlayerGraceTicks() + JRConfig.getSlimePlayerDamageInterval() - 1);
        h.runAfterDelay(20, () -> {
            h.assertFalse(player.isAlive(), "Slow digestion finishes through normal player death");
            int stored = slime.storage().items().stream().filter(stack -> stack.is(Items.GOLD_INGOT)).mapToInt(ItemStack::getCount).sum();
            int dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, slime.getBoundingBox().inflate(6),
                    item -> item.getItem().is(Items.GOLD_INGOT)).stream().mapToInt(item -> item.getItem().getCount()).sum();
            h.assertTrue(stored > 0 && dropped > 0 && stored + dropped == 36, "Capacity overflow remains real dropped loot without loss or duplication");
            h.assertTrue(slime.getDigestedExperience() > 0, "Player death experience is also absorbed");
            h.assertFalse(slime.storage().containsEntity(player.getUUID()), "Death frees the original captive volume");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void captiveOwnerCannotDriveOrManageAndCanEscape(GameTestHelper h) {
        h.assertTrue(JRConfig.isSlimePlayerCaptivity(), "Test uses default player captivity configuration");
        TamedSlimeEntity slime = slime(h);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        slime.setDirectDigestion(true);
        h.assertTrue(slime.trySwallow(owner), "Passive swallow owner");
        h.assertTrue(slime.isCaptive(owner), "Owner swallowed as prey is a captive");
        h.assertTrue(slime.getControllingPassenger() == null, "Captive owner cannot drive");
        h.assertFalse(slime.canManage(owner), "Captive owner cannot open bag or change commands");
        h.assertFalse(slime.triggerDevour(owner), "Captive owner cannot cast devour");
        h.assertFalse(slime.triggerAbility(owner), "Captive owner cannot cast ability");
        h.runAfterDelay(90, () -> {
            h.assertTrue(owner.isAlive(), "Automatic mode must not digest a captive player");
            h.assertTrue(owner.getVehicle() == slime, "Player follows slime as passenger");
            h.assertFalse(slime.storage().isStruggleImmune(owner.getUUID()), "Passive owner must be able to struggle");
            slime.applyStruggle(owner, slime.struggleThreshold());
            h.assertTrue(owner.getVehicle() == null, "Struggle releases owner");
            h.assertTrue(slime.swallowOwner(owner), "Owner can voluntarily enter after release");
            h.assertTrue(slime.isDriver(owner) && slime.getControllingPassenger() == owner, "Voluntary entry grants driving");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void explosionProtectsDriverAndSurvives(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        CompoundTag tag = save(h, slime);
        CompoundTag ability = new CompoundTag();
        ability.putInt("Ability", SlimeAbility.CREEPER.id());
        tag.put("AbsorbedAbility", ability);
        load(h, slime, tag);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        h.assertTrue(slime.swallowOwner(owner), "Owner drives");
        float ownerHealth = owner.getHealth();
        float slimeHealth = slime.getHealth();
        h.assertTrue(slime.triggerAbility(owner), "Prime explosion");
        h.assertTrue(slime.isAbilityCharging(), "Charge is synchronized");
        h.runAfterDelay(JRConfig.getSlimeFuseTicks() + 10, () -> {
            h.assertTrue(slime.isAlive() && slime.getHealth() == slimeHealth, "Ability explosion must preserve slime");
            h.assertTrue(owner.isAlive() && owner.getHealth() == ownerHealth, "Ability explosion must protect driver");
            h.assertTrue(owner.getVehicle() == slime, "Explosion must not eject driver");
            h.assertTrue(slime.getAbility() == SlimeAbility.CREEPER && slime.getAbilityCooldown() > 0, "Reusable ability enters cooldown");
            h.assertFalse(slime.triggerAbility(owner), "Cannot spam explosion during cooldown");
            CompoundTag saved = save(h, slime);
            TamedSlimeEntity copy = slime(h);
            load(h, copy, saved);
            h.assertTrue(copy.getAbility() == SlimeAbility.CREEPER && copy.getAbilityCooldown() > 0, "Ability and cooldown survive reload");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void teleportHuntsAndSwallowsPrey(GameTestHelper h) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(x, 1, z, Blocks.STONE);
        TamedSlimeEntity slime = slime(h);
        CompoundTag tag = save(h, slime);
        CompoundTag ability = new CompoundTag();
        ability.putInt("Ability", SlimeAbility.ENDERMAN.id());
        tag.put("AbsorbedAbility", ability);
        load(h, slime, tag);
        slime.setTierLevel(3);
        slime.setHealth(slime.getMaxHealth());
        slime.setDirectDigestion(false);
        Mob prey = h.spawnWithNoFreeWill(EntityType.COW, 6, 2, 6);
        Vec3 start = slime.position();
        slime.setTarget(prey);
        slime.setCommand(TamedSlimeEntity.COMMAND_FOLLOW);
        h.runAfterDelay(7, () -> {
            h.assertTrue(slime.position().distanceToSqr(start) > 0.1, "Teleport changes position towards the selected prey");
            h.assertTrue(h.getLevel().noBlockCollision(slime, slime.getBoundingBox()), "Landing must fit the entire slime");
            h.assertTrue(slime.getAbilityCooldown() > 0, "Successful hunt enters cooldown");
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(prey.getVehicle() == slime && slime.storage().containsEntity(prey.getUUID()), "Teleport must finish by swallowing its prey");
            h.assertTrue(slime.getAbility() == SlimeAbility.ENDERMAN, "Ability remains reusable");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void shulkerPulseLiftsPreyAndProtectsOwner(GameTestHelper h) {
        TamedSlimeEntity slime = slime(h);
        CompoundTag tag = save(h, slime);
        CompoundTag ability = new CompoundTag();
        ability.putInt("Ability", SlimeAbility.SHULKER.id());
        tag.put("AbsorbedAbility", ability);
        load(h, slime, tag);
        ServerPlayer owner = MockPlayers.serverPlayerInLevel(h);
        slime.setOwner(owner);
        h.assertTrue(slime.swallowOwner(owner), "Owner drives");
        Mob prey = h.spawnWithNoFreeWill(EntityType.COW, 6, 2, 3);
        double startY = prey.getY();
        h.assertTrue(slime.triggerAbility(owner), "Use levitation pulse");
        h.assertTrue(prey.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION), "Pulse affects surrounding prey");
        h.assertFalse(owner.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION), "Owner remains protected");
        h.runAfterDelay(15, () -> {
            h.assertTrue(prey.getY() > startY + 0.25, "Levitation must visibly lift the prey");
            h.succeed();
        });
    }
}
