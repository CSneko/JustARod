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
        val SHOWING_AGE: EntityDataAccessor<Int> = SynchedEntityData.defineId(LoliNekoEntity::class.java, EntityDataSerializers.INT)
    }
    override fun getBreedOffspring(
        p0: ServerLevel,
        p1: INeko
    ): NekoEntity? {
        return LoliNekoEntity(type, p0)
    }
    fun getShowingAge(): Int {
        return entityData.get(SHOWING_AGE)
    }
    fun setShowingAge(age: Int) {
        entityData.set(SHOWING_AGE, age)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(SHOWING_AGE,18)
    }
    // 26.x：实体存档改为 ValueInput/ValueOutput
    override fun addAdditionalSaveData(out: net.minecraft.world.level.storage.ValueOutput) {
        super.addAdditionalSaveData(out)
        out.putInt("showing_age", this.getShowingAge())
    }
    override fun readAdditionalSaveData(input: net.minecraft.world.level.storage.ValueInput) {
        super.readAdditionalSaveData(input)
        this.setShowingAge(input.getInt("showing_age").orElseGet { random.nextInt(1000)+18 })
    }

    override fun getAge(): Int {
        return -1
    }

    override fun getRandomSkin(): String? {
        return NekoSkinRegistry.getRandomSkin(ToNekoEntities.ADVENTURER_NEKO);
    }
}