/**
 * ============================================================
 * [HoneyEnergyManager.java]
 * Description: Gestion de la consommation de miel du controller de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageControllerBlockEntity   | Parent controller    | Buffer miel, stats, notif |
 * | ControllerStats                | Stats essences       | Calcul consommation       |
 * | HoneyReservoirBlockEntity      | Pull miel            | Reservoir multibloc       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Gere la consommation de miel du controller via un buffer interne.
 * Pull: les HoneyReservoirs du multibloc transferent vers le buffer du controller.
 * Consume: le controller consomme depuis son buffer interne.
 * Detecte l'etat "honey depleted" et notifie les viewers.
 */
public class HoneyEnergyManager {
    private static final int HONEY_CONSUME_INTERVAL = 20;
    private static final int HONEY_PULL_INTERVAL = 10;

    private final StorageControllerBlockEntity parent;
    private boolean honeyDepleted = false;

    public HoneyEnergyManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    public boolean isHoneyDepleted() {
        return honeyDepleted;
    }

    public void setHoneyDepleted(boolean depleted) {
        this.honeyDepleted = depleted;
    }

    /**
     * Tick de la consommation de miel.
     * Pull depuis les reservoirs et consomme depuis le buffer interne.
     */
    public void tickHoneyConsumption(long gameTick) {
        long offset = parent.getBlockPos().hashCode();
        if ((gameTick + offset) % HONEY_PULL_INTERVAL == 0) {
            pullHoneyFromReservoirs();
        }
        if ((gameTick + offset) % HONEY_CONSUME_INTERVAL == 0) {
            consumeHoney();
        }
    }

    /**
     * Transfere du miel depuis les 2 HoneyReservoirs physiques vers le buffer du controller.
     * Remplit jusqu'a la capacite max du buffer.
     */
    private void pullHoneyFromReservoirs() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        int space = parent.getHoneyCapacity() - parent.getHoneyStored();
        if (space <= 0) return;

        int pulled = 0;
        int rotation = parent.getMultiblockManager().getRotation();
        for (int xOff : new int[]{-1, 1}) {
            if (pulled >= space) break;
            Vec3i rotatedOffset = MultiblockPattern.rotateY(new Vec3i(xOff, 0, 0), rotation);
            BlockPos reservoirPos = parent.getBlockPos().offset(rotatedOffset);
            if (!parent.getLevel().hasChunkAt(reservoirPos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(reservoirPos);
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                FluidStack drained = reservoir.drain(space - pulled, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    pulled += drained.getAmount();
                }
            }
        }

        if (pulled > 0) {
            parent.setHoneyStored(parent.getHoneyStored() + pulled);
            parent.setChanged();
        }
    }

    /**
     * Consomme du miel depuis le buffer interne du controller.
     * Notifie les viewers quand l'etat change (depleted/restored).
     */
    private void consumeHoney() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        boolean isDaytime = parent.getLevel().isDay();
        int consumptionPerSecond = ControllerStats.getHoneyConsumption(
            parent.getEssenceSlots(), parent.getNetworkRegistry().getChestCount(),
            parent.getHiveMultiplier(), parent.getRelayCount(),
            parent.getInterfaceRelayCost(), isDaytime);

        int stored = parent.getHoneyStored();
        int consumed = Math.min(consumptionPerSecond, stored);
        parent.setHoneyStored(stored - consumed);

        boolean wasDepleted = honeyDepleted;
        honeyDepleted = consumed < consumptionPerSecond;
        if (!wasDepleted && honeyDepleted) {
            parent.notifyViewers(Component.translatable("gui.beemancer.network.honey_depleted"));
            parent.syncToClient();
        } else if (wasDepleted && !honeyDepleted) {
            parent.notifyViewers(Component.translatable("gui.beemancer.network.honey_restored"));
            parent.syncToClient();
        }
    }
}
