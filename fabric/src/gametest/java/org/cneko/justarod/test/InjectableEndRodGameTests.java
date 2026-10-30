package org.cneko.justarod.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.item.JRComponents;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.item.rod.InjectableEndRodItem;

/** 注入式末地烛：装液 / 混装拒绝 / 30 秒注入 / 岩浆点燃。 */
public class InjectableEndRodGameTests {

    private InjectableEndRodItem rod() {
        return (InjectableEndRodItem) JRItems.Companion.getINJECTABLE_END_ROD();
    }

    private ServerPlayer playerWithRod(GameTestHelper h) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(h);
        // 创造模式不会消耗桶，这里按生存玩家验证真实的消耗逻辑。
        // 26.x 的 setGameMode 不会同步 abilities，必须显式关掉 instabuild。
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rod()));
        return player;
    }

    @GameTest
    public void fillsFromOffhandBucketAndOnlyOneFluidType(GameTestHelper h) {
        ServerPlayer player = playerWithRod(h);
        ItemStack stack = player.getMainHandItem();

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        h.assertTrue(rod().use(h.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS,
                "A water bucket in the offhand fills the rod");
        h.assertTrue(InjectableEndRodItem.Companion.amountOf(stack) == 1000,
                "One bucket is 1000 mB, actual=" + InjectableEndRodItem.Companion.amountOf(stack));
        h.assertTrue("minecraft:water".equals(InjectableEndRodItem.Companion.fluidOf(stack)),
                "The stored fluid is water");
        h.assertTrue(player.getOffhandItem().is(Items.BUCKET), "The bucket is emptied, offhand=" + player.getOffhandItem());

        // 混装必须被拒绝，且不能消耗桶。
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LAVA_BUCKET));
        h.assertTrue(rod().use(h.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "A different fluid cannot be mixed in");
        h.assertTrue("minecraft:water".equals(InjectableEndRodItem.Companion.fluidOf(stack))
                        && InjectableEndRodItem.Companion.amountOf(stack) == 1000,
                "A rejected bucket leaves the stored fluid untouched");
        h.assertTrue(player.getOffhandItem().is(Items.LAVA_BUCKET), "A rejected bucket is not consumed");

        // 补满三桶后再灌要失败。
        for (int i = 0; i < 2; i++) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
            rod().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        }
        h.assertTrue(InjectableEndRodItem.Companion.amountOf(stack) == InjectableEndRodItem.MAX_FLUID,
                "Three buckets cap the storage at 3000 mB");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        h.assertTrue(rod().use(h.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "A fourth bucket is rejected while full");

        // 牛奶同样可用。
        ItemStack milkRod = new ItemStack(rod());
        player.setItemInHand(InteractionHand.MAIN_HAND, milkRod);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.MILK_BUCKET));
        h.assertTrue(rod().use(h.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS,
                "Milk can be loaded too");
        h.assertTrue("minecraft:milk".equals(InjectableEndRodItem.Companion.fluidOf(milkRod)), "Milk is stored as milk");
        h.succeed();
    }

    @GameTest
    public void injectsAfterThirtySecondsAndLavaSetsFire(GameTestHelper h) {
        ServerPlayer player = playerWithRod(h);
        ItemStack stack = player.getMainHandItem();
        stack.set(JRComponents.Companion.getINJECTABLE_FLUID(), InjectableEndRodItem.WATER);
        stack.set(JRComponents.Companion.getINJECTABLE_FLUID_AMOUNT(), 2000);

        // 差一 tick 不应触发：598 -> 599（还差 1 tick），下一次调用才到 600。
        stack.set(JRComponents.Companion.getUSED_TIME_MARK(), InjectableEndRodItem.INJECT_TICKS - 2);
        rod().onUse(stack, h.getLevel(), player, 0, true, 1);
        h.assertTrue(InjectableEndRodItem.Companion.amountOf(stack) == 2000, "Injection waits for a full 30 seconds");
        h.assertTrue(player.getRemainingFireTicks() == 0, "Water does not ignite the user");

        // 第 600 tick 触发，消耗一桶。
        rod().onUse(stack, h.getLevel(), player, 0, true, 1);
        h.assertTrue(InjectableEndRodItem.Companion.amountOf(stack) == 1000,
                "Each injection consumes exactly one bucket, actual=" + InjectableEndRodItem.Companion.amountOf(stack));
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 0, "Progress resets after an injection");

        // 岩浆：再插入 30 秒后点燃 15 秒。
        stack.set(JRComponents.Companion.getINJECTABLE_FLUID(), InjectableEndRodItem.LAVA);
        stack.set(JRComponents.Companion.getUSED_TIME_MARK(), InjectableEndRodItem.INJECT_TICKS - 1);
        rod().onUse(stack, h.getLevel(), player, 0, true, 1);
        h.assertTrue(player.getRemainingFireTicks() >= InjectableEndRodItem.LAVA_BURN_TICKS,
                "Lava injection sets the user on fire for 15 seconds, actual=" + player.getRemainingFireTicks());
        h.assertTrue(InjectableEndRodItem.Companion.amountOf(stack) == 0, "The last bucket is consumed");
        h.assertTrue(InjectableEndRodItem.Companion.fluidOf(stack) == null, "An empty rod forgets the fluid type");

        // 空棒不计时。
        rod().onUse(stack, h.getLevel(), player, 0, true, 1);
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 0, "An empty rod never charges progress");
        h.succeed();
    }

    /** 只有副手（或真正插入）才触发；背包里、主手里的末地烛都不自我使用。 */
    @GameTest
    public void onlyOffhandOrInsertedRodsSelfUse(GameTestHelper h) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(h);
        player.getAbilities().instabuild = false;
        InjectableEndRodItem rod = rod();

        ItemStack stack = new ItemStack(rod);
        stack.set(JRComponents.Companion.getINJECTABLE_FLUID(), InjectableEndRodItem.WATER);
        stack.set(JRComponents.Companion.getINJECTABLE_FLUID_AMOUNT(), 1000);

        // 主背包（槽 9 起不是快捷栏）里躺着、主副手都空：不触发。
        player.getInventory().setItem(9, stack);
        stack.getItem().inventoryTick(stack, h.getLevel(), player, null);
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 0,
                "A rod sitting in the inventory must not self-use, progress=" + InjectableEndRodItem.Companion.progressOf(stack));

        // 主手：也不触发。
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().inventoryTick(stack, h.getLevel(), player, null);
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 0,
                "A main-hand rod must not self-use, progress=" + InjectableEndRodItem.Companion.progressOf(stack));

        // 副手：触发。
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.getInventory().setItem(9, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        stack.getItem().inventoryTick(stack, h.getLevel(), player, null);
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 1,
                "The offhand rod self-uses, progress=" + InjectableEndRodItem.Companion.progressOf(stack));

        // 插入态（不在背包里）仍由 tickInserted 驱动。
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        rod.tickInserted(stack, h.getLevel(), player);
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 2,
                "Inserted rods still tick through tickInserted, progress=" + InjectableEndRodItem.Companion.progressOf(stack));
        h.succeed();
    }

    /** 没装流体时：不涨进度、不触发高潮效果、也永远不会掉耐久。 */
    @GameTest
    public void noFluidMeansNoInsertionEffects(GameTestHelper h) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(h);
        player.getAbilities().instabuild = false;
        ItemStack stack = new ItemStack(rod());
        h.assertFalse(stack.isDamageableItem(), "The injectable rod has no durability");

        // 放进副手，但不装任何流体。
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        for (int i = 0; i < 50; i++) {
            stack.getItem().inventoryTick(stack, h.getLevel(), player, null);
        }
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 0,
                "No fluid means no progress, progress=" + InjectableEndRodItem.Companion.progressOf(stack));
        h.assertFalse(player.hasEffect(JREffects.Companion.getORGASM_EFFECT()),
                "No fluid means the insertion effects never fire");
        h.assertTrue(stack.getDamageValue() == 0, "The rod never takes damage, damage=" + stack.getDamageValue());

        // 装上流体后同样的 tick 就会开始生效（且仍然不掉耐久）。
        stack.set(JRComponents.Companion.getINJECTABLE_FLUID(), InjectableEndRodItem.WATER);
        stack.set(JRComponents.Companion.getINJECTABLE_FLUID_AMOUNT(), 1000);
        stack.getItem().inventoryTick(stack, h.getLevel(), player, null);
        h.assertTrue(InjectableEndRodItem.Companion.progressOf(stack) == 1, "Fluid makes the rod start charging");
        h.assertTrue(player.hasEffect(JREffects.Companion.getORGASM_EFFECT()), "The normal insertion effects still apply");
        h.assertTrue(stack.getDamageValue() == 0, "Still no durability loss");
        h.succeed();
    }
}
