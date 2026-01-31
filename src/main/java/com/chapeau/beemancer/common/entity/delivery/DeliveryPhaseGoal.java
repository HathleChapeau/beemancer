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
 * | StorageTerminalBlockEntity    | Terminal cible       | Insertion items pickup         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DeliveryBeeEntity.java (ajout du goal)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.delivery;

import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.IDeliveryEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal de livraison multi-relais.
 *
 * Outbound: waypoint0 → waypoint1 → ... → target (chest)
 * Wait: extraction ou depot au chest
 * Return: waypoint0 → waypoint1 → ... → returnPos (controller)
 *
 * Si pas de waypoints, fonctionne comme avant (vol direct).
 */
public class DeliveryPhaseGoal extends Goal {

    private static final double ARRIVAL_DISTANCE_SQ = 4.0;
    private static final int BASE_WAIT_TICKS = 60;

    private final DeliveryBeeEntity bee;

    private enum Phase {
        FLY_OUTBOUND,
        WAIT_AT_TARGET,
        FLY_RETURN,
        FLY_TO_TERMINAL
    }

    private Phase phase = Phase.FLY_OUTBOUND;
    private int waitTimer;
    private boolean navigationStarted = false;

    // Index courant dans la liste de waypoints
    private int outboundIndex = 0;
    private int returnIndex = 0;
    private int terminalIndex = 0;

    public DeliveryPhaseGoal(DeliveryBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return bee.getTargetPos() != null && bee.getReturnPos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        phase = Phase.FLY_OUTBOUND;
        outboundIndex = 0;
        returnIndex = 0;
        navigationStarted = false;
    }

    @Override
    public void tick() {
        switch (phase) {
            case FLY_OUTBOUND -> tickFlyOutbound();
            case WAIT_AT_TARGET -> tickWaitAtTarget();
            case FLY_RETURN -> tickFlyReturn();
            case FLY_TO_TERMINAL -> tickFlyToTerminal();
        }
    }

    /**
     * Navigue a travers les waypoints outbound puis vers le target.
     */
    private void tickFlyOutbound() {
        List<BlockPos> waypoints = bee.getOutboundWaypoints();
        BlockPos currentTarget;

        if (outboundIndex < waypoints.size()) {
            currentTarget = waypoints.get(outboundIndex);
        } else {
            currentTarget = bee.getTargetPos();
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

            if (outboundIndex < waypoints.size()) {
                // Waypoint relay atteint, passer au suivant
                outboundIndex++;
            } else {
                // Target (chest) atteint, passer en attente
                phase = Phase.WAIT_AT_TARGET;
                waitTimer = Math.max(10, Math.round(BASE_WAIT_TICKS / bee.getSearchSpeedMultiplier()));
            }
        }
    }

    private void tickWaitAtTarget() {
        waitTimer--;
        if (waitTimer > 0) return;

        Level level = bee.level();
        if (level.isClientSide()) return;

        if (bee.getDeliveryType() == DeliveryTask.DeliveryType.EXTRACT) {
            performExtraction(level);
        } else {
            performDeposit(level);
        }

        phase = Phase.FLY_RETURN;
        returnIndex = 0;
        navigationStarted = false;
    }

    /**
     * Navigue a travers les waypoints retour puis vers le returnPos.
     */
    private void tickFlyReturn() {
        List<BlockPos> waypoints = bee.getReturnWaypoints();
        BlockPos currentTarget;

        if (returnIndex < waypoints.size()) {
            currentTarget = waypoints.get(returnIndex);
        } else {
            currentTarget = bee.getReturnPos();
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

            if (returnIndex < waypoints.size()) {
                // Waypoint relay atteint, passer au suivant
                returnIndex++;
            } else {
                // Arrivee au controller
                if (bee.needsTerminalFlight()) {
                    // Tache EXTRACT avec terminal distant: voler jusqu'au terminal
                    phase = Phase.FLY_TO_TERMINAL;
                    terminalIndex = 0;
                    navigationStarted = false;
                } else {
                    // Livraison directe et discard
                    performDelivery();
                    bee.notifyTaskCompleted();
                    bee.discard();
                }
            }
        }
    }

    /**
     * Navigue du controller vers le terminal (import interface) via les waypoints terminaux.
     * Phase finale pour les taches EXTRACT avec terminal distant.
     */
    private void tickFlyToTerminal() {
        List<BlockPos> waypoints = bee.getTerminalWaypoints();
        BlockPos currentTarget;

        if (terminalIndex < waypoints.size()) {
            currentTarget = waypoints.get(terminalIndex);
        } else {
            currentTarget = bee.getTerminalPos();
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

            if (terminalIndex < waypoints.size()) {
                // Waypoint relay atteint, passer au suivant
                terminalIndex++;
            } else {
                // Arrivee au terminal, livrer et discard
                performDelivery();
                bee.notifyTaskCompleted();
                bee.discard();
            }
        }
    }

    /**
     * EXTRACT: le controller extrait l'item du coffre, l'abeille le transporte.
     */
    private void performExtraction(Level level) {
        BlockEntity be = level.getBlockEntity(bee.getControllerPos());
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            bee.discard();
            return;
        }

        ItemStack extracted = controller.extractItemForDelivery(
            bee.getTemplate(), bee.getRequestCount(), bee.getTargetPos()
        );
        bee.setCarriedItems(extracted);
    }

    /**
     * DEPOSIT: l'abeille depose ses items dans le coffre via le controller.
     */
    private void performDeposit(Level level) {
        BlockEntity be = level.getBlockEntity(bee.getControllerPos());
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            bee.discard();
            return;
        }

        ItemStack remaining = controller.depositItemForDelivery(
            bee.getCarriedItems(), bee.getTargetPos()
        );
        bee.setCarriedItems(remaining);
    }

    /**
     * Livraison au retour: insere les items dans le terminal (EXTRACT)
     * ou confirme le depot (DEPOSIT).
     */
    private void performDelivery() {
        Level level = bee.level();
        if (level.isClientSide()) return;

        if (bee.getDeliveryType() == DeliveryTask.DeliveryType.EXTRACT) {
            BlockEntity terminalBe = level.getBlockEntity(bee.getTerminalPos());
            if (terminalBe instanceof IDeliveryEndpoint endpoint) {
                ItemStack remaining = endpoint.receiveDeliveredItems(bee.getCarriedItems());
                if (!remaining.isEmpty()) {
                    BlockEntity controllerBe = level.getBlockEntity(bee.getControllerPos());
                    if (controllerBe instanceof StorageControllerBlockEntity controller) {
                        controller.depositItemForDelivery(remaining, null);
                    }
                }
            }
        }
    }
}
