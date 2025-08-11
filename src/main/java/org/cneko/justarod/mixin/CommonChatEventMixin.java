package org.cneko.justarod.mixin;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.events.CommonChatEvent;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static org.cneko.toneko.common.mod.events.CommonChatEvent.sendMessage;

@Mixin(CommonChatEvent.class)
public class CommonChatEventMixin {
    @Unique
    private static final List<String> MESSAGES = List.of(
            "嗯…呜…♡",
            "呜…咿…",
            "唔…嗯嗯…",
            "嗯嗯~♡",
            "咿呀…唔…",
            "呜嗯…哈…",
            "唔…咕…",
            "嗯唔…",
            "呜…唔嗯…",
            "唔…啾…"
    );
    @Inject(method = "onChatMessage", at = @At("HEAD"),cancellable = true)
    private static void onChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        if (sender.getBallMouth()>0){
            ci.cancel();
            String msg = MESSAGES.get(sender.getRandom().nextInt(MESSAGES.size()));
            msg = Messaging.nekoModify(msg, (INeko) sender);
            msg = Messaging.format(msg, (INeko) sender, Messaging.getChatPrefixes((INeko) sender), ConfigUtil.getChatFormat());

            sendMessage(Text.of(msg));
        }
    }
}
