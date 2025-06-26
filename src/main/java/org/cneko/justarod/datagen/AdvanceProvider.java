package org.cneko.justarod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.cneko.justarod.JRCriteria;
import org.cneko.justarod.advancment.criterion.ItemUsedOnEntityCriterion;
import org.cneko.justarod.item.JRItems;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.cneko.justarod.Justarod.MODID;

public class AdvanceProvider extends FabricAdvancementProvider {
    protected AdvanceProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup wrapperLookup, Consumer<AdvancementEntry> consumer) {
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

        AdvancementEntry grassSheep = Advancement.Builder.create()
                .display(
                        JRItems.Companion.getINSERTION_PEDESTAL(),
                        Text.literal("草羊机"),
                        Text.literal("对羊使用插入底座"),
                        Identifier.ofVanilla("textures/gui/advancements/backgrounds/adventure.png"),
                        AdvancementFrame.TASK,
                        true,
                        true,
                        false
                )
                .criterion("grass_sheep",
                        JRCriteria.ITEM_USED_ON_ENTITY_CRITERION.create(
                        ItemUsedOnEntityCriterion.create(
                            ItemPredicate.Builder.create().items(JRItems.Companion.getINSERTION_PEDESTAL()).build(),
                            EntityPredicate.Builder.create().type(EntityType.SHEEP).build()
                        )
                ))
                .build(consumer, MODID + ":grass_sheep");
    }
}
