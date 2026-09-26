package org.cneko.justarod.mixin.debug;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerSynchronizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取出 {@code ServerPlayer} 自带的容器同步器。
 *
 * <p>原版箱子菜单是把「玩家自带的 synchronizer」交给菜单用的，本模组的菜单挂在实体上，
 * 拿不到那个引用就会导致 {@code broadcastChanges()} 空转、客户端永远收不到容器内容。
 * 字段声明在 {@code ServerPlayer} 自身，因此这里对 ServerPlayer 用 {@code @Accessor} 是安全的
 * （与之前 {@code AbstractContainerMenu#slots} 的情况不同——那个会踩到映射问题）。
 */
@Mixin(ServerPlayer.class)
public interface ServerPlayerSyncAccessor {

    @Accessor("containerSynchronizer")
    ContainerSynchronizer justarod$getContainerSynchronizer();
}
