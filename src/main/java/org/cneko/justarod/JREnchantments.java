package org.cneko.justarod;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public class JREnchantments {
    public static final Identifier HYSTERECTOMY_ID = Identifier.of(MODID,"hysterectomy");
    public static final RegistryKey<Enchantment> HYSTERECTOMY = of(HYSTERECTOMY_ID);
    public static final Identifier UTERUS_INSTALLATION_ID = Identifier.of(MODID,"uterus_installation");
    public static final RegistryKey<Enchantment> UTERUS_INSTALLATION = of(UTERUS_INSTALLATION_ID);
    public static final Identifier ARTIFICIAL_ABORTION_ID = Identifier.of(MODID,"artificial_abortion");
    public static final RegistryKey<Enchantment> ARTIFICIAL_ABORTION = of(ARTIFICIAL_ABORTION_ID);
    public static final Identifier MASTECTOMY_ID = Identifier.of(MODID,"mastectomy");
    public static final RegistryKey<Enchantment> MASTECTOMY = of(MASTECTOMY_ID);

    public static RegistryKey<Enchantment> of(Identifier id) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
    }
}
