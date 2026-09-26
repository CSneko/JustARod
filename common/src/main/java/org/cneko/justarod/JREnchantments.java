package org.cneko.justarod;

import org.jetbrains.annotations.NotNull;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public class JREnchantments {
    public static final Identifier HYSTERECTOMY_ID = Identifier.fromNamespaceAndPath(MODID,"hysterectomy");
    public static final ResourceKey<Enchantment> HYSTERECTOMY = of(HYSTERECTOMY_ID);
    public static final Identifier UTERUS_INSTALLATION_ID = Identifier.fromNamespaceAndPath(MODID,"uterus_installation");
    public static final ResourceKey<Enchantment> UTERUS_INSTALLATION = of(UTERUS_INSTALLATION_ID);
    public static final Identifier ARTIFICIAL_ABORTION_ID = Identifier.fromNamespaceAndPath(MODID,"artificial_abortion");
    public static final ResourceKey<Enchantment> ARTIFICIAL_ABORTION = of(ARTIFICIAL_ABORTION_ID);
    public static final Identifier MASTECTOMY_ID = Identifier.fromNamespaceAndPath(MODID,"mastectomy");
    public static final ResourceKey<Enchantment> MASTECTOMY = of(MASTECTOMY_ID);
    public static final Identifier ORCHIECTOMY_ID = Identifier.fromNamespaceAndPath(MODID,"orchiectomy");
    public static final ResourceKey<Enchantment> ORCHIECTOMY = of(ORCHIECTOMY_ID);
    public static final Identifier AMPUTATING_ID = Identifier.fromNamespaceAndPath(MODID,"amputating");
    public static final ResourceKey<Enchantment> AMPUTATING = of(AMPUTATING_ID);
    public static final Identifier PRECISION_ID = Identifier.fromNamespaceAndPath(MODID,"precision");
    public static final ResourceKey<Enchantment> PRECISION = of(PRECISION_ID);
    public static final Identifier BEHEADING_ID = Identifier.fromNamespaceAndPath(MODID,"beheading");
    public static final ResourceKey<Enchantment> BEHEADING = of(BEHEADING_ID);
    public static final Identifier HEMORRHOIDECTOMY_ID = Identifier.fromNamespaceAndPath(MODID,"hemorrhoidectomy");
    public static final ResourceKey<Enchantment> HEMORRHOIDECTOMY = of(HEMORRHOIDECTOMY_ID);
    public static final Identifier MEIOSIS_ID = Identifier.fromNamespaceAndPath(MODID,"meiosis");
    public static final ResourceKey<Enchantment> MEIOSIS = of(MEIOSIS_ID);
    public static final Identifier HYMENOTOMY_ID = Identifier.fromNamespaceAndPath(MODID,"hymenotomy");
    public static final ResourceKey<Enchantment> HYMENOTOMY = of(HYMENOTOMY_ID);
    public static final Identifier LAPAROSCOPY_ID = Identifier.fromNamespaceAndPath(MODID,"laparoscopy");
    public static final ResourceKey<Enchantment> LAPAROSCOPY = of(LAPAROSCOPY_ID);
    public static final Identifier CATARACT_SURGERY_ID = Identifier.fromNamespaceAndPath(MODID,"cataract_surgery");
    public static final ResourceKey<Enchantment> CATARACT_SURGERY = of(CATARACT_SURGERY_ID);

    public static ResourceKey<Enchantment> of(Identifier id) {
        return ResourceKey.create(Registries.ENCHANTMENT, id);
    }
}
