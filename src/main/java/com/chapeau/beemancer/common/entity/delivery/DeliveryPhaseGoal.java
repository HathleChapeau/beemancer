/**
 * ============================================================
 * [DeliveryPhaseGoal.java]
 * Description: Goal unique de la DeliveryBee: 3 phases (vol → attente → retour)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | DeliveryBeeEntity             | Entité porteuse      | Accès données tâche            |
 * | StorageControllerBlockEntity  | Controller parent    | Extraction/dépôt items         |
 * | StorageTerminalBlockEntity    | Terminal cible       | Insertion items pickup         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - DeliveryBeeEntity.java (ajout du goal)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.delivery;

import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * Goal unique gérant les 3 phases de livraison:
 *
 * Phase 1 - FLY_TO_TARGET: naviguer vers le coffre cible
 * Phase 2 - WAIT_AT_TARGET: attendre (recherche/craft), puis extraire ou déposer
 * Phase 3 - FLY_BACK: naviguer vers le point de retour, livrer puis discard
 */
public class DeliveryPhaseGoal extends Goal {

    private static final double ARRIVAL_DISTANCE_SQ = 4.0; // 2 blocs²
    private static final int BASE_WAIT_TICKS = 60; // 3 secondes base

    private final DeliveryBeeEntity bee;

    private enum Phase {
        FLY_TO_TARGET,
        WAIT_AT_TARGET,
        FLY_BACK
    }

    private Phase phase = Phase.FLY_TO_TARGET;
    private int waitTimer;
    private boolean navigationStarted = false;

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
        phase = Phase.FLY_TO_TARGET;
        navigationStarted = false;
    }

    @Override
    public void tick() {
        switch (phase) {
            case FLY_TO_TARGET -> tickFlyToTarget();
            case WAIT_AT_TARGET -> tickWaitAtTarget();
            case FLY_BACK -> tickFlyBack();
        }
    }

    private void tickFlyToTarget() {
        BlockPos target = bee.getTargetPos();
        if (!navigationStarted || bee.getNavigation().isDone()) {
            bee.getNavigation().moveTo(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                1.0 * bee.getFlySpeedMultiplier()
            );
            navigationStarted = true;
        }

        if (bee.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) < ARRIVAL_DISTANCE_SQ) {
            bee.getNavigation().stop();
            phase = Phase.WAIT_AT_TARGET;
            waitTimer = Math.max(10, Math.round(BASE_WAIT_TICKS / bee.getSearchSpeedMultiplier()));
            navigationStarted = false;
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

        phase = Phase.FLY_BACK;
        navigationStarted = false;
    }

    private void tickFlyBack() {
        BlockPos returnPos = bee.getReturnPos();
        if (!navigationStarted || bee.getNavigation().isDone()) {
            bee.getNavigation().moveTo(
                returnPos.getX() + 0.5, returnPos.getY() + 0.5, returnPos.getZ() + 0.5,
                1.0 * bee.getFlySpeedMultiplier()
            );
            navigationStarted = true;
        }

        if (bee.distanceToSqr(returnPos.getX() + 0.5, returnPos.getY() + 0.5, returnPos.getZ() + 0.5) < ARRIVAL_DISTANCE_SQ) {
            bee.getNavigation().stop();
            performDelivery();
            bee.notifyTaskCompleted();
            bee.discard();
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
     * DEPOSIT: l'abeille dépose ses items dans le coffre via le controller.
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
     * Livraison au retour: insère les items dans le terminal (EXTRACT)
     * ou confirme le dépôt (DEPOSIT).
     */
    private void performDelivery() {
        Level level = bee.level();
        if (level.isClientSide()) return;

        if (bee.getDeliveryType() == DeliveryTask.DeliveryType.EXTRACT) {
            // Insérer les items extraits dans les pickup slots du terminal
            BlockEntity terminalBe = level.getBlockEntity(bee.getTerminalPos());
            if (terminalBe instanceof StorageTerminalBlockEntity terminal) {
                ItemStack remaining = terminal.insertIntoPickupSlots(bee.getCarriedItems());
                // Si des items restent, les remettre dans le réseau
                if (!remaining.isEmpty()) {
                    BlockEntity controllerBe = level.getBlockEntity(bee.getControllerPos());
                    if (controllerBe instanceof StorageControllerBlockEntity controller) {
                        controller.depositItemForDelivery(remaining, null);
                    }
                }
            }
        }
        // DEPOSIT: déjà déposé dans le coffre en phase 2, rien à faire au retour
    }
}
