package org.cneko.justarod.test;

import com.geckolib.constant.DataTickets;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.cneko.justarod.client.renderer.JREntityGeoState;
import org.cneko.justarod.client.renderer.RodRenderer;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.RodEntity;

/** Regression for summoned rods crashing in TemptGoal and GeckoLib state extraction. */
public class RodClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Object oldSquash = context.computeOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().get("fun.entity_squash.enabled"));
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().set("fun.entity_squash.enabled", false));
            var server = world.getServer();
            int id = server.computeOnServer(game -> {
                var level = game.overworld();
                var player = game.getPlayerList().getPlayers().getFirst();
                for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                }
                player.teleportTo(0.5, 80, -4.5);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.END_ROD));
                var rod = JREntities.ROD.create(level, EntitySpawnReason.COMMAND);
                check(rod != null, "Summoned rod can be created");
                rod.setPos(0.5, 80, 0.5);
                rod.setNoAi(false);
                level.addFreshEntity(rod);
                check(rod.getAttribute(Attributes.TEMPT_RANGE) != null, "Rod has the animal temptation attribute");
                check(rod.getAttributeValue(Attributes.TEMPT_RANGE) == 10.0, "Rod uses the vanilla animal temptation range");
                check(rod.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4.0, "Rod retains its attack damage");
                check(new TemptGoal(rod, 0.8, stack -> stack.is(Items.END_ROD), false).canUse(),
                        "The real TemptGoal finds a player holding an end rod without missing attributes");
                return rod.getId();
            });
            context.waitFor(mc -> mc.level != null && mc.level.getEntity(id) instanceof RodEntity);
            context.runOnClient(mc -> {
                var rod = (RodEntity) mc.level.getEntity(id);
                var model = ((RodRenderer) mc.getEntityRenderDispatcher().getRenderer(rod)).getGeoModel();
                for (String name : new String[]{"misc.idle", "misc.sit", "move.walk", "move.run", "move.crawl", "move.swim"}) {
                    check(model.getBakedAnimation(rod, name) != null, "Local rod animation is loaded: " + name);
                }
                for (float partialTick : new float[]{0f, 0.5f, 0.9f}) {
                    var state = (JREntityGeoState) mc.getEntityRenderDispatcher().extractEntity(rod, partialTick);
                    check(!model.getBakedModel(model.getModelResource(state)).isMissingno(),
                            "Rod renders its actual model rather than the missing-model fallback");
                    check(state.hasGeckolibData(DataTickets.ANIMATABLE_MANAGER), "Animation manager is present in the state");
                    check(state.getGeckolibData(DataTickets.ANIMATABLE_MANAGER) != null,
                            "Written animation manager can be read from the same data map");
                    check(state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES) != null,
                            "GeckoLib extracts animation controllers successfully");
                    check(state.getGeckolibData(JREntityGeoState.Companion.getROD_ENTITY()) == rod,
                            "Custom rod data also survives state extraction");
                }
            });
            context.waitTicks(20);
            context.runOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().set("fun.entity_squash.enabled", true));
            context.getInput().lookAt(0, 25);
            context.waitTicks(40);
            server.runOnServer(game -> {
                var rod = (RodEntity) game.overworld().getEntity(id);
                check(rod != null && rod.isAlive() && rod.tickCount >= 40,
                        "Rod continues ticking with normal AI and the visual effect enabled");
            });
            context.takeScreenshot("rod-render-squash-spin");
        } finally {
            context.runOnClient(mc -> JRConfig.CONFIG_BUILDER.getConfig().set("fun.entity_squash.enabled", oldSquash));
        }
    }
}
