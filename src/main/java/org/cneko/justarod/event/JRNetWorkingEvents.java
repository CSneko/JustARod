package org.cneko.justarod.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.cneko.justarod.client.screen.MateScreen;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.mixin.NekoEntityMixin;
import org.cneko.justarod.packet.FullHeatPayload;
import org.cneko.justarod.packet.MatePayload;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.events.ToNekoNetworkEvents;
import org.cneko.toneko.common.mod.util.EntityUtil;

import java.util.UUID;

public class JRNetWorkingEvents {
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(FullHeatPayload.ID, (payload,context) -> {
            // 消耗体力
            PlayerEntity player = context.player();
            player.setPower(player.getPower()-80);
        });
        ServerPlayNetworking.registerGlobalReceiver(MatePayload.ID,((payload, context) -> {
            ServerPlayerEntity player = context.player();
            // 计算概率（与量和时间成正比）
            double probability = payload.amount() * payload.time() / 150;
            if (probability >= 1 || Math.random() < probability) {
                // 添加状态效果
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, payload.time()*20));
                // 减少体力
                player.setPower(player.getPower()-payload.amount()*30);
                String uuid = payload.nekoUuid();
                // 如果uuid合法
                try {
                    UUID nekoUuid = UUID.fromString(uuid);
                    NekoEntity neko = ToNekoNetworkEvents.findNearbyNekoByUuid(player, nekoUuid,32);
                    if (neko != null){
                        neko.tryMating((ServerWorld) player.getWorld(),neko);
                    }
                }catch (Exception ignored){}
            }else {
                player.setPower(player.getPower()-payload.amount()*5);
                player.sendMessage(Text.of("§c配种失败！"));
            }
        }));
    }
}
