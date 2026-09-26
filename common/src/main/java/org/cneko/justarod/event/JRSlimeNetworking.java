package org.cneko.justarod.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import org.cneko.justarod.entity.slime.SlimeMenu;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.cneko.justarod.packet.SlimeContainerActionPayload;
import org.cneko.justarod.packet.SlimeEntityListPayload;
import org.cneko.justarod.packet.SlimeMenuContextPayload;

import java.util.UUID;

/**
 * 史莱姆相关的服务端网络处理与容器打开逻辑。
 *
 * <p>放在 common 里是因为 NeoForge 侧由 Forgified Fabric API 提供同一套
 * {@code ServerPlayNetworking}，无需平台分支。
 */
public final class JRSlimeNetworking {

    private JRSlimeNetworking() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(SlimeContainerActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handle(context.player(), payload)));
    }

    /**
     * 打开体内容器 GUI。
     *
     * <p>两件事必须按顺序做好：
     * <ol>
     *   <li>先把「待打开上下文」交给客户端（{@link SlimeMenuContextPayload}）：
     *       客户端构造菜单时拿不到实体，只能靠它算容量/槽位数，
     *       否则 {@code ClientboundContainerSetSlotPacket} 会越界掉线；
     *       同时界面工厂用它拿到实体，从而**直接构造出自定义界面**——
     *       早期版本是「先造原版界面、下一 tick 再替换」，而替换会调用原版界面的
     *       {@code removed()} → {@code onClose()}，把还在同步中的菜单关掉，
     *       导致容器内容永远同步不到客户端（GUI 里看不到物品）。</li>
     *   <li>再 openMenu。</li>
     * </ol>
     */
    public static void open(ServerPlayer player, TamedSlimeEntity slime) {
        ServerPlayNetworking.send(player, new SlimeMenuContextPayload(
                -1, slime.getId(), slime.getTierLevel()));
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new SlimeMenu(id, inventory, slime),
                Component.translatable("justarod.slime.gui.title")));
        // openMenu 内部会 addSlotListener（从而装上同步器），之后必须主动推一次初始内容：
        // 没有方块实体帮我们做这件事，漏了的话客户端 slots 永远是空的。
        if (player.containerMenu instanceof SlimeMenu menu) {
            menu.sendInitialSync();
        }
    }

    /** 把体内实体列表推给客户端 */
    public static void sendEntityList(ServerPlayer player, TamedSlimeEntity slime, int containerId) {
        var storage = slime.storage();
        var entries = new java.util.ArrayList<SlimeEntityListPayload.Entry>();
        for (int i = 0; i < storage.containedEntities().size(); i++) {
            UUID uuid = storage.containedEntities().get(i);
            Entity entity = slime.findContainedEntity(uuid);
            if (entity == null) continue;
            int typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getId(entity.getType());
            int struggle = (int) Math.min(1000.0F, storage.struggleProgress().get(i));
            entries.add(new SlimeEntityListPayload.Entry(uuid, entity.getName(), typeId,
                    i < storage.entityVolumes().size() ? storage.entityVolumes().get(i) : 1, struggle,
                    i < storage.struggleImmune().size() && storage.struggleImmune().get(i)));
        }
        ServerPlayNetworking.send(player, new SlimeEntityListPayload(
                containerId,
                slime.getTierLevel(),
                storage.usedSpace(),
                slime.tier().internalSpace(),
                (int) slime.struggleThreshold(),
                entries));
    }

    private static void handle(ServerPlayer player, SlimeContainerActionPayload payload) {
        // 挣扎：不要求 GUI 打开（玩家在体内时通常没有界面）
        if (payload.action() == SlimeContainerActionPayload.ACTION_STRUGGLE) {
            if (player.getVehicle() instanceof TamedSlimeEntity vehicle) {
                vehicle.applyStruggle(player, 4.0F);
                SlimeMenu menu = player.containerMenu instanceof SlimeMenu m ? m : null;
                if (menu != null && menu.slime() == vehicle) {
                    sendEntityList(player, vehicle, menu.containerId);
                }
            }
            return;
        }

        // 「体内开背包」与「手动吞噬」都以「载具是自己的史莱姆」为前提，
        // 因此放在下面「必须已经开着我们的菜单」那道检查之前——
        // 玩家在体内时通常并没有界面（按背包键的那一刻也正是要打开它）。
        if (payload.action() == SlimeContainerActionPayload.ACTION_OPEN_BAG) {
            if (player.getVehicle() instanceof TamedSlimeEntity vehicle && vehicle.isOwner(player)) {
                open(player, vehicle);
            }
            return;
        }
        if (payload.action() == SlimeContainerActionPayload.ACTION_DEVOUR) {
            if (player.getVehicle() instanceof TamedSlimeEntity vehicle) {
                vehicle.triggerDevour(player);
            }
            return;
        }

        SlimeMenu menu = player.containerMenu instanceof SlimeMenu slimeMenu ? slimeMenu : null;
        TamedSlimeEntity slime = menu == null ? null : menu.slime();
        if (menu == null || slime == null || !slime.isAlive()) return;
        if (menu.containerId != payload.containerId()) return;

        switch (payload.action()) {
            case SlimeContainerActionPayload.ACTION_REFRESH -> sendEntityList(player, slime, menu.containerId);
            case SlimeContainerActionPayload.ACTION_SET_PAGE -> {
                if (!slime.isOwner(player)) return;
                menu.applyPage(payload.value());
                sendEntityList(player, slime, menu.containerId);
            }
            case SlimeContainerActionPayload.ACTION_SET_TAB -> menu.setTab(payload.value());
            case SlimeContainerActionPayload.ACTION_RELEASE_ENTITY -> {
                if (!slime.isOwner(player)) return;
                // 谁都能被放出来，包括主人自己（他也可以被分解，当然也可以主动下车）
                slime.releaseEntity(payload.uuid());
                sendEntityList(player, slime, menu.containerId);
            }
            case SlimeContainerActionPayload.ACTION_FEED_CATALYST -> {
                // 界面上的「喂催化物」按钮（按钮本身尚未接上）：规则与右键完全一致——
                // 手里必须是催化物，批内拒绝，成功才消耗一份
                if (slime.isOwner(player)) {
                    slime.useCatalyst(player, player.getMainHandItem());
                }
            }
            case SlimeContainerActionPayload.ACTION_SET_COMMAND -> {
                if (!slime.isOwner(player)) return;
                if (payload.value() == Integer.MIN_VALUE) {
                    // 循环：跟随 → 待命 → 吞人
                    slime.setCommand((slime.getCommand() + 1) % 3);
                } else {
                    slime.setCommand(payload.value());
                }
                String key = switch (slime.getCommand()) {
                    case 1 -> "justarod.slime.command.stay";
                    case 2 -> "justarod.slime.command.swallow";
                    default -> "justarod.slime.command.follow";
                };
                player.sendOverlayMessage(Component.translatable("justarod.slime.command.changed",
                        Component.translatable(key)));
            }
            default -> {
            }
        }
    }
}
