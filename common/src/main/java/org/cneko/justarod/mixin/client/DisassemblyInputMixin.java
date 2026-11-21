package org.cneko.justarod.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.*;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.cneko.justarod.client.disassembly.DisassemblyClient;
import org.cneko.justarod.disassembly.DisassemblyState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class DisassemblyInputMixin extends ClientInput {
    @Inject(method="tick",at=@At("TAIL"))
    private void justarod$bodyInput(CallbackInfo ci) {
        var p=Minecraft.getInstance().player;
        if (p==null) return;
        if (DisassemblyClient.viewing()) { moveVector=Vec2.ZERO; keyPresses=Input.EMPTY; }
        else if (DisassemblyState.of(p).crawling()) keyPresses=new Input(keyPresses.forward(),keyPresses.backward(),keyPresses.left(),keyPresses.right(),false,keyPresses.shift(),false);
    }
}
