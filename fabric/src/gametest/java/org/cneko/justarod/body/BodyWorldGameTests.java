package org.cneko.justarod.body;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;

public class BodyWorldGameTests {
    @GameTest(maxTicks = 20)
    public void dimensionTypeIsRegistered(GameTestHelper h) {
        var key = ResourceKey.create(Registries.DIMENSION_TYPE,
                Identifier.fromNamespaceAndPath("justarod", "inside_body"));
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).get(key).isPresent(),
                "Body dimension type must load from the mod's data pack");
        var resource = h.getLevel().getServer().getResourceManager().getResource(
                Identifier.fromNamespaceAndPath("justarod", "dimension/inside_body.json"));
        h.assertTrue(resource.isPresent(), "Body dimension definition must remain present while disabled");
        try (var reader = resource.orElseThrow().openAsReader()) {
            var parsed = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess()),
                    JsonParser.parseReader(reader));
            h.assertTrue(parsed.result().isPresent(), "Body dimension JSON must parse: " + parsed.error());
        } catch (IOException e) {
            throw new AssertionError("Cannot read body dimension definition", e);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void regionsStayUniqueAcrossSaveAndReset(GameTestHelper h) {
        BodyRegionData data = new BodyRegionData();
        Set<Long> centers = new HashSet<>();
        UUID first = UUID.randomUUID();
        long old = data.regionFor(first);
        for (int i = 0; i < 10000; i++) {
            long slot = data.regionFor(UUID.randomUUID());
            long center = (((long) BodyRegionData.centerX(slot)) << 32)
                    ^ (BodyRegionData.centerZ(slot) & 0xffffffffL);
            h.assertTrue(centers.add(center), "Each slot must have a unique center");
            h.assertTrue(BodyRegionData.contains(slot, BodyRegionData.centerX(slot), BodyRegionData.centerZ(slot)),
                    "The slot center must be inside its region");
            h.assertTrue(!BodyRegionData.contains(old, BodyRegionData.centerX(slot), BodyRegionData.centerZ(slot)),
                    "Another owner's center must not be inside the first owner's region");
        }
        long last = BodyRegionData.CAPACITY - 1;
        h.assertTrue(Math.abs(BodyRegionData.centerX(last)) + BodyRegionData.REGION_X / 2 < 30_000_000
                        && Math.abs(BodyRegionData.centerZ(last)) + BodyRegionData.REGION_Z / 2 < 30_000_000,
                "Even the last allocatable region must stay inside the coordinate limit");
        long fresh = data.reset(first);
        h.assertTrue(fresh != old, "Reset must retire the former slot");
        UUID visitor = UUID.randomUUID();
        data.putVisit(visitor, new BodyRegionData.Visit(first, fresh, "minecraft:overworld",
                12.5, 70, -8.5, 45, 10));
        BodyRegionData loaded = BodyRegionData.fromTag(data.toTag());
        h.assertTrue(loaded.regionFor(first) == fresh, "Save reload must retain current ownership");
        h.assertTrue(loaded.visit(visitor).slot() == fresh && loaded.visit(visitor).x() == 12.5,
                "Save reload must retain visitor return information");
        h.assertTrue(loaded.regionFor(UUID.randomUUID()) > fresh, "Retired slots must not be reused");
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void worldSavedDataSurvivesDiskReload(GameTestHelper h) {
        var server = h.getLevel().getServer();
        UUID owner = UUID.randomUUID();
        long slot = BodyRegionData.get(server).regionFor(owner);
        server.overworld().getDataStorage().saveAndJoin();
        // 26.x stores a dimension's SavedData under its own dimensions/<namespace>/<path>/data folder.
        var dataFolder = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions/minecraft/overworld/data");
        try (var reloaded = new SavedDataStorage(dataFolder,
                server.getFixerUpper(), server.registryAccess())) {
            BodyRegionData fromDisk = reloaded.get(BodyRegionData.TYPE);
            h.assertTrue(fromDisk != null && fromDisk.currentRegion(owner) == slot,
                    "SavedData must reload ownership from the world data folder");
        }
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void hostDeathAllocatesFreshRegion(GameTestHelper h) {
        var host = h.makeMockServerPlayerInLevel();
        host.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BodyRegionData data = BodyRegionData.get(h.getLevel().getServer());
        long old = data.regionFor(host.getUUID());
        host.die(host.damageSources().genericKill());
        h.assertTrue(data.currentRegion(host.getUUID()) != old,
                "A host death must retire the old region and assign a new one");
        h.succeed();
    }
}
