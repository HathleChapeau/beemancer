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
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Abeille de livraison pour le réseau de stockage.
 * Purement visuelle: skin de Bee vanilla avec pathfinding volant.
 * Tous les comportements vanilla (anger, sting, pollination, hive, breeding,
 * sons, drops, interactions) sont neutralisés.
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
    private UUID taskId;
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
        // Appeler super pour initialiser les champs internes de Bee (beePollinateGoal, etc.)
        // requis par BeeLookControl, puis vider les goal selectors.
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
    }

    /**
     * Initialise la tâche de livraison après le spawn.
     * Doit être appelé immédiatement après avoir spawn l'entité.
     */
    public void initDeliveryTask(BlockPos controllerPos, BlockPos targetPos, BlockPos returnPos,
                                  BlockPos terminalPos, ItemStack template, int requestCount,
                                  DeliveryTask.DeliveryType type, ItemStack carriedItems,
                                  float flySpeedMultiplier, float searchSpeedMultiplier,
                                  UUID taskId) {
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
        this.taskId = taskId;

        this.goalSelector.addGoal(0, new DeliveryPhaseGoal(this));
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        ticksAlive++;

        if (!level().isClientSide()) {
            if (ticksAlive > TIMEOUT_TICKS) {
                notifyTaskFailed();
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

    // =========================================================================
    // INVULNÉRABILITÉ — Aucun dégât, aucune mort, aucun loot
    // =========================================================================

    @Override
    public boolean isInvulnerable() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    protected void dropExperience(Entity killer) { }

    // =========================================================================
    // COLLISION / PHYSIQUE — Pas de collision, pas de push, pas de pickup
    // =========================================================================

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    protected void doPush(Entity entity) { }

    @Override
    protected void pushEntities() { }

    @Override
    public boolean canBeLeashed() { return false; }

    // =========================================================================
    // DESPAWN — Ne despawn jamais (géré par timeout interne)
    // =========================================================================

    @Override
    public boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    @Override
    public boolean requiresCustomPersistence() { return true; }

    // =========================================================================
    // INTERACTIONS — Aucune interaction joueur
    // =========================================================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    // =========================================================================
    // ANGER / STING — Désactivés
    // =========================================================================

    @Override
    public void setRemainingPersistentAngerTime(int time) { }

    @Override
    public int getRemainingPersistentAngerTime() { return 0; }

    @Override
    public boolean isAngry() { return false; }

    @Override
    public boolean hasStung() { return false; }

    @Override
    public boolean doHurtTarget(Entity target) { return false; }

    // =========================================================================
    // POLLINATION / HIVE / NECTAR — Désactivés
    // =========================================================================

    @Override
    public boolean hasNectar() { return false; }

    // =========================================================================
    // SONS — Silencieux
    // =========================================================================

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) { }

    @Override
    public void playAmbientSound() { }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return null; }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    protected float getSoundVolume() { return 0.0f; }

    // =========================================================================
    // BREEDING — Désactivé
    // =========================================================================

    @Override
    public boolean canBreed() { return false; }

    @Override
    public boolean isFood(ItemStack stack) { return false; }

    // =========================================================================
    // GETTERS
    // =========================================================================

    /**
     * Notifie le controller que la tâche est complétée.
     */
    public void notifyTaskCompleted() {
        if (taskId == null || controllerPos == null || level() == null || level().isClientSide()) return;
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            controller.getDeliveryManager().completeTask(taskId);
        }
    }

    /**
     * Notifie le controller que la tâche a échoué (timeout).
     */
    private void notifyTaskFailed() {
        if (taskId == null || controllerPos == null || level() == null || level().isClientSide()) return;
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            controller.getDeliveryManager().failTask(taskId);
        }
    }

    public UUID getTaskId() { return taskId; }
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

    // =========================================================================
    // NBT — Éphémère, discard au rechargement
    // =========================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsDeliveryBee", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("IsDeliveryBee")) {
            this.discard();
        }
    }
}
