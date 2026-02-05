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

import java.util.EnumSet;
import java.util.List;

/**
 * Goal de livraison unifie multi-relais.
 *
 * Flux normal (non-preloaded):
 *   FLY_TO_SOURCE → WAIT_AT_SOURCE (extract) → FLY_TO_DEST (via transit) → WAIT_AT_DEST (deposit) → FLY_HOME
 *
 * Flux preloaded (items deja charges):
 *   FLY_TO_DEST → WAIT_AT_DEST (deposit) → FLY_HOME
 *
 * Le chemin transit passe par l'ancetre commun (LCA) des relais source et dest,
 * sans repasser par le controller si ce n'est pas necessaire.
 */
public class DeliveryPhaseGoal extends Goal {

    private static final double ARRIVAL_DISTANCE_SQ = 4.0;
    private static final int BASE_WAIT_TICKS = 60;

    private final DeliveryBeeEntity bee;

    public enum Phase {
        FLY_TO_SOURCE,
        WAIT_AT_SOURCE,
        FLY_TO_DEST,
        WAIT_AT_DEST,
        FLY_HOME
    }

    private Phase phase = Phase.FLY_TO_SOURCE;
    private int waitTimer;
    private boolean navigationStarted = false;

    // Index courant dans la liste de waypoints (mutable int wrapper for helper)
    private final int[] outboundIdx = {0};
    private final int[] transitIdx = {0};
    private final int[] homeIdx = {0};

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
        if (!bee.getCarriedItems().isEmpty()) {
            // Preloaded: sauter source, aller directement a dest
            setPhase(Phase.FLY_TO_DEST);
            transitIdx[0] = 0;
        } else {
            setPhase(Phase.FLY_TO_SOURCE);
            outboundIdx[0] = 0;
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
        switch (phase) {
            case FLY_TO_SOURCE -> tickFlyToSource();
            case WAIT_AT_SOURCE -> tickWaitAtSource();
            case FLY_TO_DEST -> tickFlyToDest();
            case WAIT_AT_DEST -> tickWaitAtDest();
            case FLY_HOME -> tickFlyHome();
        }
    }

    /**
     * Gere le recall: force la bee a voler directement vers le controller (returnPos),
     * restitue les items au reseau, notifie l'echec, puis discard.
     */
    private void handleRecall() {
        if (phase != Phase.FLY_HOME) {
            setPhase(Phase.FLY_HOME);
            homeIdx[0] = 0;
            navigationStarted = false;
        }
        if (navigateWaypoints(List.of(), homeIdx, bee.getReturnPos())) {
            bee.returnCarriedItemsToNetwork();
            bee.notifyTaskFailed();
            bee.discard();
        }
    }

    /**
     * Navigue a travers une liste de waypoints puis vers une destination finale.
     * Retourne true quand la destination finale est atteinte.
     *
     * @param waypoints liste ordonnee de waypoints intermediaires
     * @param indexRef tableau mutable de taille 1 contenant l'index courant
     * @param finalTarget destination finale apres tous les waypoints
     * @return true si la destination finale est atteinte
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

    /**
     * Navigue vers la source via les waypoints outbound.
     */
    private void tickFlyToSource() {
        if (navigateWaypoints(bee.getOutboundWaypoints(), outboundIdx, bee.getSourcePos())) {
            setPhase(Phase.WAIT_AT_SOURCE);
            waitTimer = Math.max(10, Math.round(BASE_WAIT_TICKS / bee.getSearchSpeedMultiplier()));
        }
    }

    /**
     * Attente a la source: extraction des items.
     */
    private void tickWaitAtSource() {
        waitTimer--;
        if (waitTimer > 0) return;

        Level level = bee.level();
        if (level.isClientSide()) return;

        performExtraction(level);

        if (bee.getCarriedItems().isEmpty()) {
            bee.notifyTaskFailed();
            bee.discard();
            return;
        }

        setPhase(Phase.FLY_TO_DEST);
        transitIdx[0] = 0;
        navigationStarted = false;
    }

    /**
     * Navigue de la source vers la destination via les waypoints transit (LCA).
     * Passe par l'ancetre commun des relais source et dest sans repasser par le controller.
     */
    private void tickFlyToDest() {
        if (navigateWaypoints(bee.getTransitWaypoints(), transitIdx, bee.getDestPos())) {
            setPhase(Phase.WAIT_AT_DEST);
            waitTimer = Math.max(10, Math.round(BASE_WAIT_TICKS / bee.getSearchSpeedMultiplier()));
        }
    }

    /**
     * Attente a destination: depot des items puis retour au controller.
     */
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
            performDelivery();
        }

        // Retour au controller via les waypoints home
        setPhase(Phase.FLY_HOME);
        homeIdx[0] = 0;
        navigationStarted = false;
    }

    /**
     * Retour de la destination vers le controller via les waypoints home.
     */
    private void tickFlyHome() {
        if (navigateWaypoints(bee.getHomeWaypoints(), homeIdx, bee.getReturnPos())) {
            bee.notifyTaskCompleted();
            bee.discard();
        }
    }

    /**
     * Verifie que la destination (endpoint de livraison) est encore valide.
     * Couvre les cas: interface desactivee/delinkee, inventaire adjacent detruit,
     * coffre destination plein/detruit, chunk decharge.
     */
    private boolean isDestValid() {
        Level level = bee.level();
        BlockPos destPos = bee.getDestPos();
        if (destPos == null || !level.isLoaded(destPos)) return false;

        BlockEntity be = level.getBlockEntity(destPos);
        if (be == null) return false;

        if (be instanceof IDeliveryEndpoint) {
            if (be instanceof NetworkInterfaceBlockEntity interfaceBe) {
                if (!interfaceBe.isActive()) return false;
                if (interfaceBe.getController() == null) return false;
                if (interfaceBe.getAdjacentInventory() == null) return false;
            }
            return true;
        }

        if (be instanceof Container container) {
            return hasSpaceForItem(container, bee.getCarriedItems());
        }

        return false;
    }

    /**
     * Verifie qu'un container a au moins un slot disponible pour l'item donne.
     */
    private boolean hasSpaceForItem(Container container, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return ContainerHelper.hasSpaceFor(container, stack, 1);
    }

    /**
     * Extraction a la source: le controller extrait l'item du coffre/container.
     */
    private void performExtraction(Level level) {
        BlockEntity be = level.getBlockEntity(bee.getControllerPos());
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            bee.discard();
            return;
        }

        ItemStack extracted = controller.extractItemForDelivery(
            bee.getTemplate(), bee.getRequestCount(), bee.getSourcePos()
        );
        bee.setCarriedItems(extracted);
    }

    /**
     * Livraison a destination: insere les items dans un IDeliveryEndpoint (import interface,
     * terminal) ou directement dans un Container (coffre du reseau pour export).
     */
    private void performDelivery() {
        Level level = bee.level();
        if (level.isClientSide()) return;

        ItemStack carried = bee.getCarriedItems();
        if (carried.isEmpty()) return;

        BlockEntity destBe = level.getBlockEntity(bee.getDestPos());

        ItemStack remaining;
        if (destBe instanceof IDeliveryEndpoint endpoint) {
            remaining = endpoint.receiveDeliveredItems(carried);
        } else if (destBe instanceof Container container) {
            remaining = ContainerHelper.insertItem(container, carried);
        } else {
            remaining = carried;
        }

        if (!remaining.isEmpty()) {
            BlockEntity controllerBe = level.getBlockEntity(bee.getControllerPos());
            if (controllerBe instanceof StorageControllerBlockEntity controller) {
                controller.depositItemForDelivery(remaining, null);
            }
        }

        bee.setCarriedItems(ItemStack.EMPTY);
    }
}
