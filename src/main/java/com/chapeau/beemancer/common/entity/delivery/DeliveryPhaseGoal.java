/**
 * ============================================================
 * [DeliveryPhaseGoal.java]
 * Description: Goal unique de la DeliveryBee: vol multi-relais avec waypoints
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | DeliveryBeeEntity             | Entite porteuse      | Acces donnees tache            |
 * | StorageControllerBlockEntity  | Controller parent    | Extraction/depot items         |
 * | IDeliveryEndpoint             | Interface livraison  | Depot polymorphe               |
 * | ContainerHelper               | Utilitaire container | Insertion items                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DeliveryBeeEntity.java (ajout du goal)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.delivery;

import com.chapeau.beemancer.common.blockentity.storage.IDeliveryEndpoint;
import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.core.util.ContainerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal de livraison unifie multi-relais avec support redirection mid-flight.
 *
 * Flux normal (non-preloaded):
 *   FLY_TO_SOURCE -> WAIT_AT_SOURCE (extract) -> FLY_TO_DEST (via transit) -> WAIT_AT_DEST (deposit) -> FLY_HOME
 *
 * Flux preloaded (items deja charges):
 *   FLY_TO_DEST -> WAIT_AT_DEST (deposit) -> FLY_HOME
 *
 * Flux annulation sans items:
 *   [phase courante] -> REDIRECTING (nearest relay) -> applyNewTask -> FLY_TO_SOURCE
 *   ou si pas de nouvelle tache -> FLY_HOME
 *
 * Flux annulation avec items:
 *   [phase courante] -> SAVING_INVENTORY (deposit) -> REDIRECTING ou FLY_HOME
 */
public class DeliveryPhaseGoal extends Goal {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryPhaseGoal.class);
    private static final double ARRIVAL_DISTANCE_SQ = 4.0;
    private static final int BASE_WAIT_TICKS = 60;

    private final DeliveryBeeEntity bee;

    public enum Phase {
        FLY_TO_SOURCE,
        WAIT_AT_SOURCE,
        FLY_TO_DEST,
        WAIT_AT_DEST,
        FLY_HOME,
        SAVING_INVENTORY,
        REDIRECTING
    }

    private Phase phase = Phase.FLY_TO_SOURCE;
    private int waitTimer;
    private boolean navigationStarted = false;
    private boolean cancellationHandled = false;

    // Index courant dans la liste de waypoints (mutable int wrapper for helper)
    private final int[] outboundIdx = {0};
    private final int[] transitIdx = {0};
    private final int[] homeIdx = {0};
    private final int[] redirectIdx = {0};
    private final int[] savingIdx = {0};

    public DeliveryPhaseGoal(DeliveryBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public Phase getPhase() {
        return phase;
    }

    /**
     * Change la phase et synchronise vers le client via SynchedEntityData.
     */
    private void setPhase(Phase newPhase) {
        this.phase = newPhase;
        bee.setSyncedPhase(newPhase.name());
    }

    @Override
    public boolean canUse() {
        return bee.getSourcePos() != null && bee.getReturnPos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if (bee.isPreloaded()) {
            // Preloaded (export terminal): items charges depuis deposit slots, vol direct vers destination
            setPhase(Phase.FLY_TO_DEST);
            transitIdx[0] = 0;
            LOGGER.debug("[Phase] Bee {} start: PRELOADED → FLY_TO_DEST (carried: {}x{})",
                bee.getTaskId(), bee.getCarriedItems().getCount(), bee.getCarriedItems().getItem());
        } else {
            // Non-preloaded: toujours voler vers la source d'abord (meme avec items G3 pre-extraits)
            // Cela donne le visuel correct: bee sort du controller → vole au coffre → revient
            setPhase(Phase.FLY_TO_SOURCE);
            outboundIdx[0] = 0;
            LOGGER.debug("[Phase] Bee {} start: → FLY_TO_SOURCE {} (carried: {})",
                bee.getTaskId(), bee.getSourcePos(),
                bee.getCarriedItems().isEmpty() ? "empty" : bee.getCarriedItems().getCount() + "x" + bee.getCarriedItems().getItem());
        }
        homeIdx[0] = 0;
        navigationStarted = false;
    }

    @Override
    public void tick() {
        if (bee.isRecalled()) {
            handleRecall();
            return;
        }

        if (bee.isTaskCancelled() && !cancellationHandled) {
            handleTaskCancellation();
            return;
        }

        switch (phase) {
            case FLY_TO_SOURCE -> tickFlyToSource();
            case WAIT_AT_SOURCE -> tickWaitAtSource();
            case FLY_TO_DEST -> tickFlyToDest();
            case WAIT_AT_DEST -> tickWaitAtDest();
            case FLY_HOME -> tickFlyHome();
            case SAVING_INVENTORY -> tickSavingInventory();
            case REDIRECTING -> tickRedirecting();
        }
    }

    // =========================================================================
    // RECALL (prioritaire, force le retour)
    // =========================================================================

    /**
     * [AU] Recall avec depot automatique: si la bee transporte des items,
     * elle les depose d'abord dans un coffre du reseau avant de rentrer.
     * Comportement general: toute bee idle avec des items les depose.
     */
    private void handleRecall() {
        // Si la bee a des items, deposer dans un coffre d'abord
        if (!bee.getCarriedItems().isEmpty()) {
            if (phase != Phase.SAVING_INVENTORY && phase != Phase.FLY_HOME) {
                // Trouver un coffre pour deposer
                if (bee.getSavingChestPos() == null) {
                    BlockPos chest = bee.findSavingChest();
                    if (chest != null) {
                        bee.setSavingChestPos(chest);
                    }
                }
                if (bee.getSavingChestPos() != null) {
                    setPhase(Phase.SAVING_INVENTORY);
                    savingIdx[0] = 0;
                    navigationStarted = false;
                } else {
                    // Pas de coffre dispo: retour au controller (items restitues au reseau a l'arrivee)
                    setPhase(Phase.FLY_HOME);
                    homeIdx[0] = 0;
                    navigationStarted = false;
                }
            }

            if (phase == Phase.SAVING_INVENTORY) {
                tickSavingInventory();
                return;
            }
        } else if (phase != Phase.FLY_HOME) {
            setPhase(Phase.FLY_HOME);
            homeIdx[0] = 0;
            navigationStarted = false;
        }

        // Phase FLY_HOME: retour au controller
        if (navigateWaypoints(List.of(), homeIdx, bee.getReturnPos())) {
            bee.returnCarriedItemsToNetwork();
            bee.notifyTaskFailed();
            bee.discard();
        }
    }

    // =========================================================================
    // TASK CANCELLATION (mid-flight redirect)
    // =========================================================================

    /**
     * Gere l'annulation de tache mid-flight.
     * Si la bee transporte des items -> SAVING_INVENTORY d'abord.
     * Sinon -> REDIRECTING (si nouvelle tache) ou FLY_HOME.
     */
    private void handleTaskCancellation() {
        cancellationHandled = true;
        navigationStarted = false;

        if (!bee.getCarriedItems().isEmpty()) {
            BlockPos savingChest = bee.getSavingChestPos();
            if (savingChest != null) {
                setPhase(Phase.SAVING_INVENTORY);
                savingIdx[0] = 0;
            } else {
                // Pas de coffre disponible: retour au controller pour deposer
                setPhase(Phase.FLY_HOME);
                homeIdx[0] = 0;
            }
        } else {
            if (bee.hasNewTask() && bee.getRedirectTarget() != null) {
                setPhase(Phase.REDIRECTING);
                redirectIdx[0] = 0;
            } else {
                setPhase(Phase.FLY_HOME);
                homeIdx[0] = 0;
            }
        }
    }

    /**
     * Phase SAVING_INVENTORY: la bee vole vers un coffre pour deposer ses items,
     * puis passe a REDIRECTING ou FLY_HOME.
     */
    private void tickSavingInventory() {
        BlockPos savingChest = bee.getSavingChestPos();
        if (savingChest == null) {
            // Fallback: retour au controller
            setPhase(Phase.FLY_HOME);
            homeIdx[0] = 0;
            navigationStarted = false;
            return;
        }

        // Vol direct vers le coffre (pas de waypoints complexes, bee est invulnerable)
        if (navigateWaypoints(List.of(), savingIdx, savingChest)) {
            // Deposer les items dans le coffre
            if (!bee.level().isClientSide()) {
                performSavingDeposit(savingChest);
            }

            // Items deposes: passer a l'etape suivante
            navigationStarted = false;
            if (bee.hasNewTask() && bee.getRedirectTarget() != null) {
                setPhase(Phase.REDIRECTING);
                redirectIdx[0] = 0;
            } else {
                setPhase(Phase.FLY_HOME);
                homeIdx[0] = 0;
            }
        }
    }

    /**
     * Depose les items transportes dans le coffre de sauvegarde.
     * Les restes sont restitues au reseau (ou droppes au sol si reseau plein).
     */
    private void performSavingDeposit(BlockPos chestPos) {
        Level level = bee.level();
        ItemStack carried = bee.getCarriedItems();
        if (carried.isEmpty()) return;

        if (level.hasChunkAt(chestPos)) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                ItemStack remaining = ContainerHelper.insertItem(container, carried);
                if (!remaining.isEmpty()) {
                    bee.setCarriedItems(remaining);
                    bee.returnCarriedItemsToNetwork();
                } else {
                    bee.setCarriedItems(ItemStack.EMPTY);
                }
                return;
            }
        }

        // Fallback: coffre inaccessible, restituer au reseau
        bee.returnCarriedItemsToNetwork();
    }

    /**
     * Phase REDIRECTING: la bee vole vers le relay/controller le plus proche,
     * puis applique la nouvelle tache et repart vers la source.
     */
    private void tickRedirecting() {
        BlockPos redirectTarget = bee.getRedirectTarget();
        if (redirectTarget == null) {
            setPhase(Phase.FLY_HOME);
            homeIdx[0] = 0;
            navigationStarted = false;
            return;
        }

        if (navigateWaypoints(List.of(), redirectIdx, redirectTarget)) {
            if (bee.hasNewTask()) {
                bee.applyNewTask();
                setPhase(Phase.FLY_TO_SOURCE);
                outboundIdx[0] = 0;
                transitIdx[0] = 0;
                homeIdx[0] = 0;
                cancellationHandled = false;
            } else {
                setPhase(Phase.FLY_HOME);
                homeIdx[0] = 0;
            }
            navigationStarted = false;
        }
    }

    // =========================================================================
    // NAVIGATION HELPER
    // =========================================================================

    /**
     * Navigue a travers une liste de waypoints puis vers une destination finale.
     * Retourne true quand la destination finale est atteinte.
     */
    private boolean navigateWaypoints(List<BlockPos> waypoints, int[] indexRef, BlockPos finalTarget) {
        BlockPos currentTarget;
        if (indexRef[0] < waypoints.size()) {
            currentTarget = waypoints.get(indexRef[0]);
        } else {
            currentTarget = finalTarget;
        }

        if (!navigationStarted || bee.getNavigation().isDone()) {
            bee.getNavigation().moveTo(
                currentTarget.getX() + 0.5, currentTarget.getY() + 0.5, currentTarget.getZ() + 0.5,
                1.0 * bee.getFlySpeedMultiplier()
            );
            navigationStarted = true;
        }

        if (bee.distanceToSqr(currentTarget.getX() + 0.5, currentTarget.getY() + 0.5, currentTarget.getZ() + 0.5) < ARRIVAL_DISTANCE_SQ) {
            bee.getNavigation().stop();
            navigationStarted = false;

            if (indexRef[0] < waypoints.size()) {
                indexRef[0]++;
                return false;
            }
            return true;
        }
        return false;
    }

    // =========================================================================
    // STANDARD PHASES
    // =========================================================================

    private void tickFlyToSource() {
        if (navigateWaypoints(bee.getOutboundWaypoints(), outboundIdx, bee.getSourcePos())) {
            setPhase(Phase.WAIT_AT_SOURCE);
            waitTimer = Math.max(10, Math.round(BASE_WAIT_TICKS / bee.getSearchSpeedMultiplier()));
            LOGGER.debug("[Phase] Bee {} arrived at source {} → WAIT_AT_SOURCE ({}t)",
                bee.getTaskId(), bee.getSourcePos(), waitTimer);
        }
    }

    private void tickWaitAtSource() {
        waitTimer--;
        if (waitTimer > 0) return;

        Level level = bee.level();
        if (level.isClientSide()) return;

        // [FIX] Si la bee transporte deja des items (G3 pre-extraction atomique), skip l'extraction
        // Les items ont ete extraits AVANT le spawn pour eviter les TOCTOU,
        // mais la bee doit quand meme visuellement voler jusqu'au coffre source
        if (bee.getCarriedItems().isEmpty()) {
            LOGGER.debug("[Phase] Bee {} WAIT_AT_SOURCE: extracting from {}", bee.getTaskId(), bee.getSourcePos());
            performExtraction(level);
            LOGGER.debug("[Phase] Bee {} extraction result: {}", bee.getTaskId(),
                bee.getCarriedItems().isEmpty() ? "EMPTY (failed)" : bee.getCarriedItems().getCount() + "x" + bee.getCarriedItems().getItem());
        } else {
            LOGGER.debug("[Phase] Bee {} WAIT_AT_SOURCE: skip extraction (G3 pre-extracted {}x{})",
                bee.getTaskId(), bee.getCarriedItems().getCount(), bee.getCarriedItems().getItem());
        }

        if (bee.getCarriedItems().isEmpty()) {
            bee.notifyTaskFailed();
            bee.discard();
            return;
        }

        setPhase(Phase.FLY_TO_DEST);
        transitIdx[0] = 0;
        navigationStarted = false;
    }

    private void tickFlyToDest() {
        if (navigateWaypoints(bee.getTransitWaypoints(), transitIdx, bee.getDestPos())) {
            setPhase(Phase.WAIT_AT_DEST);
            waitTimer = Math.max(10, Math.round(BASE_WAIT_TICKS / bee.getSearchSpeedMultiplier()));
            LOGGER.debug("[Phase] Bee {} arrived at dest {} → WAIT_AT_DEST (carrying {}x{})",
                bee.getTaskId(), bee.getDestPos(),
                bee.getCarriedItems().getCount(), bee.getCarriedItems().getItem());
        }
    }

    private void tickWaitAtDest() {
        waitTimer--;
        if (waitTimer > 0) return;

        if (!bee.level().isClientSide()) {
            if (!isDestValid()) {
                bee.returnCarriedItemsToNetwork();
                bee.notifyTaskFailed();
                setPhase(Phase.FLY_HOME);
                homeIdx[0] = 0;
                navigationStarted = false;
                return;
            }
            bee.snapshotCarriedItems();
            LOGGER.debug("[Phase] Bee {} delivering {}x{} at {}",
                bee.getTaskId(), bee.getCarriedItems().getCount(),
                bee.getCarriedItems().getItem(), bee.getDestPos());
            performDelivery();
        }

        setPhase(Phase.FLY_HOME);
        homeIdx[0] = 0;
        navigationStarted = false;
    }

    private void tickFlyHome() {
        if (navigateWaypoints(bee.getHomeWaypoints(), homeIdx, bee.getReturnPos())) {
            LOGGER.debug("[Phase] Bee {} arrived home → task complete", bee.getTaskId());
            bee.notifyTaskCompleted();
            bee.discard();
        }
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    private boolean isDestValid() {
        Level level = bee.level();
        BlockPos destPos = bee.getDestPos();
        if (destPos == null || !level.hasChunkAt(destPos)) return false;

        BlockEntity be = level.getBlockEntity(destPos);
        if (be == null) return false;

        if (be instanceof IDeliveryEndpoint) {
            if (be instanceof NetworkInterfaceBlockEntity interfaceBe) {
                if (!interfaceBe.isActive()) return false;
                if (interfaceBe.getController() == null) return false;
                if (interfaceBe.getAdjacentItemHandler() == null) return false;
            }
            return true;
        }

        if (be instanceof Container container) {
            return hasSpaceForItem(container, bee.getCarriedItems());
        }

        return false;
    }

    private boolean hasSpaceForItem(Container container, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return ContainerHelper.hasSpaceFor(container, stack, 1);
    }

    // =========================================================================
    // ITEM OPERATIONS
    // =========================================================================

    /**
     * Extraction a la source: le controller extrait l'item du coffre/container.
     * Pour les taches d'interface, lit le count actuel depuis l'InterfaceTask
     * pour s'adapter aux changements pendant le transit.
     */
    private void performExtraction(Level level) {
        BlockPos controllerPos = bee.getControllerPos();
        if (!level.hasChunkAt(controllerPos)) {
            bee.notifyTaskFailed();
            bee.discard();
            return;
        }
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            bee.discard();
            return;
        }

        int maxExtract;
        if (bee.getInterfaceTaskId() != null) {
            int currentCount = bee.readCurrentInterfaceTaskCount();
            if (currentCount <= 0) {
                // Task annulee ou count a 0: abandonner
                bee.notifyTaskFailed();
                bee.discard();
                return;
            }
            maxExtract = Math.min(currentCount, bee.getRequestCount());
        } else {
            maxExtract = bee.getRequestCount();
        }

        ItemStack extracted = controller.extractItemForDelivery(
            bee.getTemplate(), maxExtract, bee.getSourcePos()
        );
        bee.setCarriedItems(extracted);
    }

    /**
     * Livraison a destination: insere les items dans un IDeliveryEndpoint (import interface,
     * terminal) ou directement dans un Container (coffre du reseau pour export).
     *
     * Pour les taches d'interface, lit le count actuel et adapte la livraison:
     * - Si le count a diminue: livrer seulement le count actuel, retourner l'excedent au reseau
     * - Si le count a augmente: livrer tout, l'interface creera une nouvelle task pour le delta
     */
    private void performDelivery() {
        Level level = bee.level();
        if (level.isClientSide()) return;

        ItemStack carried = bee.getCarriedItems();
        if (carried.isEmpty()) return;

        // Adaptation du count pour les taches d'interface
        ItemStack toDeliver = carried;
        ItemStack excess = ItemStack.EMPTY;

        if (bee.getInterfaceTaskId() != null) {
            int currentCount = bee.readCurrentInterfaceTaskCount();
            if (currentCount >= 0 && carried.getCount() > currentCount) {
                // Task a diminue: livrer seulement currentCount, retourner le reste
                if (currentCount > 0) {
                    toDeliver = carried.copyWithCount(currentCount);
                } else {
                    toDeliver = ItemStack.EMPTY;
                }
                excess = carried.copyWithCount(carried.getCount() - Math.max(0, currentCount));
            }
        }

        BlockPos destPos = bee.getDestPos();
        if (!level.hasChunkAt(destPos)) {
            returnToNetwork(level, toDeliver.isEmpty() ? carried : toDeliver);
            if (!excess.isEmpty()) {
                returnToNetwork(level, excess);
            }
            bee.setCarriedItems(ItemStack.EMPTY);
            return;
        }
        BlockEntity destBe = level.getBlockEntity(destPos);

        ItemStack remaining;
        if (!toDeliver.isEmpty()) {
            if (destBe instanceof IDeliveryEndpoint endpoint) {
                remaining = endpoint.receiveDeliveredItems(toDeliver);
            } else if (destBe instanceof Container container) {
                remaining = ContainerHelper.insertItem(container, toDeliver);
            } else {
                remaining = toDeliver;
            }
        } else {
            remaining = ItemStack.EMPTY;
        }

        // Combiner remaining + excess et retourner au reseau
        ItemStack toReturn = ItemStack.EMPTY;
        if (!remaining.isEmpty() && !excess.isEmpty()
                && ItemStack.isSameItemSameComponents(remaining, excess)) {
            toReturn = remaining.copyWithCount(remaining.getCount() + excess.getCount());
        } else if (!remaining.isEmpty()) {
            toReturn = remaining;
            if (!excess.isEmpty()) {
                // Types differents: deposer l'excedent separement
                returnToNetwork(level, excess);
            }
        } else if (!excess.isEmpty()) {
            toReturn = excess;
        }

        if (!toReturn.isEmpty()) {
            returnToNetwork(level, toReturn);
        }

        bee.setCarriedItems(ItemStack.EMPTY);
    }

    /**
     * Restitue des items au reseau de stockage via le controller.
     * Si le controller est inaccessible ou le reseau plein, drop les items au sol.
     */
    private void returnToNetwork(Level level, ItemStack items) {
        if (items.isEmpty()) return;

        BlockPos controllerPos = bee.getControllerPos();
        if (!level.hasChunkAt(controllerPos)) {
            bee.spawnAtLocation(items);
            return;
        }
        BlockEntity controllerBe = level.getBlockEntity(controllerPos);
        if (controllerBe instanceof StorageControllerBlockEntity controller) {
            ItemStack remaining = controller.depositItemForDelivery(items, null);
            if (!remaining.isEmpty()) {
                bee.spawnAtLocation(remaining);
            }
        } else {
            bee.spawnAtLocation(items);
        }
    }
}
