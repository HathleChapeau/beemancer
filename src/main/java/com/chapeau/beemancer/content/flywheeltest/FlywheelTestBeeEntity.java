/**
 * ============================================================
 * [FlywheelTestBeeEntity.java]
 * Description: Entite test legere pour valider le rendu GPU Flywheel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Entity              | Base entite          | Entite sans AI ni pathfinding   |
 * | BeeSpeciesManager   | Especes disponibles  | Texture par espece              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeSpawnerBlock.java (creation des entites)
 * - FlywheelTestBeeVisual.java (rendu GPU)
 * - BeemancerEntities.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.content.flywheeltest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class FlywheelTestBeeEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_SPECIES =
        SynchedEntityData.defineId(FlywheelTestBeeEntity.class, EntityDataSerializers.STRING);

    private double startY;

    public FlywheelTestBeeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SPECIES, "meadow");
    }

    public String getSpeciesId() {
        return this.entityData.get(DATA_SPECIES);
    }

    public void setSpeciesId(String speciesId) {
        this.entityData.set(DATA_SPECIES, speciesId);
    }

    public void setStartY(double y) {
        this.startY = y;
    }

    @Override
    public void tick() {
        super.tick();
        float bob = Mth.sin(this.tickCount * 0.1f) * 0.15f;
        this.setPos(this.getX(), this.startY + bob, this.getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("SpeciesId")) {
            setSpeciesId(tag.getString("SpeciesId"));
        }
        this.startY = tag.getDouble("StartY");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("SpeciesId", getSpeciesId());
        tag.putDouble("StartY", this.startY);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
