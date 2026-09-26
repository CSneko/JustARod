package org.cneko.justarod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class JRKeyBindings {
    // 26.x：KeyMapping 分类改为 KeyMapping.Category（Identifier 注册）；fabric keybinding v1 更名为 keymapping v1
    public static final net.minecraft.client.KeyMapping.Category CATEGORY =
            net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(org.cneko.justarod.Justarod.MODID, "keybinds"));
    public static KeyMapping EXCREMENT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.justarod.excrement", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY));
    public static KeyMapping URINATE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.justarod.urinate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, CATEGORY));
    public static KeyMapping STATUS_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.justarod.status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, CATEGORY));
    /** 被史莱姆吞入体内时连点此键挣扎逃出（越大越难逃） */
    public static KeyMapping SLIME_STRUGGLE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.justarod.slime_struggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
    /**
     * 在史莱姆体内时打开它的背包。
     *
     * <p>为什么要单独一个键：钻进体内后玩家是乘客，右键全部走「操控载具」那条路，
     * {@code TamedSlimeEntity#mobInteract} 根本不会被调用，所以「体内开背包」只能靠按键 + 包。
     */
    public static KeyMapping SLIME_BAG_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.justarod.slime_bag", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
    /** 骑着史莱姆时按下的手动「吞噬」技能 */
    public static KeyMapping SLIME_DEVOUR_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.justarod.slime_devour", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));
    public static void init(){
    }
}
