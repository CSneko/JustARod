package org.cneko.justarod.item

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.cneko.justarod.Justarod.MODID
import org.cneko.justarod.JRIds

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
import org.cneko.justarod.item.slime.DecomposeCatalystItem
import org.cneko.justarod.item.slime.SlimeBellItem

/*
仙之人兮列如喵
 */
class JRItems {
    companion object{
        val SLIME_ROD = SlimeRodItem()
        // ===== 驯服史莱姆系统 =====
        /** 史莱姆铃铛：确认驯服（信任满的野生史莱姆）/ 召回已绑定的驯服史莱姆 */
        val SLIME_BELL = SlimeBellItem(JRIds.itemProps("slime_bell").stacksTo(1))
        /** 分解催化物：岩浆膏 + 虚弱药水 + 金苹果合成，喂给驯服史莱姆开启分解队列 */
        val DECOMPOSE_CATALYST = DecomposeCatalystItem(JRIds.itemProps("decompose_catalyst").stacksTo(16))
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
        // 26.x：FoodProperties 不再携带药水效果，效果迁移到 CONSUMABLE 组件
        val SHENBAO = Item(JRIds.itemProps("shenbao").food(FoodProperties.Builder()
            .nutrition(1).alwaysEdible()
            .build())
            .component(DataComponents.CONSUMABLE, Consumable.builder()
                .onConsume(ApplyStatusEffectsConsumeEffect(MobEffectInstance(JREffects.STRONG_EFFECT, 6000,1,false,true)))
                .build()))
        val FIREWORKS_ROD = FireworksRodItem()
        val TRIBOCHARGING_ROD = TribochargingRod(JRIds.itemProps("tribocharging_rod"))
        val XP_GUN = XPGun()
        val REMOTE_CONTROL = RemoteControlItem(JRIds.itemProps("remote_control"))
        val ICED_TEA = IcedTeaItem(JRIds.itemProps("iced_tea"))
        val FREE_MATING = FreeMatingItem(JRIds.itemProps("free_mating"))
        val SANITARY_TOWEL = SanitaryTowel(JRIds.itemProps("sanitary_towel"))
        val STERILIZATION_PILLS = SterilizationPills(JRIds.itemProps("sterilization_pills"))
        val BYT = Item(JRIds.itemProps("byt"))
        val MOLE = Item(JRIds.itemProps("mole").food(FoodProperties.Builder().nutrition(1)
            .alwaysEdible().build())
            .component(DataComponents.CONSUMABLE, Consumable.builder()
                .onConsume(ApplyStatusEffectsConsumeEffect(MobEffectInstance(MobEffects.NAUSEA,10,0)))
                .build()))
        val HPV_VACCINE = HPVVaccine(JRIds.itemProps("hpv_vaccine"))
        val COTTON_SWAB = CottonSwabItem(JRIds.itemProps("cotton_swab").stacksTo(1))
        val SCALPEL = ScalpelItem(JRIds.itemProps("scalpel"))
        val UTERUS = Item(JRIds.itemProps("uterus").food(FoodProperties.Builder().nutrition(6).alwaysEdible().build()))
        val BRITH_CONTROLLING_PILL = BrithControllingPill(JRIds.itemProps("brith_controlling_pill"))
        val ABORtiON_PILL = AbortionPillItem(JRIds.itemProps("abortion_pill"))
        val ESTROGEN = EstrogenItem(JRIds.itemProps("estrogen"))
        val TESTOSTERONE = TestosteroneItem(JRIds.itemProps("testosterone"))
        val ANTI_ANDROGEN = AntiAndrogenItem(JRIds.itemProps("anti_androgen"))
        val AROMATASE = Item(JRIds.itemProps("aromatase").stacksTo(1))
        val PENICILLIN = PenicillinItem(JRIds.itemProps("penicillin"))
        val BALL_MOUTH = BallMouthItem(JRIds.itemProps("ball_mouth"))
        val ELECTRIC_SHOCK_DEVICE = ElectricShockDeviceItem(JRIds.itemProps("electric_shock_device"))
        val ELECTRIC_SHOCK_CONTROLLER = ElectricShockController(JRIds.itemProps("electric_shock_controller"))
        val WHIP = WhipItem(JRIds.itemProps("whip"))
        val CONTRACT_WHIP = ContractWhipItem(JRIds.itemProps("contract_whip"))
        val BINDING_ROPE = BindingRopeItem(JRIds.itemProps("binding_rope"))
        val EYE_PATCH = EyePatchItem(JRIds.itemProps("eye_patch"))
        val EARPLUG = EarplugItem(JRIds.itemProps("earplug"))
        val HANDCUFFES = HandcuffesItem(JRIds.itemProps("handcuffes"))
        val SHACKLES = ShacklesItem(JRIds.itemProps("shackles"))
        val HANDCUFFES_RING = Item(JRIds.itemProps("handcuffes_ring"))
        val HANDCUFFES_CHAIN = Item(JRIds.itemProps("handcuffes_chain"))
        val NO_MATING_PLZ = NoMatingPlz(JRIds.itemProps("no_mating_plz"))
        val EXCREMENT = BoneMealItem(JRIds.itemProps("excrement").food(FoodProperties.Builder().alwaysEdible().nutrition(1)
            .build())
            .component(DataComponents.CONSUMABLE, Consumable.builder()
                .onConsume(ApplyStatusEffectsConsumeEffect(MobEffectInstance(MobEffects.NAUSEA,200,0)))
                .build()))
        val FEMALE_POTION = GenderChangePotionItem(JRIds.itemProps("female_potion"),GenderChangePotionItem.Gender.FEMALE)
        val MALE_POTION = GenderChangePotionItem(JRIds.itemProps("male_potion"), GenderChangePotionItem.Gender.MALE)
        val SPERM_RETRIEVAL_DEVICE = SpermRetrievalDeviceItem(5*60*20,JRIds.itemProps("sperm_retrieval_device"))
        val FROZEN_SPERM_RETRIEVAL_DEVICE = FrozenSpermRetrievalDeviceItem(JRIds.itemProps("frozen_sperm_retrieval_device"))
        val CLONER_DEVICE = ClonerDevice()
        val PANTSU = PantsuItem(JRArmorMaterials.PANTSU_MATERIAL, JRIds.itemProps("pantsu").stacksTo(1))
        val PANTSU_GETTER = PantsuGetterItem(JRIds.itemProps("pantsu_getter").stacksTo(1))
        val DIAPER = DiaperItem(JRArmorMaterials.DIAPER_MATERIAL, JRIds.itemProps("diaper").stacksTo(1))
        val AIDS_VACCINE = AidsVaccine(JRIds.itemProps("aids_vaccine"))
        val TAMSULOSIN_CAPSULE = TamsulosinCapsuleItem(JRIds.itemProps("tamsulosin_capsule"))
        val CHEMOTHERAPY_DRUG = ChemotherapyDrugItem(JRIds.itemProps("chemotherapy_drug"))
        val PARTHENOGENESIS_CATALYST = ParthenogenesisCatalystItem(JRIds.itemProps("parthenogenesis_catalyst").stacksTo(1))
        val YURI_MATING_MATING_MATING = YuriMatingMatingMatingItem()

        var JR_ITEM_GROUP_KEY: ResourceKey<CreativeModeTab>? = null
        var JR_ITEM_GROUP: CreativeModeTab? = null
        fun init(){
            // 注册物品
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "slime_rod"), SLIME_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "slime_bell"), SLIME_BELL)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "decompose_catalyst"), DECOMPOSE_CATALYST)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "giant_rod"), GIANT_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "lubricating_book"), LUBRICATING_BOOK)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "redstone_rod"), REDSTONE_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "cactus_rod"), CACTUS_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "eatable_rod"), EATABLE_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "long_rod"), LONG_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID,"longer_rod"), LONGER_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "lightning_end_rod"), LIGHTNING_END_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "networking_rod"), NETWORKING_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "basic_electric_rod"), BASIC_ELECTRIC_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "bremelanotide"), BREMELANOTIDE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "growth_agent"), GROWTH_AGENT)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "reverse_growth_agent"), REVERSE_GROWTH_AGENT)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "rod_agent"), ROD_AGENT)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "advanced_electric_rod"), ADVANCED_ELECTRIC_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "industrial_electric_rod"), INDUSTRIAL_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "firecracker_rod"), FirecrackerRodItem)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "tribocharging_rod"), TRIBOCHARGING_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "insertion_pedestal"), INSERTION_PEDESTAL)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "retriever"), RETRIEVER)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "shenbao"), SHENBAO)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, FireworksRodItem.ID), FIREWORKS_ROD)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "xp_gun"), XP_GUN)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "remote_control"), REMOTE_CONTROL)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "iced_tea"), ICED_TEA)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "free_mating"), FREE_MATING)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "sanitary_towel"), SANITARY_TOWEL)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "sterilization_pills"), STERILIZATION_PILLS)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "byt"), BYT)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "mole"), MOLE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "hpv_vaccine"), HPV_VACCINE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "cotton_swab"), COTTON_SWAB)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "scalpel"), SCALPEL)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "uterus"), UTERUS)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "brith_controlling_pill"), BRITH_CONTROLLING_PILL)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "abortion_pill"), ABORtiON_PILL)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "estrogen"), ESTROGEN)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "testosterone"), TESTOSTERONE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "anti_androgen"), ANTI_ANDROGEN)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "aromatase"),AROMATASE )
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "penicillin"), PENICILLIN)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "ball_mouth"), BALL_MOUTH)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "electric_shock_device"), ELECTRIC_SHOCK_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "electric_shock_controller"), ELECTRIC_SHOCK_CONTROLLER)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "whip"), WHIP)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "contract_whip"), CONTRACT_WHIP)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "binding_rope"), BINDING_ROPE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "eye_patch"), EYE_PATCH)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "earplug"), EARPLUG)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "handcuffes"), HANDCUFFES)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "shackles"), SHACKLES)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "handcuffes_ring"), HANDCUFFES_RING)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "handcuffes_chain"), HANDCUFFES_CHAIN)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "no_mating_plz"), NO_MATING_PLZ)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "excrement"), EXCREMENT)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID,"female_potion"),FEMALE_POTION)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID,"male_potion"),MALE_POTION)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "sperm_retrieval_device"), SPERM_RETRIEVAL_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "frozen_sperm_retrieval_device"), FROZEN_SPERM_RETRIEVAL_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "cloner_device"), CLONER_DEVICE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "pantsu"), PANTSU)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "pantsu_getter"), PANTSU_GETTER)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "diaper"), DIAPER)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "aids_vaccine"), AIDS_VACCINE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "tamsulosin_capsule"), TAMSULOSIN_CAPSULE)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "chemotherapy_drug"), CHEMOTHERAPY_DRUG)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "parthenogenesis_catalyst"), PARTHENOGENESIS_CATALYST)
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MODID, "yuri_mating_mating_mating"), YURI_MATING_MATING_MATING)
            // 注册物品组
            JR_ITEM_GROUP_KEY = ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "item_group"))
            JR_ITEM_GROUP = FabricCreativeModeTab.builder()
                .icon { ItemStack(SLIME_ROD) }
                .title(Component.translatable("itemGroup.justarod"))
                .build()
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, JR_ITEM_GROUP_KEY!!, JR_ITEM_GROUP!!)

            CreativeModeTabEvents.modifyOutputEvent(JR_ITEM_GROUP_KEY!!).register { entries ->
                entries.accept(SLIME_ROD)
                entries.accept(SLIME_BELL)
                entries.accept(DECOMPOSE_CATALYST)
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
                entries.accept(CHEMOTHERAPY_DRUG)
                entries.accept(PARTHENOGENESIS_CATALYST)
                entries.accept(YURI_MATING_MATING_MATING)
            }
        }
    }
}

fun MobEffect?.entry(): Holder<MobEffect>? {
    return this?.let { BuiltInRegistries.MOB_EFFECT.wrapAsHolder(it) }
}
