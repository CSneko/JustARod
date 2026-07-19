package org.cneko.justarod;

import org.jetbrains.annotations.NotNull;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class JREnchantments {
    public static final ResourceLocation HYSTERECTOMY_ID = ResourceLocation.fromNamespaceAndPath(MODID,"hysterectomy");
    public static final ResourceKey<Enchantment> HYSTERECTOMY = of(HYSTERECTOMY_ID);
    public static final ResourceLocation UTERUS_INSTALLATION_ID = ResourceLocation.fromNamespaceAndPath(MODID,"uterus_installation");
    public static final ResourceKey<Enchantment> UTERUS_INSTALLATION = of(UTERUS_INSTALLATION_ID);
    public static final ResourceLocation ARTIFICIAL_ABORTION_ID = ResourceLocation.fromNamespaceAndPath(MODID,"artificial_abortion");
    public static final ResourceKey<Enchantment> ARTIFICIAL_ABORTION = of(ARTIFICIAL_ABORTION_ID);
    public static final ResourceLocation MASTECTOMY_ID = ResourceLocation.fromNamespaceAndPath(MODID,"mastectomy");
    public static final ResourceKey<Enchantment> MASTECTOMY = of(MASTECTOMY_ID);
    public static final ResourceLocation ORCHIECTOMY_ID = ResourceLocation.fromNamespaceAndPath(MODID,"orchiectomy");
    public static final ResourceKey<Enchantment> ORCHIECTOMY = of(ORCHIECTOMY_ID);
    public static final ResourceLocation AMPUTATING_ID = ResourceLocation.fromNamespaceAndPath(MODID,"amputating");
    public static final ResourceKey<Enchantment> AMPUTATING = of(AMPUTATING_ID);
    public static final ResourceLocation PRECISION_ID = ResourceLocation.fromNamespaceAndPath(MODID,"precision");
    public static final ResourceKey<Enchantment> PRECISION = of(PRECISION_ID);
    public static final ResourceLocation BEHEADING_ID = ResourceLocation.fromNamespaceAndPath(MODID,"beheading");
    public static final ResourceKey<Enchantment> BEHEADING = of(BEHEADING_ID);
    public static final ResourceLocation HEMORRHOIDECTOMY_ID = ResourceLocation.fromNamespaceAndPath(MODID,"hemorrhoidectomy");
    public static final ResourceKey<Enchantment> HEMORRHOIDECTOMY = of(HEMORRHOIDECTOMY_ID);
    public static final ResourceLocation MEIOSIS_ID = ResourceLocation.fromNamespaceAndPath(MODID,"meiosis");
    public static final ResourceKey<Enchantment> MEIOSIS = of(MEIOSIS_ID);
    public static final ResourceLocation HYMENOTOMY_ID = ResourceLocation.fromNamespaceAndPath(MODID,"hymenotomy");
    public static final ResourceKey<Enchantment> HYMENOTOMY = of(HYMENOTOMY_ID);
    public static final ResourceLocation LAPAROSCOPY_ID = ResourceLocation.fromNamespaceAndPath(MODID,"laparoscopy");
    public static final ResourceKey<Enchantment> LAPAROSCOPY = of(LAPAROSCOPY_ID);

    public static ResourceKey<Enchantment> of(ResourceLocation id) {
        return ResourceKey.create(Registries.ENCHANTMENT, id);
    }
}
