package org.cneko.justarod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;
import org.cneko.justarod.ascaris.AscarisPoseState;
import org.cneko.justarod.client.gui.ScanType;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.packet.XRayScanScreenPayload;

import java.util.Comparator;
import java.util.List;

/**
 * /ascaris 调试与演出命令：方便直接测试感染阶段、双药驱虫、动态 B 超与蛔虫娘全部 v53 姿态动画。
 */
public final class AscarisCommand {

    private AscarisCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("ascaris");

        // /ascaris status [target]
        root.then(Commands.literal("status")
                .executes(ctx -> showStatus(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> showStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        // /ascaris infect <stage 1..4> [target]
        root.then(Commands.literal("infect")
                .then(Commands.argument("stage", IntegerArgumentType.integer(1, 4))
                        .executes(ctx -> infect(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "stage")))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> infect(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), IntegerArgumentType.getInteger(ctx, "stage"))))));

        // /ascaris cure [target]
        root.then(Commands.literal("cure")
                .executes(ctx -> cure(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> cure(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        // /ascaris expel [pagoda|albendazole]
        root.then(Commands.literal("expel")
                .executes(ctx -> expel(ctx.getSource(), ctx.getSource().getPlayerOrException(), AscarisInfectionController.DRUG_PAGODA))
                .then(Commands.literal("pagoda")
                        .executes(ctx -> expel(ctx.getSource(), ctx.getSource().getPlayerOrException(), AscarisInfectionController.DRUG_PAGODA)))
                .then(Commands.literal("albendazole")
                        .executes(ctx -> expel(ctx.getSource(), ctx.getSource().getPlayerOrException(), AscarisInfectionController.DRUG_ALBENDAZOLE))));

        // /ascaris scan [target]
        root.then(Commands.literal("scan")
                .executes(ctx -> openScan(ctx.getSource().getPlayerOrException(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> openScan(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "target")))));

        // /ascaris spawn [tamed|wild|expelled]
        root.then(Commands.literal("spawn")
                .executes(ctx -> spawnAscaris(ctx.getSource().getPlayerOrException(), "tamed"))
                .then(Commands.literal("tamed").executes(ctx -> spawnAscaris(ctx.getSource().getPlayerOrException(), "tamed")))
                .then(Commands.literal("wild").executes(ctx -> spawnAscaris(ctx.getSource().getPlayerOrException(), "wild")))
                .then(Commands.literal("expelled").executes(ctx -> spawnAscaris(ctx.getSource().getPlayerOrException(), "expelled"))));

        // /ascaris pose <name>
        root.then(Commands.literal("pose")
                .then(Commands.argument("pose", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String s : List.of(
                                    "normal", "idle", "crawl", "sit", "sit_chair", "boat", "ride",
                                    "swim", "swim_stand", "jump", "sneak", "sneaking",
                                    "ladder_up", "ladder_stillness", "ladder_down", "hurt", "fly", "elytra",
                                    "prone", "curl", "tail_wrap", "collapse", "sleep", "wave", "happy",
                                    "look_around", "rest", "yandere_stare", "yandere_wrap",
                                    "yandere_wrap_idle", "yandere_tighten", "stalk", "threaten", "bed_watch")) {
                                builder.suggest(s);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> setNearestPose(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "pose")))));

        // /ascaris hunger <0..100>
        root.then(Commands.literal("hunger")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setNearestHunger(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value")))));

        // /ascaris obsession <0..100>
        root.then(Commands.literal("obsession")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setNearestObsession(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value")))));

        // /ascaris eyes <auto|default|yandere|heart>
        root.then(Commands.literal("eyes")
                .then(Commands.argument("variant", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String s : List.of("auto", "default", "yandere", "heart")) builder.suggest(s);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> setNearestEyes(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "variant")))));

        // /ascaris yandere_wrap
        root.then(Commands.literal("yandere_wrap")
                .executes(ctx -> triggerYandereWrap(ctx.getSource().getPlayerOrException())));

        // /ascaris host (收回最近已驯服蛔虫娘至体内，或从体内唤出)
        root.then(Commands.literal("host")
                .executes(ctx -> toggleHost(ctx.getSource().getPlayerOrException())));

        // /ascaris chest (缩小自身并安放到最近已驯服蛔虫娘的胸口上)
        root.then(Commands.literal("chest")
                .executes(ctx -> mountChest(ctx.getSource().getPlayerOrException())));

        // /ascaris enter [mouth|vagina|anus|anywhere] (反向寄生：进入最近粉白蛔虫娘的体内维度区域)
        root.then(Commands.literal("enter")
                .executes(ctx -> enterNearestAscaris(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.BodyWorld.MOUTH))
                .then(Commands.literal("mouth").executes(ctx -> enterNearestAscaris(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.BodyWorld.MOUTH)))
                .then(Commands.literal("vagina").executes(ctx -> enterNearestAscaris(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.BodyWorld.VAGINA)))
                .then(Commands.literal("anus").executes(ctx -> enterNearestAscaris(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.BodyWorld.ANUS)))
                .then(Commands.literal("anywhere").executes(ctx -> enterNearestAscaris(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.BodyWorld.ANYWHERE))));

        // /ascaris molt (立即触发最近粉白蛔虫娘的活体蜕皮成长)
        root.then(Commands.literal("molt")
                .executes(ctx -> triggerNearestMolt(ctx.getSource().getPlayerOrException())));

        // /ascaris resistance <0..100> (设置最近粉白蛔虫娘的哌嗪/宝塔糖耐药性)
        root.then(Commands.literal("resistance")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setNearestResistance(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "value")))));

        // /ascaris physiology <status|drug|bolus|ovulate|waste|glycogen> (调试与体验硬核体内生理循环)
        var phys = Commands.literal("physiology");
        phys.then(Commands.literal("status")
                .executes(ctx -> showPhysiologyStatus(ctx.getSource().getPlayerOrException())));
        phys.then(Commands.literal("drug")
                .then(Commands.literal("none").executes(ctx -> setPhysiologyDrug(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.AscarisPhysiologyState.DRUG_NONE)))
                .then(Commands.literal("piperazine").executes(ctx -> setPhysiologyDrug(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.AscarisPhysiologyState.DRUG_PIPERAZINE)))
                .then(Commands.literal("albendazole").executes(ctx -> setPhysiologyDrug(ctx.getSource().getPlayerOrException(), org.cneko.justarod.body.AscarisPhysiologyState.DRUG_ALBENDAZOLE))));
        phys.then(Commands.literal("bolus")
                .executes(ctx -> spawnPhysiologyBolus(ctx.getSource().getPlayerOrException(), org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_SWEET))
                .then(Commands.literal("sweet").executes(ctx -> spawnPhysiologyBolus(ctx.getSource().getPlayerOrException(), org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_SWEET)))
                .then(Commands.literal("glow").executes(ctx -> spawnPhysiologyBolus(ctx.getSource().getPlayerOrException(), org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_GLOW)))
                .then(Commands.literal("honey").executes(ctx -> spawnPhysiologyBolus(ctx.getSource().getPlayerOrException(), org.cneko.justarod.block.ascaris.AscarisChymeBlock.FLAVOR_HONEY_GOLD))));
        phys.then(Commands.literal("ovulate")
                .executes(ctx -> triggerPhysiologyOvulation(ctx.getSource().getPlayerOrException())));
        phys.then(Commands.literal("waste")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setPhysiologyVital(ctx.getSource().getPlayerOrException(), "waste", IntegerArgumentType.getInteger(ctx, "value")))));
        phys.then(Commands.literal("glycogen")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setPhysiologyVital(ctx.getSource().getPlayerOrException(), "glycogen", IntegerArgumentType.getInteger(ctx, "value")))));
        root.then(phys);

        dispatcher.register(root);
    }

    private static int showStatus(CommandSourceStack src, ServerPlayer target) {
        int ticks = ((Pregnant) (Object) target).getAscaris();
        int stage = AscarisInfectionController.stageOf(ticks);
        src.sendSuccess(() -> Component.literal("§d[蛔虫状态] §f" + target.getName().getString()
                + " -> §e" + AscarisInfectionController.stageLabel(stage) + " §7(" + ticks + " tick)"), false);
        return 1;
    }

    private static int infect(CommandSourceStack src, ServerPlayer target, int stage) {
        ((Pregnant) (Object) target).setAscaris(AscarisInfectionController.minTicksForStage(stage));
        AscarisInfectionController.infectPlayer(target, stage, "§d[测试] 已设为 " + AscarisInfectionController.stageLabel(stage));
        src.sendSuccess(() -> Component.literal("§a已将 " + target.getName().getString() + " 设为 " + AscarisInfectionController.stageLabel(stage)), false);
        return 1;
    }

    private static int cure(CommandSourceStack src, ServerPlayer target) {
        AscarisInfectionController.cureCleanly(target);
        src.sendSuccess(() -> Component.literal("§a已彻底清除 " + target.getName().getString() + " 的蛔虫感染。"), false);
        return 1;
    }

    private static int expel(CommandSourceStack src, ServerPlayer target, int drugType) {
        Pregnant p = (Pregnant) (Object) target;
        if (AscarisInfectionController.stageOf(p.getAscaris()) < 3) {
            p.setAscaris(AscarisInfectionController.STAGE_3_TICKS);
        }
        AscarisInfectionController.administerDewormer(target, drugType, target);
        return 1;
    }

    private static int openScan(ServerPlayer viewer, ServerPlayer target) {
        ServerPlayNetworking.send(viewer, new XRayScanScreenPayload(ScanType.ABDOMEN, target.getId()));
        return 1;
    }

    private static int spawnAscaris(ServerPlayer player, String mode) {
        ServerLevel level = (ServerLevel) player.level();
        AscarisEntity entity = JREntities.ASCARIS.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return 0;
        Vec3 fwd = Vec3.directionFromRotation(0, player.getYRot()).scale(1.8);
        entity.setPos(player.getX() + fwd.x, player.getY(), player.getZ() + fwd.z);
        entity.setYRot(player.getYRot() + 180f);
        entity.setYBodyRot(player.getYRot() + 180f);
        if ("tamed".equals(mode)) {
            entity.setTame(true, true);
            entity.tame(player);
            entity.playTransientPose(AscarisPoseState.WAVE, 56);
        } else if ("expelled".equals(mode)) {
            entity.configureAsFreshlyExpelled(false);
        }
        level.addFreshEntity(entity);
        player.sendSystemMessage(Component.literal("§d已生成粉白蛔虫娘 (" + mode + ")。"));
        return 1;
    }

    private static int setNearestPose(ServerPlayer player, String poseName) {
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        boolean crawl = "crawl".equalsIgnoreCase(poseName);
        AscarisPoseState state = AscarisPoseState.byName(poseName);
        if (state == null) {
            player.sendSystemMessage(Component.literal("§c未知姿态: " + poseName));
            return 0;
        }
        nearest.forceShowcasePose(state, crawl);
        player.sendSystemMessage(Component.literal("§d已切换姿态为: §e" + poseName));
        return 1;
    }

    private static int setNearestHunger(ServerPlayer player, int value) {
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        nearest.setHunger(value);
        player.sendSystemMessage(Component.literal("§d已将粉白蛔虫娘饱食度设为: §e" + value + "/100"));
        return 1;
    }

    private static int setNearestObsession(ServerPlayer player, int value) {
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        nearest.setObsession(value);
        player.sendSystemMessage(Component.literal("§c已将粉白蛔虫娘执念值设为: §d" + value + "/100"));
        return 1;
    }

    private static int setNearestEyes(ServerPlayer player, String variant) {
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        int v = switch (variant.toLowerCase(java.util.Locale.ROOT)) {
            case "default" -> AscarisEntity.EYE_DEFAULT;
            case "yandere" -> AscarisEntity.EYE_YANDERE;
            case "heart" -> AscarisEntity.EYE_YANDERE_HEART;
            default -> AscarisEntity.EYE_AUTO;
        };
        nearest.setEyeOverride(v);
        player.sendSystemMessage(Component.literal("§d已切换眼部贴图为: §e" + variant));
        return 1;
    }

    private static int triggerYandereWrap(ServerPlayer player) {
        if (!org.cneko.justarod.config.JRConfig.isAscarisYandereEnabled()) {
            player.sendSystemMessage(Component.literal("§e配置中已关闭粉白蛔虫娘病娇选项（ascaris.yandere = false）。"));
            return 0;
        }
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        nearest.setObsession(Math.max(nearest.getObsession(), 85));
        nearest.startTailWrap(player, AscarisEntity.WRAP_YANDERE);
        return 1;
    }

    private static int toggleHost(ServerPlayer player) {
        if (org.cneko.justarod.ascaris.AscarisHostController.hasHostedAscaris(player)) {
            org.cneko.justarod.ascaris.AscarisHostController.summonFromHostBody(player, false, false);
            return 1;
        }
        AscarisEntity nearest = findNearest(player);
        if (nearest == null || !nearest.isTame() || !nearest.isOwnedBy(player)) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有已驯服的粉白蛔虫娘。"));
            return 0;
        }
        org.cneko.justarod.ascaris.AscarisHostController.enterHostBody(nearest, player, "manual");
        return 1;
    }

    private static int mountChest(ServerPlayer player) {
        if (!org.cneko.justarod.scale.CuteScaleController.isSmallEnoughForChest(player)) {
            org.cneko.justarod.scale.CuteScaleController.setCuteScaleImmediate(player, 0.38);
        }
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            spawnAscaris(player, "tamed");
            nearest = findNearest(player);
        }
        if (nearest == null) return 0;
        if (!nearest.isTame()) {
            nearest.setTame(true, true);
            nearest.tame(player);
        }
        nearest.mountShrunkPlayerOnChest(player);
        return 1;
    }

    private static int enterNearestAscaris(ServerPlayer player, int route) {
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        return org.cneko.justarod.body.BodyWorld.enterAscaris(player, nearest, route, true) ? 1 : 0;
    }

    private static Long resolvePhysiologySlot(ServerPlayer player) {
        var server = ((ServerLevel) player.level()).getServer();
        var data = org.cneko.justarod.body.BodyRegionData.get(server);
        var visit = data.visit(player.getUUID());
        if (visit != null && visit.isAscaris()) {
            return visit.slot();
        }
        AscarisEntity nearest = findNearest(player);
        if (nearest != null) {
            return data.regionFor(nearest.getBodyRegionOwnerUuid());
        }
        return null;
    }

    private static int showPhysiologyStatus(ServerPlayer player) {
        Long slot = resolvePhysiologySlot(player);
        if (slot == null) {
            player.sendSystemMessage(Component.literal("§c你既不在粉白蛔虫娘体内，附近 16 格内也没有粉白蛔虫娘。"));
            return 0;
        }
        var server = ((ServerLevel) player.level()).getServer();
        var st = org.cneko.justarod.body.BodyRegionData.get(server).physiologyFor(slot);
        String drugLabel = switch (st.drugType) {
            case org.cneko.justarod.body.AscarisPhysiologyState.DRUG_PIPERAZINE -> "§b哌嗪麻痹(" + (st.drugTicks / 20) + "s)";
            case org.cneko.justarod.body.AscarisPhysiologyState.DRUG_ALBENDAZOLE -> "§c阿苯达唑断糖(" + (st.drugTicks / 20) + "s)";
            default -> "§a无";
        };
        player.sendSystemMessage(Component.literal(
                "§d[体内硬核生理监护] §f静水压 §d" + st.pressureKpa() + "kPa(" + Math.round(st.hydrostaticPressure) + "%)"
                        + " §7| §f糖原 §e" + Math.round(st.glycogen) + "%"
                        + " §7| §f代谢毒 §b" + Math.round(st.metabolicWaste) + "%"
                        + " §7| §f体温 §6" + String.format("%.1f°C", st.temperatureC)
                        + "\n§7消化: 食糜团 §f" + st.boluses.size() + " §7| 直肠 §6" + Math.round(st.rectalLoad) + "%"
                        + " §7| 生殖: 卵巢 §f" + Math.round(st.ovaryProgress) + "%§7→子宫 §d" + Math.round(st.uterineLoad) + "%"
                        + " §7| 体壁 §a" + st.wallIntegrityPct() + "%§7(破口" + st.breaches.size() + ") §7| 药理: " + drugLabel));
        return 1;
    }

    private static int setPhysiologyDrug(ServerPlayer player, int drugType) {
        Long slot = resolvePhysiologySlot(player);
        if (slot == null) {
            player.sendSystemMessage(Component.literal("§c未找到目标粉白蛔虫娘体内槽位。"));
            return 0;
        }
        var server = ((ServerLevel) player.level()).getServer();
        ServerLevel body = server.getLevel(org.cneko.justarod.body.BodyWorld.DIMENSION);
        if (body == null) return 0;
        var data = org.cneko.justarod.body.BodyRegionData.get(server);
        var visit = data.visit(player.getUUID());
        AscarisEntity host = (visit != null && visit.isAscaris())
                ? org.cneko.justarod.body.BodyWorld.findAscarisHost(server, visit)
                : findNearest(player);
        int dur = switch (drugType) {
            case org.cneko.justarod.body.AscarisPhysiologyState.DRUG_PIPERAZINE -> 900;
            case org.cneko.justarod.body.AscarisPhysiologyState.DRUG_ALBENDAZOLE -> 1200;
            default -> 0;
        };
        org.cneko.justarod.body.AscarisPhysiologyEngine.administerInternalDrug(body, slot, host, drugType, dur);
        if (player.level().dimension().equals(org.cneko.justarod.body.BodyWorld.DIMENSION)) {
            org.cneko.justarod.body.AscarisPhysiologyEngine.sendPhysiologySync(player, data.physiologyFor(slot));
        }
        player.sendSystemMessage(Component.literal("§d已设置体内药理状态为: §e" + (drugType == 1 ? "哌嗪(宝塔糖)弛缓麻痹" : (drugType == 2 ? "阿苯达唑微管断糖" : "清除药物"))));
        return 1;
    }

    private static int spawnPhysiologyBolus(ServerPlayer player, int flavor) {
        Long slot = resolvePhysiologySlot(player);
        if (slot == null) {
            player.sendSystemMessage(Component.literal("§c未找到目标粉白蛔虫娘体内槽位。"));
            return 0;
        }
        var server = ((ServerLevel) player.level()).getServer();
        ServerLevel body = server.getLevel(org.cneko.justarod.body.BodyWorld.DIMENSION);
        if (body == null) return 0;
        var visit = org.cneko.justarod.body.BodyRegionData.get(server).visit(player.getUUID());
        AscarisEntity host = (visit != null && visit.isAscaris())
                ? org.cneko.justarod.body.BodyWorld.findAscarisHost(server, visit)
                : findNearest(player);
        org.cneko.justarod.body.AscarisPhysiologyEngine.onFoodSwallowed(body, slot, host, flavor);
        player.sendSystemMessage(Component.literal("§d已触发咽部负压抽吸并投送「"
                + org.cneko.justarod.block.ascaris.AscarisChymeBlock.flavorLabel(flavor) + "」移动食糜团！"));
        return 1;
    }

    private static int triggerPhysiologyOvulation(ServerPlayer player) {
        Long slot = resolvePhysiologySlot(player);
        if (slot == null) {
            player.sendSystemMessage(Component.literal("§c未找到目标粉白蛔虫娘体内槽位。"));
            return 0;
        }
        var server = ((ServerLevel) player.level()).getServer();
        ServerLevel body = server.getLevel(org.cneko.justarod.body.BodyWorld.DIMENSION);
        if (body == null) return 0;
        var data = org.cneko.justarod.body.BodyRegionData.get(server);
        var st = data.physiologyFor(slot);
        st.ovaryProgress = 100f;
        org.cneko.justarod.body.AscarisPhysiologyEngine.triggerOviductTransfer(body, slot, st);
        st.uterineLoad = Math.max(st.uterineLoad, 90f);
        st.ovipositionCooldown = 0;
        data.markPhysiologyDirty();
        player.sendSystemMessage(Component.literal("§d已触发排卵输送与双子宫充盈，即将启动阴道宫缩产卵波！"));
        return 1;
    }

    private static int setPhysiologyVital(ServerPlayer player, String key, int value) {
        Long slot = resolvePhysiologySlot(player);
        if (slot == null) {
            player.sendSystemMessage(Component.literal("§c未找到目标粉白蛔虫娘体内槽位。"));
            return 0;
        }
        var server = ((ServerLevel) player.level()).getServer();
        var data = org.cneko.justarod.body.BodyRegionData.get(server);
        var st = data.physiologyFor(slot);
        if ("waste".equals(key)) {
            st.metabolicWaste = value;
        } else {
            st.glycogen = value;
        }
        st.clampAll();
        data.markPhysiologyDirty();
        if (player.level().dimension().equals(org.cneko.justarod.body.BodyWorld.DIMENSION)) {
            org.cneko.justarod.body.AscarisPhysiologyEngine.sendPhysiologySync(player, st);
        }
        player.sendSystemMessage(Component.literal("§d已将体内 " + key + " 设为 §e" + value + "%"));
        return 1;
    }

    private static int triggerNearestMolt(ServerPlayer player) {
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        nearest.performEcdysis((ServerLevel) player.level());
        return 1;
    }

    private static int setNearestResistance(ServerPlayer player, int value) {
        AscarisEntity nearest = findNearest(player);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§c附近 16 格内没有粉白蛔虫娘。"));
            return 0;
        }
        nearest.setPiperazineResistance(value);
        player.sendSystemMessage(Component.literal("§d已将粉白蛔虫娘的哌嗪(宝塔糖)耐药性设为: §e" + value + "%"));
        return 1;
    }

    private static AscarisEntity findNearest(ServerPlayer player) {
        List<AscarisEntity> list = player.level().getEntitiesOfClass(AscarisEntity.class, player.getBoundingBox().inflate(16.0));
        return list.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }
}
