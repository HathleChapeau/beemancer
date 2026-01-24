/**
 * ============================================================
 * [HiveBeeSlot.java]
 * Description: Données d'un slot d'abeille dans une ruche
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune externe)    |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlockEntity.java: Gestion des slots d'abeilles
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Encapsule toutes les données d'un slot d'abeille dans une ruche.
 */
public class HiveBeeSlot {

    public enum State { EMPTY, INSIDE, OUTSIDE }

    private State state = State.EMPTY;
    @Nullable private UUID beeUUID = null;
    private int cooldown = 0;
    private boolean needsHealing = false;
    private float currentHealth = 0;
    private float maxHealth = 10;
    @Nullable private BlockPos assignedFlower = null;

    // --- State Management ---

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isEmpty() {
        return state == State.EMPTY;
    }

    public boolean isInside() {
        return state == State.INSIDE;
    }

    public boolean isOutside() {
        return state == State.OUTSIDE;
    }

    // --- UUID ---

    @Nullable
    public UUID getBeeUUID() {
        return beeUUID;
    }

    public void setBeeUUID(@Nullable UUID uuid) {
        this.beeUUID = uuid;
    }

    // --- Cooldown ---

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public void decrementCooldown() {
        if (cooldown > 0) cooldown--;
    }

    public boolean isCooldownComplete() {
        return cooldown <= 0;
    }

    // --- Health ---

    public boolean needsHealing() {
        return needsHealing;
    }

    public void setNeedsHealing(boolean needsHealing) {
        this.needsHealing = needsHealing;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(float health) {
        this.currentHealth = health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void heal(float amount) {
        currentHealth = Math.min(currentHealth + amount, maxHealth);
        if (currentHealth >= maxHealth) {
            needsHealing = false;
        }
    }

    // --- Assigned Flower ---

    @Nullable
    public BlockPos getAssignedFlower() {
        return assignedFlower;
    }

    public void setAssignedFlower(@Nullable BlockPos flower) {
        this.assignedFlower = flower;
    }

    public boolean hasAssignedFlower() {
        return assignedFlower != null;
    }

    public void clearAssignedFlower() {
        this.assignedFlower = null;
    }

    // --- Reset ---

    public void clear() {
        state = State.EMPTY;
        beeUUID = null;
        cooldown = 0;
        needsHealing = false;
        currentHealth = 0;
        maxHealth = 10;
        assignedFlower = null;
    }

    // --- NBT ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("State", state.ordinal());
        if (beeUUID != null) {
            tag.putUUID("UUID", beeUUID);
        }
        tag.putInt("Cooldown", cooldown);
        tag.putBoolean("NeedsHealing", needsHealing);
        tag.putFloat("CurrentHealth", currentHealth);
        tag.putFloat("MaxHealth", maxHealth);
        if (assignedFlower != null) {
            tag.putInt("FlowerX", assignedFlower.getX());
            tag.putInt("FlowerY", assignedFlower.getY());
            tag.putInt("FlowerZ", assignedFlower.getZ());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        state = State.values()[tag.getInt("State")];
        beeUUID = tag.hasUUID("UUID") ? tag.getUUID("UUID") : null;
        cooldown = tag.getInt("Cooldown");
        needsHealing = tag.getBoolean("NeedsHealing");
        currentHealth = tag.getFloat("CurrentHealth");
        maxHealth = tag.getFloat("MaxHealth");
        if (tag.contains("FlowerX")) {
            assignedFlower = new BlockPos(
                    tag.getInt("FlowerX"),
                    tag.getInt("FlowerY"),
                    tag.getInt("FlowerZ")
            );
        } else {
            assignedFlower = null;
        }
    }
}
