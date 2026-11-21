package org.cneko.justarod.entity;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.constant.DefaultAnimations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;

/** An adult owner NPC with deliberately small, predictable behavior for play and filming. */
public final class PetOwnerNekoEntity extends NekoEntity {
    public PetOwnerNekoEntity(EntityType<? extends NekoEntity> type, Level level) { super(type, level); }
    @Override public void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }
    @Override public String getDefaultSkin() { return "shiuri_neko"; }
    @Override public String getRandomSkin() { return getDefaultSkin(); }
    @Override public boolean canMate(INeko other) { return false; }
    @Override public NekoEntity getBreedOffspring(ServerLevel level, INeko other) { return null; }
    @Override public void slowTick() { /* Interaction is handled by the shared PetController. */ }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PetOwnerNekoEntity>("main", 5,
                state -> state.setAndContinue(state.isMoving() ? DefaultAnimations.WALK : DefaultAnimations.IDLE)));
    }
}
