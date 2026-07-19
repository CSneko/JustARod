package org.cneko.justarod.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.component.DataComponentTypes
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.cneko.justarod.Justarod.MODID

import org.cneko.justarod.block.JRBlocks.*
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.rod.*
import org.cneko.justarod.item.armor.*
import org.cneko.justarod.item.bdsm.*
import org.cneko.justarod.item.bio.ClonerDevice
import org.cneko.justarod.item.bio.ParthenogenesisCatalystItem
import org.cneko.justarod.item.custom.DiaperItem
import org.cneko.justarod.item.custom.PantsuItem
import org.cneko.justarod.item.custom.PantsuGetterItem
import org.cneko.justarod.item.electric.*
import org.cneko.justarod.item.medical.*
import org.cneko.justarod.item.syringe.*

/*
仙之人兮列如喵
 */
class JRItems {
    companion object{
        val SLIME_ROD = SlimeRodItem()
        val GIANT_ROD = GiantRodItem()
        val LUBRICATING_BOOK = LubricatingBookItem()
        val REDSTONE_ROD = RedstoneEndRod()
        val CACTUS_ROD = CactusRodItem()
        val EATABLE_ROD = EatableRodItem()
        val LONG_ROD = LongRodItem()
        val LONGER_ROD = LongerRodItem()
        val LIGHTNING_END_ROD = LightningEndRodItem()
        val NETWORKING_ROD: NetWorkingRodItem = NetWorkingRodItem()
        val BASIC_ELECTRIC_ROD = BasicElectricRodItem()
        val BREMELANOTIDE = BremelanotideItem()
        val GROWTH_AGENT = GrowthAgentItem()
        val REVERSE_GROWTH_AGENT = ReverseGrowthAgentItem()
        val ROD_AGENT = RodAgentItem()
        val ADVANCED_ELECTRIC_ROD = AdvancedElectricRodItem()
        val INDUSTRIAL_ROD = IndustrialElectricRodItem()
        val FirecrackerRodItem = FirecrackerRodItem()
        val INSERTION_PEDESTAL = InsertionPedestalItem()
        val RETRIEVER = RetrieverItem()
        val SHENBAO = Item(Item.Properties().food(FoodProperties.Builder()
            .nutrition(1).statusEffect(MobEffectInstance(JREffects.STRONG_EFFECT.entry(), 6000,1,false,true),1f).alwaysEdible()
            .build()))
        val FIREWORKS_ROD = FireworksRodItem()
        val TRIBOCHARGING_ROD = TribochargingRod(Item.Properties())
        val XP_GUN = XPGun()
        val REMOTE_CONTROL = RemoteControlItem(Item.Properties())
        val ICED_TEA = IcedTeaItem(Item.Properties())
        val FREE_MATING = FreeMatingItem(Item.Properties())
        val SANITARY_TOWEL = SanitaryTowel(Item.Properties())
        val STERILIZATION_PILLS = SterilizationPills(Item.Properties())
        val BYT = Item(Item.Properties())
        val MOLE = Item(Item.Properties().food(FoodProperties.Builder().nutrition(1)
            .statusEffect(MobEffectInstance(MobEffects.CONFUSION,10,0),1f).alwaysEdible().build()))
        val HPV_VACCINE = HPVVaccine(Item.Properties())
        val COTTON_SWAB = CottonSwabItem(Item.Properties().stacksTo(1))
        val SCALPEL = ScalpelItem(Item.Properties())
        val UTERUS = Item(Item.Properties().food(FoodProperties.Builder().nutrition(6).alwaysEdible().build()))
        val BRITH_CONTROLLING_PILL = BrithControllingPill(Item.Properties())
        val ABORtiON_PILL = AbortionPillItem(Item.Properties())
        val ESTROGEN = EstrogenItem(Item.Properties())
        val TESTOSTERONE = TestosteroneItem(Item.Properties())
        val ANTI_ANDROGEN = AntiAndrogenItem(Item.Properties())
        val AROMATASE = object : Item(Item.Properties().stacksTo(1)) {
            // 这个物品参与合成后不消耗
            override fun hasCraftingRemainingItem(): Boolean {
                return true
            }
            override fun getCraftingRemainingItem(stack: ItemStack?): ItemStack {
                return ItemStack(this)
            }
        }
        val PENICILLIN = PenicillinItem(Item.Properties())
        val BALL_MOUTH = BallMouthItem(Item.Properties())
        val ELECTRIC_SHOCK_DEVICE = ElectricShockDeviceItem(Item.Properties())
        val ELECTRIC_SHOCK_CONTROLLER = ElectricShockController(Item.Properties())
        val WHIP = WhipItem(Item.Properties())
        val CONTRACT_WHIP = ContractWhipItem(Item.Properties())
        val BINDING_ROPE = BindingRopeItem(Item.Properties())
        val EYE_PATCH = EyePatchItem(Item.Properties())
        val EARPLUG = EarplugItem(Item.Properties())
        val HANDCUFFES = HandcuffesItem(Item.Properties())
        val SHACKLES = ShacklesItem(Item.Properties())
        val HANDCUFFES_RING = Item(Item.Properties())
        val HANDCUFFES_CHAIN = Item(Item.Properties())
        val NO_MATING_PLZ = NoMatingPlz(Item.Properties())
        val EXCREMENT = BoneMealItem(Item.Properties().food(FoodProperties.Builder().alwaysEdible().nutrition(1).statusEffect(
            MobEffectInstance(MobEffects.CONFUSION,200,0),1f).build()))
        val FEMALE_POTION = GenderChangePotionItem(Item.Properties(),GenderChangePotionItem.Gender.FEMALE)
        val MALE_POTION = GenderChangePotionItem(Item.Properties(), GenderChangePotionItem.Gender.MALE)
        val SPERM_RETRIEVAL_DEVICE = SpermRetrievalDeviceItem(5*60*20,Item.Properties())
        val FROZEN_SPERM_RETRIEVAL_DEVICE = FrozenSpermRetrievalDeviceItem(Item.Properties())
        val CLONER_DEVICE = ClonerDevice()
        val PANTSU = PantsuItem(JRArmorMaterials.PANTSU_MATERIAL, ArmorItem.Type.LEGGINGS, Item.Properties().stacksTo(1))
        val PANTSU_GETTER = PantsuGetterItem(Item.Properties().stacksTo(1))
        val DIAPER = DiaperItem(JRArmorMaterials.DIAPER_MATERIAL, ArmorItem.Type.LEGGINGS, Item.Properties().stacksTo(1))
        val AIDS_VACCINE = AidsVaccine(Item.Properties())
        val TAMSULOSIN_CAPSULE = TamsulosinCapsuleItem(Item.Properties())
        val PARTHENOGENESIS_CATALYST = ParthenogenesisCatalystItem(Item.Properties().stacksTo(1))
        val YURI_MATING_MATING_MATING = YuriMatingMatingMatingItem()

        var JR_ITEM_GROUP_KEY: ResourceKey<CreativeModeTab>? = null
        var JR_ITEM_GROUP: CreativeModeTab? = null
        fun init(){
            // 注册物品
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "slime_rod"), SLIME_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "giant_rod"), GIANT_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "lubricating_book"), LUBRICATING_BOOK)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "redstone_rod"), REDSTONE_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "cactus_rod"), CACTUS_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "eatable_rod"), EATABLE_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "long_rod"), LONG_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID,"longer_rod"), LONGER_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "lightning_end_rod"), LIGHTNING_END_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "networking_rod"), NETWORKING_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "basic_electric_rod"), BASIC_ELECTRIC_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "bremelanotide"), BREMELANOTIDE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "growth_agent"), GROWTH_AGENT)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "reverse_growth_agent"), REVERSE_GROWTH_AGENT)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "rod_agent"), ROD_AGENT)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "advanced_electric_rod"), ADVANCED_ELECTRIC_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "industrial_electric_rod"), INDUSTRIAL_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "firecracker_rod"), FirecrackerRodItem)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "tribocharging_rod"), TRIBOCHARGING_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "insertion_pedestal"), INSERTION_PEDESTAL)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "retriever"), RETRIEVER)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "shenbao"), SHENBAO)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, FireworksRodItem.ID), FIREWORKS_ROD)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "xp_gun"), XP_GUN)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "remote_control"), REMOTE_CONTROL)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "iced_tea"), ICED_TEA)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "free_mating"), FREE_MATING)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "sanitary_towel"), SANITARY_TOWEL)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "sterilization_pills"), STERILIZATION_PILLS)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "byt"), BYT)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "mole"), MOLE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "hpv_vaccine"), HPV_VACCINE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "cotton_swab"), COTTON_SWAB)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "scalpel"), SCALPEL)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "uterus"), UTERUS)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "brith_controlling_pill"), BRITH_CONTROLLING_PILL)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "abortion_pill"), ABORtiON_PILL)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "estrogen"), ESTROGEN)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "testosterone"), TESTOSTERONE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "anti_androgen"), ANTI_ANDROGEN)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "aromatase"),AROMATASE )
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "penicillin"), PENICILLIN)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "ball_mouth"), BALL_MOUTH)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "electric_shock_device"), ELECTRIC_SHOCK_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "electric_shock_controller"), ELECTRIC_SHOCK_CONTROLLER)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "whip"), WHIP)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "contract_whip"), CONTRACT_WHIP)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "binding_rope"), BINDING_ROPE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "eye_patch"), EYE_PATCH)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "earplug"), EARPLUG)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "handcuffes"), HANDCUFFES)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "shackles"), SHACKLES)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "handcuffes_ring"), HANDCUFFES_RING)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "handcuffes_chain"), HANDCUFFES_CHAIN)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "no_mating_plz"), NO_MATING_PLZ)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "excrement"), EXCREMENT)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID,"female_potion"),FEMALE_POTION)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID,"male_potion"),MALE_POTION)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "sperm_retrieval_device"), SPERM_RETRIEVAL_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "frozen_sperm_retrieval_device"), FROZEN_SPERM_RETRIEVAL_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "cloner_device"), CLONER_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "pantsu"), PANTSU)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "pantsu_getter"), PANTSU_GETTER)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "diaper"), DIAPER)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "aids_vaccine"), AIDS_VACCINE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "tamsulosin_capsule"), TAMSULOSIN_CAPSULE)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "parthenogenesis_catalyst"), PARTHENOGENESIS_CATALYST)
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "yuri_mating_mating_mating"), YURI_MATING_MATING_MATING)
            // 注册物品组
            JR_ITEM_GROUP_KEY = ResourceKey.of(BuiltInRegistries.ITEM_GROUP.key, ResourceLocation.fromNamespaceAndPath(MODID, "item_group"))
            JR_ITEM_GROUP = FabricItemGroup.builder()
                .icon { ItemStack(SLIME_ROD) }
                .displayName(Component.translatable("itemGroup.justarod"))
                .build()
            Registry.register(BuiltInRegistries.ITEM_GROUP, JR_ITEM_GROUP_KEY, JR_ITEM_GROUP)

            ItemGroupEvents.modifyEntriesEvent(JR_ITEM_GROUP_KEY!!).register { entries ->
                entries.accept(SLIME_ROD)
                entries.accept(GIANT_ROD)
                entries.accept(LUBRICATING_BOOK)
                entries.accept(REDSTONE_ROD)
                entries.accept(CACTUS_ROD)
                entries.accept(EATABLE_ROD)
                entries.accept(LONG_ROD)
                entries.accept(LONGER_ROD)
                entries.accept(LIGHTNING_END_ROD)
                entries.accept(NETWORKING_ROD)
                entries.accept(GOLDEN_LEAVES)
                entries.accept(BASIC_ELECTRIC_ROD)
                entries.accept(ADVANCED_ELECTRIC_ROD)
                entries.accept(INDUSTRIAL_ROD)
                entries.accept(FirecrackerRodItem)
                entries.accept(BREMELANOTIDE)
                entries.accept(GROWTH_AGENT)
                entries.accept(REVERSE_GROWTH_AGENT)
                entries.accept(ROD_AGENT)
                entries.accept(INSERTION_PEDESTAL)
                entries.accept(RETRIEVER)
                entries.accept(SHENBAO)
                entries.accept(FIREWORKS_ROD)
                entries.accept(TRIBOCHARGING_ROD)
                entries.accept(XP_GUN)
                entries.accept(REMOTE_CONTROL)
                entries.accept(ICED_TEA)
                entries.accept(FREE_MATING)
                entries.accept(SANITARY_TOWEL)
                entries.accept(STERILIZATION_PILLS)
                entries.accept(BYT)
                entries.accept(MOLE)
                entries.accept(HPV_VACCINE)
                entries.accept(COTTON_SWAB)
                entries.accept(SCALPEL)
                entries.accept(UTERUS)
                entries.accept(BRITH_CONTROLLING_PILL)
                entries.accept(ABORtiON_PILL)
                entries.accept(ESTROGEN)
                entries.accept(TESTOSTERONE)
                entries.accept(ANTI_ANDROGEN)
                entries.accept(AROMATASE)
                entries.accept(PENICILLIN)
                entries.accept(BALL_MOUTH)
                entries.accept(ELECTRIC_SHOCK_DEVICE)
                entries.accept(ELECTRIC_SHOCK_CONTROLLER)
                entries.accept(WHIP)
                entries.accept(CONTRACT_WHIP)
                entries.accept(BINDING_ROPE)
                entries.accept(EYE_PATCH)
                entries.accept(EARPLUG)
                entries.accept(HANDCUFFES)
                entries.accept(SHACKLES)
                entries.accept(HANDCUFFES_RING)
                entries.accept(HANDCUFFES_CHAIN)
                entries.accept(NO_MATING_PLZ)
                entries.accept(EXCREMENT)
                entries.accept(FEMALE_POTION)
                entries.accept(MALE_POTION)
                entries.accept(SPERM_RETRIEVAL_DEVICE)
                entries.accept(FROZEN_SPERM_RETRIEVAL_DEVICE)
                entries.accept(CLONER_DEVICE)
                entries.accept(PANTSU)
                entries.accept(PANTSU_GETTER)
                entries.accept(DIAPER)
                entries.accept(AIDS_VACCINE)
                entries.accept(TAMSULOSIN_CAPSULE)
                entries.accept(PARTHENOGENESIS_CATALYST)
                entries.accept(YURI_MATING_MATING_MATING)
            }
        }
    }
}

fun MobEffect?.entry(): Holder<MobEffect>? {
    return BuiltInRegistries.MOB_EFFECT.getOrThrow(this)
}
