package org.cneko.justarod.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.disassembly.*;
import org.cneko.justarod.entity.JREntities;
import java.util.UUID;

/** Core gameplay checks for prop round-trips, a survival worker and eye handoffs. */
public class DisassemblyGameTests {
    private ServerPlayer player(GameTestHelper h) {
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"PartsTest"),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        var pos=h.absoluteVec(new Vec3(3.5,12,3.5)); p.teleportTo(pos.x,pos.y,pos.z); p.setNoGravity(true); p.setGameMode(GameType.SURVIVAL);
        DisassemblyState.of(p).random=false;
        return p;
    }
    @GameTest
    public void headCanBePlacedSavedPickedUpAndAttached(GameTestHelper h) {
        var p=player(h);
        h.assertTrue(DisassemblyController.detach(p,BodyPart.HEAD),"Head detaches");
        var carried=p.getMainHandItem();
        h.assertTrue(p.getUUID().equals(DetachedPartItem.owner(carried)),"Prop keeps the player's skin owner");
        var e=new DetachedPartEntity(JREntities.DETACHED_PART,h.getLevel());
        e.setStack(carried); e.setPos(p.position().add(0,0,1)); e.setNoGravity(true); h.getLevel().addFreshEntity(e); carried.shrink(1);
        var data=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess()); e.saveWithoutId(data);
        var restored=new DetachedPartEntity(JREntities.DETACHED_PART,h.getLevel());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),data.buildResult()));
        h.assertTrue(restored.stack().get(DataComponents.PROFILE).equals(e.stack().get(DataComponents.PROFILE)),"World save preserves the complete skin profile");
        var body=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess()); DisassemblyState.of(p).save(body);
        var state=new DisassemblyState(); state.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),body.buildResult()));
        h.assertTrue(state.detached.equals(DisassemblyState.of(p).detached),"Player save preserves the detached slot identity");
        p.setShiftKeyDown(true); e.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);
        h.assertTrue(e.isRemoved(),"A world prop can be picked up");
        h.assertTrue(DisassemblyController.attach(p,p.getMainHandItem()),"The picked-up head reattaches");
        h.assertTrue(!DisassemblyState.of(p).missing(BodyPart.HEAD),"The body has its head again");
        h.succeed();
    }
    @GameTest(maxTicks=160)
    public void handMinesWithRealToolWearAndRunawaysCanBeRecalled(GameTestHelper h) {
        var p=player(h); DisassemblyController.detach(p,BodyPart.LEFT_ARM);
        var hand=new DetachedPartEntity(JREntities.DETACHED_PART,h.getLevel());
        hand.setStack(p.getMainHandItem()); p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        var target=h.absolutePos(new BlockPos(4,12,4));
        h.getLevel().setBlockAndUpdate(target,Blocks.STONE.defaultBlockState());
        hand.setPos(Vec3.atCenterOf(target).add(-1,0,0)); hand.setNoGravity(true); h.getLevel().addFreshEntity(hand);
        hand.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);
        hand.mine(target,Direction.NORTH);
        h.runAfterDelay(100,()-> {
            h.assertTrue(h.getLevel().getBlockState(target).isAir(),"The worker really mines stone");
            h.assertTrue(hand.tool().getDamageValue()>0,"Mining consumes its equipped pickaxe durability");
            h.assertTrue(p.getInventory().contains(s->s.is(Items.COBBLESTONE)),"The owner receives the mined drops");
            hand.flee(); h.assertTrue(hand.mode()==DetachedPartEntity.FLEE,"Filming can force a runaway");
            DisassemblyController.recall(p);
            h.assertTrue(hand.mode()==DetachedPartEntity.IDLE && hand.distanceToSqr(p)<9,"Recall stops and returns the hand");
            h.succeed();
        });
    }
    @GameTest
    public void cameraFollowsHandPocketAndInstalledEye(GameTestHelper h) {
        var owner=player(h); var carrier=player(h);
        DisassemblyController.detach(owner,BodyPart.LEFT_EYE);
        var eye=owner.getMainHandItem(); owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        carrier.setItemInHand(InteractionHand.MAIN_HAND,eye);
        DisassemblyVision.start(owner,BodyPart.LEFT_EYE,false);
        h.assertTrue(DisassemblyState.of(owner).lastView.target()==carrier.getId(),"The camera finds its carrier after a handoff");
        carrier.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        carrier.getInventory().setItem(9,eye); carrier.getInventory().setItem(10,new ItemStack(Items.DIAMOND,7));
        DisassemblyVision.update(owner);
        var pocket=DisassemblyState.of(owner).lastView;
        h.assertTrue(pocket.mode()==org.cneko.justarod.packet.DisassemblyViewPayload.INVENTORY && pocket.contents().get(10).getCount()==7,
                "A pocket view contains the actual carrier inventory and counts");
        carrier.getInventory().setItem(9,ItemStack.EMPTY);
        owner.setItemInHand(InteractionHand.MAIN_HAND,eye);
        DisassemblyController.exitView(owner);
        h.assertTrue(DisassemblyVision.install(owner,carrier,eye),"The owner can mount their detached eye on another player");
        var mounted=DisassemblyState.of(owner).lastView;
        h.assertTrue(mounted.mode()==org.cneko.justarod.packet.DisassemblyViewPayload.MOUNTED && mounted.dual(),"Mounting opens the host's live dual view");
        var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        DisassemblyState.of(carrier).save(save);
        var restored=new DisassemblyState(); restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),save.buildResult()));
        h.assertTrue(restored.installedEyes.size()==1,"Mounted eyes survive saving the host");
        h.assertTrue(DisassemblyVision.remove(carrier,carrier),"The host can take off the foreign eye");
        DisassemblyVision.update(owner);
        h.assertTrue(DisassemblyState.of(owner).lastView.mode()!=org.cneko.justarod.packet.DisassemblyViewPayload.MOUNTED,"The view follows the removed eye back to the host's hand");
        h.succeed();
    }

}
