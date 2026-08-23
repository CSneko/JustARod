package org.cneko.justarod.advancment.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.cneko.justarod.Justarod;

import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class ItemUsedOnEntityCriterion extends SimpleCriterionTrigger<ItemUsedOnEntityCriterion.Conditions> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Justarod.MODID, "item_used_on_entity");

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player, ItemStack stack, Entity entity) {
        this.trigger(player, conditions -> conditions.matches(player,stack,entity));
    }

    public record Conditions(
            Optional<ContextAwarePredicate> playerPredicate,
            ItemPredicate itemPredicate,
            EntityPredicate entityPredicate
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::playerPredicate),
                        ItemPredicate.CODEC.fieldOf("item").forGetter(Conditions::itemPredicate),
                        EntityPredicate.CODEC.fieldOf("entity").forGetter(Conditions::entityPredicate)
                ).apply(instance, Conditions::new));

        @Override
        public Optional<ContextAwarePredicate> player() {
            return playerPredicate;
        }

        public boolean matches(ServerPlayer player,ItemStack stack, Entity entity) {
            return itemPredicate.test(stack) && entityPredicate().matches(player,entity);
        }
    }

    public static Conditions create(ItemPredicate item, EntityPredicate entity) {
        return new Conditions(Optional.empty(), item, entity);
    }
}