package org.cneko.justarod.disassembly;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

/** A skin-bearing prop, worker and occasional runaway. Work orders do not survive world reloads. */
public final class DetachedPartEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(DetachedPartEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> TOOL = SynchedEntityData.defineId(DetachedPartEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(DetachedPartEntity.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, WORK = 1, STRIKE = 2, FLEE = 3, DELIVER = 4;
    private final ArrayDeque<BlockPos> work = new ArrayDeque<>();
    private int fatigue, speechUntil, nextHop;
    private float mining;
    private Vec3 escape = Vec3.ZERO;
    public DetachedPartEntity(EntityType<? extends DetachedPartEntity> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(STACK, ItemStack.EMPTY); b.define(TOOL, ItemStack.EMPTY); b.define(MODE, IDLE); }
    public ItemStack stack() { return entityData.get(STACK); }
    public void setStack(ItemStack stack) { entityData.set(STACK, stack.copyWithCount(1)); refreshDimensions(); }
    @Override public EntityDimensions getDimensions(Pose pose) {
        BodyPart part = part();
        return part.eye() ? EntityDimensions.fixed(0.18f, 0.18f).withEyeHeight(0.1f)
                : part == BodyPart.HEAD ? EntityDimensions.fixed(0.5f, 0.5f).withEyeHeight(0.3f)
                : EntityDimensions.fixed(0.25f, 0.75f).withEyeHeight(0.5f);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(STACK)) refreshDimensions();
    }
    public ItemStack tool() { return entityData.get(TOOL); }
    public int mode() { return entityData.get(MODE); }
    public BodyPart part() { return stack().getItem() instanceof DetachedPartItem item ? item.part : BodyPart.HEAD; }
    public ServerPlayer owner() {
        var id = DetachedPartItem.owner(stack());
        return id != null && level() instanceof ServerLevel s ? s.getServer().getPlayerList().getPlayer(id) : null;
    }
    @Override public boolean isPickable() { return true; }
    @Override public ItemStack getPickResult() { return stack().copy(); }
    public void say(Component line) { setCustomName(line); setCustomNameVisible(true); speechUntil = tickCount + 100; }
    public void stop() {
        if (level() instanceof ServerLevel s && !work.isEmpty()) s.destroyBlockProgress(getId(), work.peek(), -1);
        work.clear(); mining = 0; entityData.set(MODE, IDLE); fatigue = 0;
    }
    public void strike() { stop(); entityData.set(MODE, STRIKE); say(Component.translatable("disassembly.justarod.strike")); }
    public void flee() {
        stop(); entityData.set(MODE, FLEE);
        var p = owner(); var direction = p == null ? new Vec3(1, 0, 0) : position().subtract(p.position()).multiply(1,0,1);
        if (direction.lengthSqr() < 0.1) direction = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
        escape = direction.normalize().yRot((random.nextFloat() - 0.5f) * 2.0f);
        say(Component.translatable(part() == BodyPart.HEAD ? "disassembly.justarod.head_run" : "disassembly.justarod.run"));
    }
    public void mine(BlockPos target, net.minecraft.core.Direction face) {
        stop();
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++)
            work.add(switch (face.getAxis()) {
                case X -> target.offset(0, a, b); case Y -> target.offset(a, 0, b); case Z -> target.offset(a, b, 0);
            });
        entityData.set(MODE, WORK); say(Component.translatable("disassembly.justarod.working"));
    }
    public void releaseEquipment(ServerLevel level) {
        if (!tool().isEmpty()) { spawnAtLocation(level, tool().copy()); entityData.set(TOOL, ItemStack.EMPTY); }
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSpectator()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        var server = (ServerLevel) level();
        if (held.isEmpty() && player.isShiftKeyDown()) {
            stop(); releaseEquipment(server);
            var result = stack().copy();
            if (!player.getInventory().add(result)) player.drop(result, false);
            discard(); return InteractionResult.SUCCESS;
        }
        if (!player.getUUID().equals(DetachedPartItem.owner(stack()))) return InteractionResult.PASS;
        if ((part().arm() || part().leg()) && !held.isEmpty()) {
            if (held.getItem() instanceof DetachedPartItem || (part().arm() && !held.has(net.minecraft.core.component.DataComponents.TOOL))) return InteractionResult.PASS;
            stop(); releaseEquipment(server);
            entityData.set(TOOL, held.split(part().arm() ? 1 : held.getCount()));
            if (part().leg()) entityData.set(MODE, DELIVER);
        } else if (held.isEmpty() && !tool().isEmpty()) { stop(); releaseEquipment(server); }
        else if (part().camera() && player instanceof ServerPlayer p) DisassemblyController.view(p, this);
        return InteractionResult.SUCCESS;
    }
    @Override public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof ServerPlayer p && p.getUUID().equals(DetachedPartItem.owner(stack()))) {
            if (p.isShiftKeyDown()) {
                var copy = stack().copy();
                if (DisassemblyController.attach(p, copy)) { releaseEquipment(p.level()); discard(); }
            } else if (mode() == FLEE || mode() == STRIKE) { stop(); say(Component.translatable("disassembly.justarod.caught")); }
            else if (part() == BodyPart.HEAD) say(Component.translatable("disassembly.justarod.head_talk"));
        }
        return true;
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player || isInvulnerableToBase(source)) return false;
        stop(); releaseEquipment(level); spawnAtLocation(level, stack().copy()); discard(); return true;
    }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (tickCount > speechUntil && isCustomNameVisible()) setCustomNameVisible(false);
            ServerPlayer p = owner();
            if (p != null && !DisassemblyController.valid(stack(), level.getServer())) { stop(); releaseEquipment(level); discard(); return; }
            if (p != null && p.isAlive() && p.level() == level) {
                if (mode() == WORK) tickMining(level, p);
                if (mode() == DELIVER) {
                    hopToward(p.position(), 0.22);
                    if (distanceToSqr(p) < 2.5) {
                        var cargo = tool().copy();
                        if (!p.getInventory().add(cargo)) p.drop(cargo, false);
                        entityData.set(TOOL, ItemStack.EMPTY); stop(); say(Component.translatable("disassembly.justarod.delivered"));
                    }
                }
                if (mode() == FLEE) {
                    // Stay within the owner's loaded surroundings so the chase remains playable.
                    if (distanceToSqr(p) > 28 * 28) escape = p.position().subtract(position()).multiply(1,0,1).normalize();
                    hopToward(position().add(escape.scale(5)), part().leg() ? 0.29 : 0.19);
                    if (horizontalCollision) escape = new Vec3(-escape.z, 0, escape.x);
                }
                if (part() == BodyPart.HEAD && mode() == IDLE && tickCount % 10 == 0) {
                    Player nearest = level.getNearestPlayer(this, 6);
                    if (nearest != null) { var d = nearest.position().subtract(position()); setYRot((float)Math.toDegrees(Math.atan2(d.z, d.x)) - 90); }
                }
                if (DisassemblyState.of(p).random) {
                    fatigue += mode() == WORK ? 1 : (tickCount % 4 == 0 ? 1 : 0);
                    if (fatigue == 1200 && mode() != FLEE) say(Component.translatable("disassembly.justarod.tired"));
                    if (fatigue >= 1800 && mode() != FLEE) {
                        if (mode() == STRIKE) flee(); else { strike(); fatigue = 1600; }
                    }
                }
            } else if (mode() == WORK && !work.isEmpty()) level.destroyBlockProgress(getId(), work.peek(), -1);
        }
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(onGround() ? 0.65 : 0.95, onGround() ? 0 : 0.98, onGround() ? 0.65 : 0.95));
    }
    private void hopToward(Vec3 target, double speed) {
        if (tickCount < nextHop || !onGround()) return;
        var d = target.subtract(position()).multiply(1,0,1).normalize();
        setDeltaMovement(d.scale(speed).add(0, 0.28, 0)); nextHop = tickCount + 8;
        setYRot((float)Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
    }
    private void tickMining(ServerLevel level, ServerPlayer p) {
        if (work.isEmpty() || tool().isEmpty() || p.isSpectator()) { stop(); return; }
        BlockPos target = work.peek();
        if (!level.hasChunkAt(target) || p.distanceToSqr(Vec3.atCenterOf(target)) > 16 * 16) { stop(); return; }
        if (position().distanceToSqr(Vec3.atCenterOf(target)) > 6.25) { hopToward(Vec3.atCenterOf(target), 0.16); return; }
        var state = level.getBlockState(target);
        if (state.isAir() || state.getDestroySpeed(level, target) < 0 || state.hasBlockEntity() || !level.mayInteract(p, target)) { nextBlock(level); return; }
        var original = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, tool());
        try {
            mining += state.getDestroyProgress(p, level, target);
            level.destroyBlockProgress(getId(), target, Math.min(9, (int)(mining * 10)));
            if (mining >= 1) {
                p.gameMode.destroyBlock(target);
                // Workers collect their output near the mined block and return it to the owner.
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(target).inflate(0.7))) {
                    var drop = item.getItem();
                    if (p.getInventory().add(drop)) item.discard();
                }
                nextBlock(level);
            }
        } finally {
            entityData.set(TOOL, p.getMainHandItem().copy());
            p.setItemInHand(InteractionHand.MAIN_HAND, original);
        }
    }
    private void nextBlock(ServerLevel level) { level.destroyBlockProgress(getId(), work.remove(), -1); mining = 0; }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        output.store("Part", ItemStack.CODEC, stack()); output.store("Equipment", ItemStack.OPTIONAL_CODEC, tool()); output.putInt("Fatigue", fatigue);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        setStack(input.read("Part", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        entityData.set(TOOL, input.read("Equipment", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        fatigue = input.getIntOr("Fatigue", 0);
    }
}
