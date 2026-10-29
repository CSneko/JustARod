package org.cneko.justarod.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * 26.1.2 起 {@code GameTestHelper#makeMockServerPlayerInLevel()} 被标记为
 * {@code @Deprecated(forRemoval = true)}，而原版没有给出返回 {@link ServerPlayer} 的替代品：
 * {@code makeMockPlayer(GameType)} 返回的是普通 {@code Player}，没有连接、不在玩家列表里，
 * 测试中 {@code player.connection}/{@code setGameMode} 这类调用会直接失效。
 * <p>
 * 因此这里按原版实现重建同样的“已连接、已进入世界的模拟服务端玩家”：
 * 构造 {@link ServerPlayer}（游戏模式 CREATIVE）、建一条 serverbound 连接的
 * {@link EmbeddedChannel}，再通过 {@code PlayerList#placeNewPlayer} 真正放进世界。
 */
public final class MockPlayers {
    private MockPlayers() {
    }

    /** 等价于已弃用的 {@code h.makeMockServerPlayerInLevel()}。 */
    public static ServerPlayer serverPlayerInLevel(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "test-mock-player");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);

        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                cookie.gameProfile(),
                cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }
        };

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        // 通道由 Connection 持有，placeNewPlayer 之后正常收发数据包。
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
}
