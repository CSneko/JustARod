package org.cneko.justarod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class JRKeyBindings {
    public static KeyMapping EXCREMENT_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.justarod.excrement", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.justarod.category"));
    public static KeyMapping URINATE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.justarod.urinate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, "key.justarod.category"));
    public static KeyMapping STATUS_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.justarod.status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, "key.justarod.category"));
    public static void init(){
    }
}
