package org.cneko.justarod.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.component.type.FoodComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.BoneMealItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID

import org.cneko.justarod.block.JRBlocks.*
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.*
import org.cneko.justarod.item.rod.*
import org.cneko.justarod.item.armor.*
import org.cneko.justarod.item.bdsm.*
import org.cneko.justarod.item.bio.ClonerDevice
import org.cneko.justarod.item.electric.*
import org.cneko.justarod.item.medical.*
import org.cneko.justarod.item.syringe.*


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
        val SHENBAO = Item(Item.Settings().food(FoodComponent.Builder()
            .nutrition(1).statusEffect(StatusEffectInstance(JREffects.STRONG_EFFECT.entry(), 6000,1,false,true),1f).alwaysEdible()
            .build()))
        val FIREWORKS_ROD = FireworksRodItem()
        val TRIBOCHARGING_ROD = TribochargingRod(Item.Settings())
        val XP_GUN = XPGun()
        val REMOTE_CONTROL = RemoteControlItem(Item.Settings())
        val ICED_TEA = IcedTeaItem(Item.Settings())
        val FREE_MATING = FreeMatingItem(Item.Settings())
        val SANITARY_TOWEL = SanitaryTowel(Item.Settings())
        val STERILIZATION_PILLS = SterilizationPills(Item.Settings())
        val BYT = Item(Item.Settings())
        val MOLE = Item(Item.Settings().food(FoodComponent.Builder().nutrition(1)
            .statusEffect(StatusEffectInstance(StatusEffects.NAUSEA,10,0),1f).alwaysEdible().build()))
        val HPV_VACCINE = HPVVaccine(Item.Settings())
        val COTTON_SWAB = CottonSwabItem(Item.Settings().maxCount(1))
        val SCALPEL = ScalpelItem(Item.Settings())
        val UTERUS = Item(Item.Settings().food(FoodComponent.Builder().nutrition(6).alwaysEdible().build()))
        val BRITH_CONTROLLING_PILL = BrithControllingPill(Item.Settings())
        val ABORtiON_PILL = AbortionPillItem(Item.Settings())
        val ESTROGEN = EstrogenItem(Item.Settings())
        val PENICILLIN = PenicillinItem(Item.Settings())
        val BALL_MOUTH = BallMouthItem(Item.Settings())
        val ELECTRIC_SHOCK_DEVICE = ElectricShockDeviceItem(Item.Settings())
        val ELECTRIC_SHOCK_CONTROLLER = ElectricShockController(Item.Settings())
        val WHIP = WhipItem(Item.Settings())
        val CONTRACT_WHIP = ContractWhipItem(Item.Settings())
        val BINDING_ROPE = BindingRopeItem(Item.Settings())
        val EYE_PATCH = EyePatchItem(Item.Settings())
        val EARPLUG = EarplugItem(Item.Settings())
        val HANDCUFFES = HandcuffesItem(Item.Settings())
        val SHACKLES = ShacklesItem(Item.Settings())
        val HANDCUFFES_RING = Item(Item.Settings())
        val HANDCUFFES_CHAIN = Item(Item.Settings())
        val NO_MATING_PLZ = NoMatingPlz(Item.Settings())
        val EXCREMENT = BoneMealItem(Item.Settings().food(FoodComponent.Builder().alwaysEdible().nutrition(1).statusEffect(
            StatusEffectInstance(StatusEffects.NAUSEA,200,0),1f).build()))
        val FEMALE_POTION = GenderChangePotionItem(Item.Settings(),GenderChangePotionItem.Gender.FEMALE)
        val MALE_POTION = GenderChangePotionItem(Item.Settings(), GenderChangePotionItem.Gender.MALE)
        val SPERM_RETRIEVAL_DEVICE = SpermRetrievalDeviceItem(5*60*20,Item.Settings())
        val FROZEN_SPERM_RETRIEVAL_DEVICE = FrozenSpermRetrievalDeviceItem(Item.Settings())
        val CLONER_DEVICE = ClonerDevice()

        var JR_ITEM_GROUP_KEY: RegistryKey<ItemGroup>? = null
        var JR_ITEM_GROUP: ItemGroup? = null
        fun init(){
            // 注册物品
            Registry.register(Registries.ITEM, Identifier.of(MODID, "slime_rod"), SLIME_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "giant_rod"), GIANT_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "lubricating_book"), LUBRICATING_BOOK)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "redstone_rod"), REDSTONE_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "cactus_rod"), CACTUS_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "eatable_rod"), EATABLE_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "long_rod"), LONG_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID,"longer_rod"), LONGER_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "lightning_end_rod"), LIGHTNING_END_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "networking_rod"), NETWORKING_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "basic_electric_rod"), BASIC_ELECTRIC_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "bremelanotide"), BREMELANOTIDE)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "growth_agent"), GROWTH_AGENT)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "reverse_growth_agent"), REVERSE_GROWTH_AGENT)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "rod_agent"), ROD_AGENT)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "advanced_electric_rod"), ADVANCED_ELECTRIC_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "industrial_electric_rod"), INDUSTRIAL_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "firecracker_rod"), FirecrackerRodItem)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "tribocharging_rod"), TRIBOCHARGING_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "insertion_pedestal"), INSERTION_PEDESTAL)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "retriever"), RETRIEVER)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "shenbao"), SHENBAO)
            Registry.register(Registries.ITEM, Identifier.of(MODID, FireworksRodItem.ID), FIREWORKS_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "xp_gun"), XP_GUN)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "remote_control"), REMOTE_CONTROL)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "iced_tea"), ICED_TEA)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "free_mating"), FREE_MATING)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "sanitary_towel"), SANITARY_TOWEL)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "sterilization_pills"), STERILIZATION_PILLS)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "byt"), BYT)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "mole"), MOLE)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "hpv_vaccine"), HPV_VACCINE)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "cotton_swab"), COTTON_SWAB)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "scalpel"), SCALPEL)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "uterus"), UTERUS)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "brith_controlling_pill"), BRITH_CONTROLLING_PILL)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "abortion_pill"), ABORtiON_PILL)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "estrogen"), ESTROGEN)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "penicillin"), PENICILLIN)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "ball_mouth"), BALL_MOUTH)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "electric_shock_device"), ELECTRIC_SHOCK_DEVICE)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "electric_shock_controller"), ELECTRIC_SHOCK_CONTROLLER)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "whip"), WHIP)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "contract_whip"), CONTRACT_WHIP)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "binding_rope"), BINDING_ROPE)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "eye_patch"), EYE_PATCH)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "earplug"), EARPLUG)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "handcuffes"), HANDCUFFES)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "shackles"), SHACKLES)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "handcuffes_ring"), HANDCUFFES_RING)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "handcuffes_chain"), HANDCUFFES_CHAIN)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "no_mating_plz"), NO_MATING_PLZ)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "excrement"), EXCREMENT)
            Registry.register(Registries.ITEM, Identifier.of(MODID,"female_potion"),FEMALE_POTION)
            Registry.register(Registries.ITEM, Identifier.of(MODID,"male_potion"),MALE_POTION)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "sperm_retrieval_device"), SPERM_RETRIEVAL_DEVICE)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "frozen_sperm_retrieval_device"), FROZEN_SPERM_RETRIEVAL_DEVICE)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "cloner_device"), CLONER_DEVICE)

            // 注册物品组
            JR_ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.key, Identifier.of(MODID, "item_group"))
            JR_ITEM_GROUP = FabricItemGroup.builder()
                .icon { ItemStack(SLIME_ROD) }
                .displayName(Text.translatable("itemGroup.justarod"))
                .build()
            Registry.register(Registries.ITEM_GROUP, JR_ITEM_GROUP_KEY, JR_ITEM_GROUP)

            ItemGroupEvents.modifyEntriesEvent(JR_ITEM_GROUP_KEY!!).register { entries ->
                entries.add(SLIME_ROD)
                entries.add(GIANT_ROD)
                entries.add(LUBRICATING_BOOK)
                entries.add(REDSTONE_ROD)
                entries.add(CACTUS_ROD)
                entries.add(EATABLE_ROD)
                entries.add(LONG_ROD)
                entries.add(LONGER_ROD)
                entries.add(LIGHTNING_END_ROD)
                entries.add(NETWORKING_ROD)
                entries.add(GOLDEN_LEAVES)
                entries.add(BASIC_ELECTRIC_ROD)
                entries.add(ADVANCED_ELECTRIC_ROD)
                entries.add(INDUSTRIAL_ROD)
                entries.add(FirecrackerRodItem)
                entries.add(BREMELANOTIDE)
                entries.add(GROWTH_AGENT)
                entries.add(REVERSE_GROWTH_AGENT)
                entries.add(ROD_AGENT)
                entries.add(INSERTION_PEDESTAL)
                entries.add(RETRIEVER)
                entries.add(SHENBAO)
                entries.add(FIREWORKS_ROD)
                entries.add(TRIBOCHARGING_ROD)
                entries.add(XP_GUN)
                entries.add(REMOTE_CONTROL)
                entries.add(ICED_TEA)
                entries.add(FREE_MATING)
                entries.add(SANITARY_TOWEL)
                entries.add(STERILIZATION_PILLS)
                entries.add(BYT)
                entries.add(MOLE)
                entries.add(HPV_VACCINE)
                entries.add(COTTON_SWAB)
                entries.add(SCALPEL)
                entries.add(UTERUS)
                entries.add(BRITH_CONTROLLING_PILL)
                entries.add(ABORtiON_PILL)
                entries.add(ESTROGEN)
                entries.add(PENICILLIN)
                entries.add(BALL_MOUTH)
                entries.add(ELECTRIC_SHOCK_DEVICE)
                entries.add(ELECTRIC_SHOCK_CONTROLLER)
                entries.add(WHIP)
                entries.add(CONTRACT_WHIP)
                entries.add(BINDING_ROPE)
                entries.add(EYE_PATCH)
                entries.add(EARPLUG)
                entries.add(HANDCUFFES)
                entries.add(SHACKLES)
                entries.add(HANDCUFFES_RING)
                entries.add(HANDCUFFES_CHAIN)
                entries.add(NO_MATING_PLZ)
                entries.add(EXCREMENT)
                entries.add(FEMALE_POTION)
                entries.add(MALE_POTION)
                entries.add(SPERM_RETRIEVAL_DEVICE)
                entries.add(FROZEN_SPERM_RETRIEVAL_DEVICE)
                entries.add(CLONER_DEVICE)
            }
        }
    }
}

fun StatusEffect?.entry(): RegistryEntry<StatusEffect>? {
    return Registries.STATUS_EFFECT.getEntry(this)
}
