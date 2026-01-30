/**
 * ============================================================
 * [DeliveryBeeEntity.java]
 * Description: Abeille visuelle de livraison pour le réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | Bee                           | Entité parent        | Skin vanilla                   |
 * | DeliveryTask                  | Tâche assignée       | Navigation et logique          |
 * | StorageControllerBlockEntity  | Controller parent    | Extraction/dépôt items         |
 * | DeliveryPhaseGoal             | AI unique            | Vol → attente → retour         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (spawn)
 * - ClientSetup.java (renderer = vanilla BeeRenderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.delivery;

import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Abeille de livraison pour le réseau de stockage.
 * Visuelle uniquement: skin de Bee vanilla, pas de collision, pas de gravité,
 * pas de drops, pas de dégâts. Suit un pathfinding volant entre le controller
 * et les coffres du réseau.
 *
 * Timeout: se discard après 2400 ticks (2 min).
 * Vérifie chaque tick que le controller est encore formé.
 */
public class DeliveryBeeEntity extends Bee {

    private static final int TIMEOUT_TICKS = 2400;

    private BlockPos controllerPos;
    private BlockPos targetPos;
    private BlockPos returnPos;
    private BlockPos terminalPos;
    private ItemStack template = ItemStack.EMPTY;
    private int requestCount;
    private ItemStack carriedItems = ItemStack.EMPTY;
    private DeliveryTask.DeliveryType deliveryType;
    private float flySpeedMultiplier = 1.0f;
    private float searchSpeedMultiplier = 1.0f;
    private int ticksAlive = 0;

    public DeliveryBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        // Supprime TOUS les goals vanilla. Seul DeliveryPhaseGoal est ajouté après spawn.
    }

    /**
     * Initialise la tâche de livraison après le spawn.
     * Doit être appelé immédiatement après avoir spawn l'entité.
     */
    public void initDeliveryTask(BlockPos controllerPos, BlockPos targetPos, BlockPos returnPos,
                                  BlockPos terminalPos, ItemStack template, int requestCount,
                                  DeliveryTask.DeliveryType type, ItemStack carriedItems,
                                  float flySpeedMultiplier, float searchSpeedMultiplier) {
        this.controllerPos = controllerPos;
        this.targetPos = targetPos;
        this.returnPos = returnPos;
        this.terminalPos = terminalPos;
        this.template = template.copy();
        this.requestCount = requestCount;
        this.deliveryType = type;
        this.carriedItems = carriedItems.copy();
        this.flySpeedMultiplier = flySpeedMultiplier;
        this.searchSpeedMultiplier = searchSpeedMultiplier;

        this.goalSelector.addGoal(0, new DeliveryPhaseGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        if (!level().isClientSide()) {
            if (ticksAlive > TIMEOUT_TICKS) {
                discard();
                return;
            }
            if (!isControllerValid()) {
                discard();
            }
        }
    }

    private boolean isControllerValid() {
        if (controllerPos == null || level() == null) return false;
        if (!level().isLoaded(controllerPos)) return true;
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            return controller.isFormed();
        }
        return false;
    }

    // === Pas de collision, pas de drops, invincible ===

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isInvulnerable() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    // === Getters ===

    public BlockPos getControllerPos() { return controllerPos; }
    public BlockPos getTargetPos() { return targetPos; }
    public BlockPos getReturnPos() { return returnPos; }
    public BlockPos getTerminalPos() { return terminalPos; }
    public ItemStack getTemplate() { return template; }
    public int getRequestCount() { return requestCount; }
    public DeliveryTask.DeliveryType getDeliveryType() { return deliveryType; }
    public ItemStack getCarriedItems() { return carriedItems; }
    public float getFlySpeedMultiplier() { return flySpeedMultiplier; }
    public float getSearchSpeedMultiplier() { return searchSpeedMultiplier; }

    public void setCarriedItems(ItemStack items) {
        this.carriedItems = items.copy();
    }

    // === NBT (pas besoin de sauvegarder, les bees sont éphémères) ===
    // Au rechargement, les tâches actives sont remises en queue par le controller.

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Volontairement minimal: au rechargement, l'abeille est discard
        // et la tâche est remise en queue
        tag.putBoolean("IsDeliveryBee", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Au rechargement, discard l'abeille (tâche remise en queue par controller)
        if (tag.getBoolean("IsDeliveryBee")) {
            this.discard();
        }
    }
}
