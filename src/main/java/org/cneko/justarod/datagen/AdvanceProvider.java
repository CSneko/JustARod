package org.cneko.justarod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.cneko.justarod.JRCriteria;
import org.cneko.justarod.advancment.criterion.ItemUsedOnEntityCriterion;
import org.cneko.justarod.item.JRItems;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.cneko.justarod.Justarod.MODID;

public class AdvanceProvider extends FabricAdvancementProvider {
    protected AdvanceProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider wrapperLookup, Consumer<AdvancementHolder> consumer) {
//        // 获取进度注册表包装器
//        var advancementRegistry = wrapperLookup.getWrapperOrThrow(RegistryKeys.ADVANCEMENT);
//        // 获取 rods/root 的 AdvancementEntry
//        var rootKey = RegistryKey.of(RegistryKeys.ADVANCEMENT, Identifier.of(MODID, "rods/root"));
//        Optional<RegistryEntry.Reference<Advancement>> rootRefOpt = advancementRegistry.getOptional(rootKey);
//        if (rootRefOpt.isEmpty()) {
//            throw new IllegalStateException("Root advancement not found: " + rootKey);
//        }
//        var rootRef = rootRefOpt.get();
//        AdvancementEntry rootEntry = new AdvancementEntry(Identifier.of(MODID, "rods/root"),rootRef.value());

        AdvancementHolder grassSheep = Advancement.Builder.advancement()
                .display(
                        JRItems.Companion.getINSERTION_PEDESTAL(),
                        Component.literal("草羊机"),
                        Component.literal("对羊使用插入底座"),
                        ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .criterion("grass_sheep",
                        JRCriteria.ITEM_USED_ON_ENTITY_CRITERION.createCriterion(
                        ItemUsedOnEntityCriterion.create(
                            ItemPredicate.Builder.item().of(JRItems.Companion.getINSERTION_PEDESTAL()).build(),
                            EntityPredicate.Builder.entity().of(EntityType.SHEEP).build()
                        )
                ))
                .build(consumer, MODID + ":grass_sheep");
    }
}
