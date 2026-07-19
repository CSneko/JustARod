package org.cneko.justarod.entity

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.cneko.toneko.common.mod.api.NekoSkinRegistry
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.entities.ToNekoEntities
import org.spongepowered.asm.mixin.Unique

/*
萝莉控别看了，对，说的就是你！变态一只！
 */

/*
我才不是呢~
 */
class LoliNekoEntity(private val type: EntityType<LoliNekoEntity>, world: Level): NekoEntity(type, world) {
    companion object{
        val SHOWING_AGE: EntityDataAccessor<Int?> = SynchedEntityData.defineId(LoliNekoEntity::class.java, EntityDataSerializers.INT)
    }
    override fun getBreedOffspring(
        p0: ServerLevel?,
        p1: INeko?
    ): NekoEntity? {
        return p0?.let { LoliNekoEntity(type, it) }
    }
    fun getShowingAge(): Int {
        return entityData.get(SHOWING_AGE)!!
    }
    fun setShowingAge(age: Int) {
        entityData.set(SHOWING_AGE, age)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(SHOWING_AGE,18)
    }
    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putInt("showing_age", this.getShowingAge())
    }
    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("showing_age")) {
            this.setShowingAge(compound.getInt("showing_age"))
        }else {
            this.setShowingAge(random.nextInt(1000)+18)
        }
    }

    override fun getAge(): Int {
        return -1
    }

    override fun getRandomSkin(): String? {
        return NekoSkinRegistry.getRandomSkin(ToNekoEntities.ADVENTURER_NEKO);
    }
}