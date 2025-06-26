package org.cneko.justarod.advancment.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.cneko.justarod.Justarod;

import java.util.Optional;

public class ItemUsedOnEntityCriterion extends AbstractCriterion<ItemUsedOnEntityCriterion.Conditions> {
    public static final Identifier ID = Identifier.of(Justarod.MODID, "item_used_on_entity");

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, ItemStack stack, Entity entity) {
        this.trigger(player, conditions -> conditions.matches(player,stack,entity));
    }

    public record Conditions(
            Optional<LootContextPredicate> playerPredicate,
            ItemPredicate itemPredicate,
            EntityPredicate entityPredicate
    ) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        LootContextPredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::playerPredicate),
                        ItemPredicate.CODEC.fieldOf("item").forGetter(Conditions::itemPredicate),
                        EntityPredicate.CODEC.fieldOf("entity").forGetter(Conditions::entityPredicate)
                ).apply(instance, Conditions::new));

        @Override
        public Optional<LootContextPredicate> player() {
            return playerPredicate;
        }

        public boolean matches(ServerPlayerEntity player,ItemStack stack, Entity entity) {
            return itemPredicate.test(stack) && entityPredicate().test(player,entity);
        }
    }

    public static Conditions create(ItemPredicate item, EntityPredicate entity) {
        return new Conditions(Optional.empty(), item, entity);
    }
}