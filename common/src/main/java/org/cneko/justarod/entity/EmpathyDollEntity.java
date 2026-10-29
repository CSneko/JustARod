package org.cneko.justarod.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.empathy.EmpathyDollEvents;
import org.cneko.justarod.empathy.EmpathyReaction;
import org.cneko.justarod.item.JRItems;

/** A small persistent decoration; ordinary attacks squeeze rather than damage it. */
public final class EmpathyDollEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(EmpathyDollEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Long> SQUEEZE_AT = SynchedEntityData.defineId(EmpathyDollEntity.class, EntityDataSerializers.LONG);

    public EmpathyDollEntity(EntityType<? extends EmpathyDollEntity> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STACK, ItemStack.EMPTY);
        builder.define(SQUEEZE_AT, -1L);
    }

    public ItemStack getDollStack() {
        ItemStack stack = entityData.get(STACK);
        return stack.isEmpty() ? new ItemStack(JRItems.Companion.getEMPATHY_DOLL()) : stack.copy();
    }

    public void setDollStack(ItemStack stack) { entityData.set(STACK, stack.copyWithCount(1)); }
    public void squeeze() { entityData.set(SQUEEZE_AT, level().getGameTime()); }
    public float squeezeHeight(float partialTick) {
        long start = entityData.get(SQUEEZE_AT);
        return start < 0 ? 1 : EmpathyReaction.squeezeHeight(level().getGameTime() - start + partialTick);
    }

    @Override public boolean isPickable() { return true; }
    @Override public ItemStack getPickResult() { return getDollStack(); }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSpectator()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (level() instanceof ServerLevel) {
                ItemStack stack = getDollStack();
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                discard();
            }
            return InteractionResult.SUCCESS;
        }
        var reaction = EmpathyReaction.forItem(held);
        if (reaction == null) return InteractionResult.PASS;
        if (!level().isClientSide()) EmpathyDollEvents.trigger(this, player, reaction);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!(attacker instanceof Player player)) return true;
        if (!player.isSpectator() && player.getMainHandItem().isEmpty() && !level().isClientSide())
            EmpathyDollEvents.trigger(this, player, EmpathyReaction.SQUEEZE);
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source)) return false;
        // Environmental destruction recovers the doll and its binding; player punches are handled above.
        if (source.getEntity() instanceof Player) return false;
        spawnAtLocation(level, getDollStack());
        discard();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.8, onGround() ? 0 : 0.98, 0.8));
    }

    @Override protected void readAdditionalSaveData(ValueInput input) {
        setDollStack(input.read("doll", ItemStack.CODEC).orElseGet(() -> new ItemStack(JRItems.Companion.getEMPATHY_DOLL())));
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) { output.store("doll", ItemStack.CODEC, getDollStack()); }
}
