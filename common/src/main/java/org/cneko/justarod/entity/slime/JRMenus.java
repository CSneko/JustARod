package org.cneko.justarod.entity.slime;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import org.cneko.justarod.Justarod;

/**
 * 菜单类型注册。
 *
 * <p>26.x 的 {@code MenuType} 构造器是私有的，且 common 侧不能依赖平台 API，
 * 因此通过 access widener 打开构造器后直接注册。
 */
public class JRMenus {

    public static final MenuType<SlimeMenu> SLIME_MENU = new MenuType<>(
            (containerId, inventory) -> new SlimeMenu(containerId, inventory),
            FeatureFlags.VANILLA_SET);

    public static void init() {
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_menu"),
                SLIME_MENU);
    }
}
