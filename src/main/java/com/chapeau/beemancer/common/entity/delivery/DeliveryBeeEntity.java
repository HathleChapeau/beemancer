/**
 * ============================================================
 * [DeliveryBeeEntity.java]
 * Description: Abeille visuelle de livraison pour le reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | Bee                           | Entite parent        | Skin vanilla                   |
 * | StorageControllerBlockEntity  | Controller parent    | Extraction/depot items         |
 * | DeliveryPhaseGoal             | AI unique            | Vol → attente → retour         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (spawn)
 * - ClientSetup.java (renderer = vanilla BeeRenderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.delivery;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import java.util.ArrayList;
import java.util.List;
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
 * Abeille de livraison pour le reseau de stockage.
 * Purement visuelle: skin de Bee vanilla avec pathfinding volant.
 * Tous les comportements vanilla (anger, sting, pollination, hive, breeding,
 * sons, drops, interactions) sont neutralises.
 *
 * Flux unifie: source → extraction → controller → dest → depot → controller.
 * Si preloaded (items deja charges), saute la phase source.
 *
 * Timeout: se discard apres 2400 ticks (2 min).
 * Verifie chaque tick que le controller est encore forme.
 */
public class DeliveryBeeEntity extends Bee {

    private static final int TIMEOUT_TICKS = 2400;

    private BlockPos controllerPos;
    private BlockPos sourcePos;
    private BlockPos returnPos;
    private BlockPos destPos;
    private ItemStack template = ItemStack.EMPTY;
    private int requestCount;
    private ItemStack carriedItems = ItemStack.EMPTY;
    private UUID taskId;
    private float flySpeedMultiplier = 1.0f;
    private float searchSpeedMultiplier = 1.0f;
    private int ticksAlive = 0;
    private boolean recalled = false;

    // Waypoints pour le trajet multi-relais
    // outboundWaypoints: controller → relay1 → relay2 → ... → source
    // returnWaypoints: source → ... → relay2 → relay1 → controller
    // destWaypoints: controller → relay1 → ... → dest (pour livraison a une interface/coffre)
    private List<BlockPos> outboundWaypoints = new ArrayList<>();
    private List<BlockPos> returnWaypoints = new ArrayList<>();
    private List<BlockPos> destWaypoints = new ArrayList<>();

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
     * Initialise la tache de livraison apres le spawn.
     * Doit etre appele immediatement apres avoir spawn l'entite.
     */
    public void initDeliveryTask(BlockPos controllerPos, BlockPos sourcePos, BlockPos returnPos,
                                  BlockPos destPos, ItemStack template, int requestCount,
                                  ItemStack carriedItems,
                                  float flySpeedMultiplier, float searchSpeedMultiplier,
                                  UUID taskId) {
        this.controllerPos = controllerPos;
        this.sourcePos = sourcePos;
        this.returnPos = returnPos;
        this.destPos = destPos;
        this.template = template.copy();
        this.requestCount = requestCount;
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
            // Abeille chargee depuis une sauvegarde sans tache: cleanup et discard
            if (taskId == null) {
                returnCarriedItemsToNetwork();
                discard();
                return;
            }
            if (ticksAlive > TIMEOUT_TICKS) {
                notifyTaskFailed();
                returnCarriedItemsToNetwork();
                discard();
                return;
            }
            if (!isControllerValid()) {
                discard();
            }
        }
    }

    /**
     * Restitue les items transportes dans le reseau de stockage si possible.
     * Appele lors du discard pour eviter la perte d'items en transit.
     */
    public void returnCarriedItemsToNetwork() {
        if (carriedItems.isEmpty() || controllerPos == null || level() == null) return;
        if (!level().isLoaded(controllerPos)) return;
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            controller.depositItemForDelivery(carriedItems, null);
            carriedItems = ItemStack.EMPTY;
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
    // INVULNERABILITE — Aucun degat, aucune mort, aucun loot
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
    // DESPAWN — Ne despawn jamais (gere par timeout interne)
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
    // ANGER / STING — Desactives
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
    // POLLINATION / HIVE / NECTAR — Desactives
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
    // BREEDING — Desactive
    // =========================================================================

    @Override
    public boolean canBreed() { return false; }

    @Override
    public boolean isFood(ItemStack stack) { return false; }

    // =========================================================================
    // GETTERS
    // =========================================================================

    /**
     * Notifie le controller que la tache est completee.
     */
    public void notifyTaskCompleted() {
        if (taskId == null || controllerPos == null || level() == null || level().isClientSide()) return;
        if (!level().isLoaded(controllerPos)) return;
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            controller.getDeliveryManager().completeTask(taskId);
        }
    }

    /**
     * Notifie le controller que la tache a echoue (timeout).
     */
    public void notifyTaskFailed() {
        if (taskId == null || controllerPos == null || level() == null || level().isClientSide()) return;
        if (!level().isLoaded(controllerPos)) return;
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            controller.getDeliveryManager().failTask(taskId);
        }
    }

    /**
     * Rappelle la bee vers le controller. Elle volera directement vers returnPos,
     * restituera ses items au reseau, puis se discard.
     */
    public void recall() {
        this.recalled = true;
    }

    public boolean isRecalled() { return recalled; }

    public UUID getTaskId() { return taskId; }
    public BlockPos getControllerPos() { return controllerPos; }
    public List<BlockPos> getOutboundWaypoints() { return outboundWaypoints; }
    public List<BlockPos> getReturnWaypoints() { return returnWaypoints; }
    public List<BlockPos> getDestWaypoints() { return destWaypoints; }

    public void setWaypoints(List<BlockPos> outbound, List<BlockPos> returnPath) {
        this.outboundWaypoints = new ArrayList<>(outbound);
        this.returnWaypoints = new ArrayList<>(returnPath);
    }

    public void setDestWaypoints(List<BlockPos> destPath) {
        this.destWaypoints = new ArrayList<>(destPath);
    }

    public BlockPos getSourcePos() { return sourcePos; }
    public BlockPos getReturnPos() { return returnPos; }
    public BlockPos getDestPos() { return destPos; }
    public ItemStack getTemplate() { return template; }
    public int getRequestCount() { return requestCount; }
    public ItemStack getCarriedItems() { return carriedItems; }
    public float getFlySpeedMultiplier() { return flySpeedMultiplier; }
    public float getSearchSpeedMultiplier() { return searchSpeedMultiplier; }

    public void setCarriedItems(ItemStack items) {
        this.carriedItems = items.copy();
    }

    // =========================================================================
    // NBT — Ephemere, discard au rechargement
    // =========================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsDeliveryBee", true);
        if (controllerPos != null) {
            tag.putInt("CtrlX", controllerPos.getX());
            tag.putInt("CtrlY", controllerPos.getY());
            tag.putInt("CtrlZ", controllerPos.getZ());
        }
        if (!carriedItems.isEmpty()) {
            tag.put("CarriedItems", carriedItems.save(level().registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("IsDeliveryBee")) {
            // Restaurer le minimum pour le cleanup (restitution items, notification)
            if (tag.contains("CtrlX")) {
                controllerPos = new BlockPos(
                    tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"));
            }
            if (tag.contains("CarriedItems")) {
                carriedItems = ItemStack.parse(
                    level().registryAccess(), tag.getCompound("CarriedItems")
                ).orElse(ItemStack.EMPTY);
            }
            // Marquer pour discard (taskId reste null → cleanup au premier tick)
            this.discard();
        }
    }
}
