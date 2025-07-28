package org.cneko.justarod;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public class JREnchantments {
    public static final Identifier HYSTERECTOMY_ID = Identifier.of(MODID,"hysterectomy");
    public static final RegistryKey<Enchantment> HYSTERECTOMY = of(HYSTERECTOMY_ID);

    public static RegistryKey<Enchantment> of(Identifier id) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
    }
}
