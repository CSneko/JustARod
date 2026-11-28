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
import org.cneko.justarod.test.MockPlayers;

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
        var host = MockPlayers.serverPlayerInLevel(h);
        host.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BodyRegionData data = BodyRegionData.get(h.getLevel().getServer());
        long old = data.regionFor(host.getUUID());
        host.die(host.damageSources().genericKill());
        h.assertTrue(data.currentRegion(host.getUUID()) != old,
                "A host death must retire the old region and assign a new one");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void ascarisRegionPersistsAcrossHostReentryAndRetiresOnDeath(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var owner = MockPlayers.serverPlayerInLevel(h);
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

        var ascaris = org.cneko.justarod.entity.JREntities.ASCARIS.create(
                h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris entity must create cleanly");
        ascaris.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ());
        ascaris.setTame(true, true);
        ascaris.tame(owner);
        h.getLevel().addFreshEntity(ascaris);

        BodyRegionData data = BodyRegionData.get(server);
        UUID originalRegionOwner = ascaris.getBodyRegionOwnerUuid();
        long originalSlot = data.regionFor(originalRegionOwner);

        // Round-trip HOST_ASCARIS visit through NBT
        BodyRegionData temp = new BodyRegionData();
        long tempSlot = temp.regionFor(originalRegionOwner);
        UUID visitorUuid = UUID.randomUUID();
        temp.putVisit(visitorUuid, new BodyRegionData.Visit(originalRegionOwner, tempSlot,
                "minecraft:overworld", 10.5, 80, -4.5, 90f, 5f,
                BodyRegionData.HOST_ASCARIS, ascaris.getUUID(), true));
        BodyRegionData reloaded = BodyRegionData.fromTag(temp.toTag());
        var loadedVisit = reloaded.visit(visitorUuid);
        h.assertTrue(loadedVisit != null && loadedVisit.isAscaris()
                        && loadedVisit.host().equals(originalRegionOwner)
                        && loadedVisit.hostEntityUuid().equals(ascaris.getUUID())
                        && loadedVisit.yandereCling(),
                "Ascaris visit metadata (including yandereCling) must survive NBT round-trip");

        // Enter owner's host body and summon back -> must preserve the exact same body region UUID & slot!
        h.assertTrue(org.cneko.justarod.ascaris.AscarisHostController.enterHostBody(ascaris, owner, "manual"),
                "Ascaris enters owner host body");
        var restored = org.cneko.justarod.ascaris.AscarisHostController.summonFromHostBody(owner, false, false);
        h.assertTrue(restored != null, "Ascaris summons back out of owner host body");
        h.assertTrue(restored.getBodyRegionOwnerUuid().equals(originalRegionOwner),
                "Summoned Ascaris must keep her original body region owner UUID");
        h.assertTrue(data.regionFor(restored.getBodyRegionOwnerUuid()) == originalSlot,
                "Summoned Ascaris must keep her exact same body region slot");

        // Death retires old region and assigns a fresh slot
        restored.die(restored.damageSources().genericKill());
        h.assertTrue(data.currentRegion(originalRegionOwner) != originalSlot,
                "Ascaris death must retire her former body region slot");

        // All 12 custom Ascaris anatomy blocks must register with collectible BlockItems
        net.minecraft.world.level.block.Block[] anatomyBlocks = {
                org.cneko.justarod.block.JRBlocks.ASCARIS_CUTICLE,
                org.cneko.justarod.block.JRBlocks.ASCARIS_MUSCLE,
                org.cneko.justarod.block.JRBlocks.ASCARIS_MUCOSA,
                org.cneko.justarod.block.JRBlocks.ASCARIS_MUCOSA_STAIRS,
                org.cneko.justarod.block.JRBlocks.ASCARIS_NERVE_CORD,
                org.cneko.justarod.block.JRBlocks.ASCARIS_EXCRETORY_MEMBRANE,
                org.cneko.justarod.block.JRBlocks.ASCARIS_INTESTINAL_MEMBRANE,
                org.cneko.justarod.block.JRBlocks.ASCARIS_PSEUDOCOELOM_MEMBRANE,
                org.cneko.justarod.block.JRBlocks.ASCARIS_UTERINE_WALL,
                org.cneko.justarod.block.JRBlocks.ASCARIS_EGG_SAC,
                org.cneko.justarod.block.JRBlocks.ASCARIS_OVARY_TISSUE,
                org.cneko.justarod.block.JRBlocks.ASCARIS_CHYME_BLOCK,
                org.cneko.justarod.block.JRBlocks.ASCARIS_MICROVILLI,
                org.cneko.justarod.block.JRBlocks.ASCARIS_WARM_CUSHION
        };
        for (var block : anatomyBlocks) {
            h.assertTrue(block.asItem() != net.minecraft.world.item.Items.AIR,
                    "Custom Ascaris anatomy block must have a collectible item: " + block);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void customAscarisAnatomyBlocksHaveWorkingGameplayFeatures(GameTestHelper h) {
        var level = h.getLevel();
        var player = MockPlayers.serverPlayerInLevel(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

        // 1. 甜味食糜团：4 口可食用 + 3 种甜食风味（粉蜜 / 发光浆果夜视 / 蜂蜜金苹伤害吸收）
        var chymePos = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        level.setBlockAndUpdate(chymePos, org.cneko.justarod.block.JRBlocks.ASCARIS_CHYME_BLOCK.defaultBlockState());
        player.getFoodData().setFoodLevel(6);
        var hitChyme = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(chymePos), net.minecraft.core.Direction.UP, chymePos, false);
        level.getBlockState(chymePos).useWithoutItem(level, player, hitChyme);
        h.assertTrue(player.getFoodData().getFoodLevel() == 10, "Eating chyme restores 4 food");
        h.assertTrue(level.getBlockState(chymePos).getValue(org.cneko.justarod.block.ascaris.AscarisChymeBlock.BITES) == 1,
                "First bite increments chyme bites to 1");
        for (int i = 0; i < 3; i++) {
            player.getFoodData().setFoodLevel(6);
            level.getBlockState(chymePos).useWithoutItem(level, player, hitChyme);
        }
        h.assertTrue(level.getBlockState(chymePos).isAir(), "4th bite finishes the chyme block cleanly");

        // 1b. 发光浆果风味食糜 -> 夜视 & 亮度 12；蜂蜜金苹风味食糜 -> 伤害吸收 & 6 饱食
        var glowState = org.cneko.justarod.block.JRBlocks.ASCARIS_CHYME_BLOCK.defaultBlockState()
                .setValue(org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR,
                        org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_GLOW);
        h.assertTrue(glowState.getLightEmission() == 12, "Glow-berry chyme emits light level 12");
        level.setBlockAndUpdate(chymePos, glowState);
        level.getBlockState(chymePos).useWithoutItem(level, player, hitChyme);
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),
                "Glow-berry chyme grants Night Vision");

        var honeyState = org.cneko.justarod.block.JRBlocks.ASCARIS_CHYME_BLOCK.defaultBlockState()
                .setValue(org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR,
                        org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_HONEY_GOLD);
        h.assertTrue(honeyState.getLightEmission() == 10, "Honey-gold chyme emits light level 10");
        level.setBlockAndUpdate(chymePos, honeyState);
        player.getFoodData().setFoodLevel(10);
        level.getBlockState(chymePos).useWithoutItem(level, player, hitChyme);
        h.assertTrue(player.getFoodData().getFoodLevel() == 16
                        && player.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION),
                "Honey-gold chyme restores 6 food and grants Absorption");

        // 1c. 「暖腹余温」增益可完全阻断生食野生蛔虫感染
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                org.cneko.justarod.effect.JREffects.Companion.getWARM_BELLY_EFFECT(), 400, 0));
        ((org.cneko.justarod.entity.Pregnant) (Object) player).setAscaris(0);
        for (int i = 0; i < 20; i++) {
            org.cneko.justarod.ascaris.AscarisInfectionController.onFinishConsuming(
                    player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH));
        }
        h.assertTrue(((org.cneko.justarod.entity.Pregnant) (Object) player).getAscaris() == 0,
                "Warm Belly effect blocks wild raw-food Ascaris infection");

        // 2. 蛔虫神经索：右键触发兴奋态，输出强度 10 的红石脉冲并在 16 tick 后恢复
        var nervePos = h.absolutePos(new net.minecraft.core.BlockPos(3, 2, 2));
        level.setBlockAndUpdate(nervePos, org.cneko.justarod.block.JRBlocks.ASCARIS_NERVE_CORD.defaultBlockState());
        var hitNerve = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(nervePos), net.minecraft.core.Direction.UP, nervePos, false);
        level.getBlockState(nervePos).useWithoutItem(level, player, hitNerve);
        h.assertTrue(level.getBlockState(nervePos).getValue(org.cneko.justarod.block.ascaris.AscarisNerveCordBlock.EXCITED),
                "Right-clicking nerve cord excites it");
        h.assertTrue(level.getBlockState(nervePos).getSignal(level, nervePos, net.minecraft.core.Direction.NORTH) == 10,
                "Excited nerve cord emits bio-electric redstone signal 10");

        // 3. 蛔虫排泄管膜：右键清除中毒等负面效果
        var excPos = h.absolutePos(new net.minecraft.core.BlockPos(4, 2, 2));
        level.setBlockAndUpdate(excPos, org.cneko.justarod.block.JRBlocks.ASCARIS_EXCRETORY_MEMBRANE.defaultBlockState());
        org.cneko.justarod.block.ascaris.AscarisExcretoryMembraneBlock.clearCooldown(player.getUUID());
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200, 0));
        var hitExc = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(excPos), net.minecraft.core.Direction.UP, excPos, false);
        level.getBlockState(excPos).useWithoutItem(level, player, hitExc);
        h.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                "Excretory membrane detoxes harmful Poison effect");

        // 4. 蛔虫卵囊块：空手右键剥离产出蛔虫卵胶囊
        var eggPos = h.absolutePos(new net.minecraft.core.BlockPos(5, 2, 2));
        level.setBlockAndUpdate(eggPos, org.cneko.justarod.block.JRBlocks.ASCARIS_EGG_SAC.defaultBlockState());
        var hitEgg = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(eggPos), net.minecraft.core.Direction.UP, eggPos, false);
        level.getBlockState(eggPos).useWithoutItem(level, player, hitEgg);
        h.assertTrue(level.getBlockState(eggPos).isAir(), "Harvesting egg sac consumes block");
        h.assertTrue(player.getInventory().countItem(org.cneko.justarod.item.JRItems.Companion.getASCARIS_EGG_CAPSULE()) >= 1,
                "Harvesting egg sac yields Ascaris Egg Capsule");

        // 4.5 柔暖体腔软垫：右键依偎回血并清除疲劳与宫寒
        var cushionPos = h.absolutePos(new net.minecraft.core.BlockPos(6, 2, 2));
        level.setBlockAndUpdate(cushionPos, org.cneko.justarod.block.JRBlocks.ASCARIS_WARM_CUSHION.defaultBlockState());
        org.cneko.justarod.block.ascaris.AscarisWarmCushionBlock.clearCooldown(player.getUUID());
        player.setHealth(12.0f);
        ((org.cneko.justarod.entity.Pregnant) (Object) player).setFatigue(5000);
        ((org.cneko.justarod.entity.Pregnant) (Object) player).setUterineCold(5000);
        var hitCushion = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(cushionPos), net.minecraft.core.Direction.UP, cushionPos, false);
        level.getBlockState(cushionPos).useWithoutItem(level, player, hitCushion);
        h.assertTrue(player.getHealth() > 12.0f, "Snuggling on warm body cushion heals player");
        h.assertTrue(((org.cneko.justarod.entity.Pregnant) (Object) player).getFatigue() == 0
                        && ((org.cneko.justarod.entity.Pregnant) (Object) player).getUterineCold() == 0,
                "Snuggling on warm body cushion clears fatigue and uterine cold");

        // 5. 解剖区域识别覆盖全部器官（含体温暖窝）
        long slot = 0L;
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        h.assertTrue(AscarisBodyInteractions.detectZone(slot, cx + 2, AscarisAnatomyBuilder.FLOOR_Y + 1, cz)
                == AscarisBodyInteractions.OrganZone.WARM_NOOK, "Detects Cozy Warm Nook");
        h.assertTrue(AscarisBodyInteractions.detectZone(slot, cx + AscarisAnatomyBuilder.NERVE_RING_X, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz)
                == AscarisBodyInteractions.OrganZone.NERVE_RING, "Detects Circumenteric Nerve Ring");
        h.assertTrue(AscarisBodyInteractions.detectZone(slot, cx, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz)
                == AscarisBodyInteractions.OrganZone.INTESTINE, "Detects Straight Intestine");
        h.assertTrue(AscarisBodyInteractions.detectZone(slot, cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X, AscarisAnatomyBuilder.FLOOR_Y + 1, cz)
                == AscarisBodyInteractions.OrganZone.EXCRETORY, "Detects H-Shaped Excretory Bridge");
        h.assertTrue(AscarisBodyInteractions.detectZone(slot, cx, AscarisAnatomyBuilder.FLOOR_Y + 1, cz + 9)
                == AscarisBodyInteractions.OrganZone.UTERUS, "Detects Paired Uterus");
        h.assertTrue(AscarisBodyInteractions.detectZone(slot, cx + 26, AscarisAnatomyBuilder.FLOOR_Y + 1, cz + 8)
                == AscarisBodyInteractions.OrganZone.OVARY, "Detects Coiled Ovaries");

        h.runAfterDelay(20, () -> {
            h.assertTrue(!level.getBlockState(nervePos).getValue(org.cneko.justarod.block.ascaris.AscarisNerveCordBlock.EXCITED),
                    "Nerve cord bio-electric pulse relaxes after 16 ticks");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void hardcorePhysiologyPersistenceAndBitPackingRoundTrip(GameTestHelper h) {
        BodyRegionData data = new BodyRegionData();
        UUID owner = UUID.randomUUID();
        long slot = data.regionFor(owner);
        AscarisPhysiologyState st = data.physiologyFor(slot);
        st.hydrostaticPressure = 82.0f;
        st.glycogen = 41.0f;
        st.metabolicWaste = 74.0f;
        st.temperatureC = 39.1f;
        st.ovaryProgress = 66.0f;
        st.uterineLoad = 88.0f;
        st.rectalLoad = 52.0f;
        st.drugType = AscarisPhysiologyState.DRUG_ALBENDAZOLE;
        st.drugTicks = 640;
        st.addBolus(-12.5, 1, org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_HONEY_GOLD, 72.0f);
        st.recordBreach(3, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, 4,
                AscarisPhysiologyState.ORGAN_GUT, "justarod:ascaris_intestinal_membrane");

        // 校验位打包编码与客户端解包精确一致
        int vitals = st.packVitalsInt();
        h.assertTrue((vitals & 0x7F) == 82, "Packed pressure matches 82");
        h.assertTrue(((vitals >> 7) & 0x7F) == 41, "Packed glycogen matches 41");
        h.assertTrue(((vitals >> 14) & 0x7F) == 74, "Packed waste matches 74");
        h.assertTrue(Math.abs(((300 + ((vitals >> 21) & 0x7F)) / 10.0f) - 39.1f) < 0.05f, "Packed temperature matches 39.1C");

        int organs = st.packOrgansInt();
        h.assertTrue((organs & 0x7F) == 88, "Packed uterine load matches 88");
        h.assertTrue(((organs >> 7) & 0x7F) == 52, "Packed rectal load matches 52");
        h.assertTrue(((organs >> 21) & 0x3) == AscarisPhysiologyState.DRUG_ALBENDAZOLE, "Packed drug type matches Albendazole");

        // 校验完整 NBT 存档往返与重置清理
        BodyRegionData reloaded = BodyRegionData.fromTag(data.toTag());
        h.assertTrue(reloaded.hasPhysiology(slot), "Reloaded BodyRegionData must retain physiology entry");
        AscarisPhysiologyState rs = reloaded.physiologyFor(slot);
        h.assertTrue(Math.abs(rs.hydrostaticPressure - 82.0f) < 0.1f && Math.abs(rs.glycogen - 41.0f) < 0.1f
                        && Math.abs(rs.metabolicWaste - 74.0f) < 0.1f && Math.abs(rs.temperatureC - 39.1f) < 0.1f,
                "Core physiological vitals survive NBT round-trip");
        h.assertTrue(rs.drugType == AscarisPhysiologyState.DRUG_ALBENDAZOLE && rs.drugTicks == 640,
                "Active pharmacology state survives NBT round-trip");
        h.assertTrue(rs.boluses.size() == 1 && rs.boluses.getFirst().flavor == org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_HONEY_GOLD,
                "Moving chyme boluses survive NBT round-trip");
        h.assertTrue(rs.breaches.size() == 1 && "justarod:ascaris_intestinal_membrane".equals(rs.breaches.getFirst().blockId),
                "Tissue breaches survive NBT round-trip");

        long fresh = reloaded.reset(owner);
        h.assertTrue(!reloaded.hasPhysiology(slot) && fresh != slot, "Resetting host clears retired slot physiology");
        h.succeed();
    }

    @GameTest(maxTicks = 60)
    public void hardcorePhysiologyOrgansPharmacologyAndRepairWork(GameTestHelper h) {
        var level = h.getLevel();
        var server = level.getServer();
        var player = MockPlayers.serverPlayerInLevel(h);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

        var ascaris = org.cneko.justarod.entity.JREntities.ASCARIS.create(
                level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        h.assertTrue(ascaris != null, "Ascaris entity created");
        ascaris.setPos(player.getX() + 1.5, player.getY(), player.getZ());
        ascaris.setTame(true, true);
        ascaris.tame(player);
        level.addFreshEntity(ascaris);

        BodyRegionData data = BodyRegionData.get(server);
        long slot = data.regionFor(ascaris.getBodyRegionOwnerUuid());
        AscarisAnatomyBuilder.build(level, slot);
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);

        // 1. 排泄横桥与肠绒毛完整度测量：破坏实体结构会真实降低生理效率！
        float fullExcEff = AscarisPhysiologyEngine.measureExcretoryBridgeEfficiency(level, slot);
        float fullVilliEff = AscarisPhysiologyEngine.measureLocalMicrovilliEfficiency(level, slot, 0);
        h.assertTrue(fullExcEff >= 0.99f && fullVilliEff >= 0.99f,
                "Intact H-shaped excretory bridge and intestinal microvilli report 100% efficiency");

        var brokenExcPos = new net.minecraft.core.BlockPos(cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X, AscarisAnatomyBuilder.FLOOR_Y, cz);
        level.setBlockAndUpdate(brokenExcPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.assertTrue(AscarisPhysiologyEngine.measureExcretoryBridgeEfficiency(level, slot) < fullExcEff,
                "Damaging excretory bridge reduces osmoregulation/detox efficiency");
        level.setBlockAndUpdate(brokenExcPos, org.cneko.justarod.block.JRBlocks.ASCARIS_EXCRETORY_MEMBRANE.defaultBlockState());

        // 2. 消化流水线：吞食触发咽部抽吸泵 + 生成食糜团 + 发出运动神经脉冲
        AscarisPhysiologyState st = data.physiologyFor(slot);
        st.boluses.clear();
        st.impulses.clear();
        AscarisPhysiologyEngine.onFoodSwallowed(level, slot, ascaris,
                org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_HONEY_GOLD);
        h.assertTrue(st.boluses.size() == 1 && st.boluses.getFirst().rx <= -35.0,
                "Swallowing food spawns moving chyme bolus inside pharynx");
        h.assertTrue(st.pharynxSuctionTicks > 0 && !st.impulses.isEmpty(),
                "Swallowing activates pharynx negative-pressure suction and motor nerve wave");

        // 3. 体壁破口漏压 + 体腔小细胞自主修补（且绝不覆盖玩家箱子！）
        var wallPos = new net.minecraft.core.BlockPos(cx + 2, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, cz + 4);
        var origState = level.getBlockState(wallPos);
        float pBefore = st.hydrostaticPressure;
        level.setBlockAndUpdate(wallPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        AscarisPhysiologyEngine.onTissueBreached(level, slot, wallPos, origState);
        h.assertTrue(st.breaches.size() == 1 && st.hydrostaticPressure < pBefore,
                "Tissue breach drops hydrostatic pressure and records repair target");

        var breach = st.breaches.getFirst();
        boolean repaired = false;
        for (int i = 0; i < 6 && !repaired; i++) {
            repaired = AscarisPhysiologyEngine.advanceCoelomocyteRepair(level, slot, breach, 20);
        }
        h.assertTrue(repaired && st.breaches.isEmpty()
                        && level.getBlockState(wallPos).is(org.cneko.justarod.block.JRBlocks.ASCARIS_INTESTINAL_MEMBRANE),
                "Coelomocyte repair autonomously restores breached tissue block");

        // 校验箱子保护：若破口处已被玩家放置箱子，修复会移除破口记录而绝不覆盖箱子！
        level.setBlockAndUpdate(wallPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        st.recordBreach(2, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, 4,
                AscarisPhysiologyState.ORGAN_GUT, "justarod:ascaris_intestinal_membrane");
        AscarisPhysiologyEngine.advanceCoelomocyteRepair(level, slot, st.breaches.getFirst(), 100);
        h.assertTrue(level.getBlockState(wallPos).is(net.minecraft.world.level.block.Blocks.CHEST) && st.breaches.isEmpty(),
                "Coelomocyte repair never overwrites player-placed chests");

        // 4. 双药硬核药理反差：哌嗪阻滞神经+松弛括约肌 vs 阿苯达唑断糖发热+排泄管抢救
        AscarisPhysiologyEngine.administerInternalDrug(level, slot, ascaris, AscarisPhysiologyState.DRUG_PIPERAZINE, 900);
        h.assertTrue(st.isParalyzedByPiperazine() && st.isSphincterRelaxed() && st.hydrostaticPressure <= 30.0f,
                "Piperazine causes flaccid paralysis, hydrostatic drop and sphincter relaxation");
        st.launchImpulse(0, -1, AscarisPhysiologyState.NerveImpulse.KIND_SENSORY, "");
        h.assertTrue(st.impulses.isEmpty(), "Piperazine blocks bio-electric nerve impulse propagation");

        AscarisPhysiologyEngine.administerInternalDrug(level, slot, ascaris, AscarisPhysiologyState.DRUG_ALBENDAZOLE, 400);
        h.assertTrue(st.isBlockedByAlbendazole() && st.hasFever()
                        && ascaris.getExpelledStage() == org.cneko.justarod.ascaris.AscarisEntity.EXPELLED_ALBENDAZOLE_CRITICAL,
                "Albendazole induces fever, glucose-uptake blockade and critical toxicity");
        float wasteBeforeFlush = st.metabolicWaste;
        st.glycogen = 60.0f;
        AscarisPhysiologyEngine.onExcretoryFlushed(level, slot, ascaris, player);
        h.assertTrue(st.metabolicWaste < wasteBeforeFlush && st.drugType == AscarisPhysiologyState.DRUG_NONE
                        && ascaris.getExpelledStage() == org.cneko.justarod.ascaris.AscarisEntity.EXPELLED_NONE,
                "Flushing H-shaped excretory bridge clears waste/Albendazole and rescues critical Ascaris-chan");

        // 5. 双管生殖流水线：按真实解剖从后段(x=+12)向阴门前段(x=-4)生长卵囊
        var postEgg = new net.minecraft.core.BlockPos(cx + 12, AscarisAnatomyBuilder.FLOOR_Y + 1, cz - 11);
        var antEgg = new net.minecraft.core.BlockPos(cx - 4, AscarisAnatomyBuilder.FLOOR_Y + 1, cz - 11);
        level.setBlockAndUpdate(postEgg, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(antEgg, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        float uBefore = st.uterineLoad;
        AscarisPhysiologyEngine.triggerOviductTransfer(level, slot, st);
        h.assertTrue(st.uterineLoad > uBefore
                        && level.getBlockState(postEgg).is(org.cneko.justarod.block.JRBlocks.ASCARIS_EGG_SAC)
                        && level.getBlockState(antEgg).isAir(),
                "Oviduct transfer fills paired uterus anatomically from posterior (x=+12) toward anterior (x=-4)");

        h.succeed();
    }
}
