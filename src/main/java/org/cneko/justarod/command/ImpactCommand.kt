package org.cneko.justarod.command

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.entity.player.PlayerEntity

import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.cneko.justarod.api.ImpactModel

class ImpactCommand {
    companion object{
        fun init(){
            CommandRegistrationCallback.EVENT.register{ dispatcher, _, _ ->
                dispatcher.register(literal("impact")
                    .executes(ImpactCommand::execute)
                )
            }

        }
        private fun execute(context: CommandContext<ServerCommandSource>?): Int {
            val player = context!!.source.player as PlayerEntity
            // 如果开启了银趴，则关闭
            if (ImpactModel.isEnable(player)){
                ImpactModel.setEnable(player,false)
                context.source.sendFeedback({
                    Text.translatable("command.justarod.impact.disable")
                }, true)
            }else{
                ImpactModel.setEnable(player,true)
                context.source.sendFeedback({
                    Text.translatable("command.justarod.impact.enable")
                }, true)
            }
            return 1
        }
    }
}