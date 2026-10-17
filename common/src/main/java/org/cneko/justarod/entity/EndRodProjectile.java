package org.cneko.justarod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.damage.JRDamageTypes;

/** A real, non-recoverable projectile; gathering rods do not damage anything until released. */
public final class EndRodProjectile extends Projectile implements ItemSupplier {
    public static final double SPEED = 3.2;
    public static final float DAMAGE = 0.2f;
    private static final int LIFETIME = 240;
    private int flightTicks;
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(EndRodProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(EndRodProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GATHERING = SynchedEntityData.defineId(EndRodProjectile.class, EntityDataSerializers.BOOLEAN);

    public EndRodProjectile(EntityType<? extends EndRodProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET, -1);
        builder.define(SLOT, 0);
        builder.define(GATHERING, false);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.END_ROD);
    }

    public void gatherAround(LivingEntity target, int slot) {
        entityData.set(TARGET, target.getId());
        entityData.set(SLOT, slot);
        entityData.set(GATHERING, true);
    }

    public boolean isGathering() {
        return entityData.get(GATHERING);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Entity's default scales by the tiny collision box and hides rods just ten blocks away.
        return distance < 96.0 * 96.0;
    }

    public void launchAtTarget() {
        entityData.set(GATHERING, false);
        flightTicks = 0;
        needsSync = true;
    }

    private LivingEntity target() {
        Entity target = level().getEntity(entityData.get(TARGET));
        return target instanceof LivingEntity living && living.isAlive() && !living.isSpectator() ? living : null;
    }

    public Vec3 gatheringPosition(LivingEntity target) {
        double angle = entityData.get(SLOT) * (Math.PI * 2 / 20);
        return new Vec3(target.getX() + Math.cos(angle) * 2.0, target.getEyeY() + 3.0,
                target.getZ() + Math.sin(angle) * 2.0);
    }

    public boolean isReady() {
        LivingEntity target = target();
        return isAlive() && target != null && position().distanceToSqr(gatheringPosition(target)) < 0.16;
    }

    @Override
    public void tick() {
        super.tick();
        // Waiting in a focus ring is not flight time; a paused batch must not expire mid-gathering.
        if (!level().isClientSide() && !isGathering() && ++flightTicks > LIFETIME) {
            discard();
            return;
        }
        LivingEntity target = target();
        if (entityData.get(TARGET) != -1) {
            if (target == null) {
                if (!level().isClientSide()) discard();
                return;
            }
            Vec3 destination = isGathering() ? gatheringPosition(target) : target.getBoundingBox().getCenter();
            Vec3 direction = destination.subtract(position());
            setDeltaMovement(direction.normalize().scale(Math.min(SPEED, direction.length())));
        }
        Vec3 movement = getDeltaMovement();
        if (isGathering()) {
            setPos(position().add(movement));
            // Once in position, point down towards the locked target, ready to strike.
            Vec3 facing = target.getBoundingBox().getCenter().subtract(position());
            setYRot((float) Math.toDegrees(Math.atan2(facing.x, facing.z)));
            setXRot((float) Math.toDegrees(Math.atan2(facing.y, facing.horizontalDistance())));
        } else {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            setPos(hit.getType() == HitResult.Type.MISS ? position().add(movement) : hit.getLocation());
            updateRotation();
            if (!level().isClientSide() && hit.getType() != HitResult.Type.MISS) onHit(hit);
        }
        if (level().isClientSide() && tickCount % 4 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == getOwner() || entity instanceof EndRodProjectile) return false;
        if (getOwner() instanceof Player owner && entity instanceof Player victim
                && (!owner.canHarmPlayer(victim) || (level() instanceof ServerLevel server
                && !server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP)))) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult hit) {
        // Do not call Projectile.onHitBlock: this novelty gun must not activate target blocks etc.
        if (hit instanceof EntityHitResult entityHit) onHitEntity(entityHit);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel server && hit.getEntity() instanceof LivingEntity victim) {
            var type = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getOrThrow(JRDamageTypes.END_ROD_GUN);
            victim.hurtServer(server, new DamageSource(type, this, getOwner()), DAMAGE);
            // Exactly one heart, also visible to the shooter at the full focus-lock distance.
            server.sendParticles(ParticleTypes.HEART, true, true, victim.getX(), victim.getEyeY() + 0.2, victim.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
