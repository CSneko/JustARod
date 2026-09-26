package org.cneko.justarod.advancment.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.justarod.Justarod;

import java.util.Optional;

/**
 * 夜猫子成就触发器：疲劳达到极限等级时触发。
 */
public class NightOwlCriterion extends SimpleCriterionTrigger<NightOwlCriterion.Conditions> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Justarod.MODID, "night_owl");

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, conditions -> true);
    }

    public record Conditions(
            Optional<ContextAwarePredicate> playerPredicate
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::playerPredicate)
                ).apply(instance, Conditions::new));

        @Override
        public Optional<ContextAwarePredicate> player() {
            return playerPredicate;
        }
    }
}
