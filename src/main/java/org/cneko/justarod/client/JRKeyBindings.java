package org.cneko.justarod.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class JRKeyBindings {
    public static KeyBinding EXCREMENT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.justarod.excrement", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.justarod.category"));
    public static KeyBinding URINATE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.justarod.urinate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, "key.justarod.category"));
    public static KeyBinding STATUS_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.justarod.status", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, "key.justarod.category"));
    public static void init(){
    }
}
