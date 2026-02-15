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
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import net.minecraft.network.chat.Component;

/**
 * Gere la consommation de miel du controller via un buffer interne.
 * Les pipes remplissent le buffer directement via les reservoirs (capability delegation).
 * Ce manager consomme depuis le buffer interne et detecte l'etat "honey depleted".
 */
public class HoneyEnergyManager {
    private static final int HONEY_CONSUME_INTERVAL = 20;

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
     * Les pipes remplissent le buffer directement via les reservoirs (capability delegation).
     * Ce tick consomme depuis le buffer interne.
     */
    public void tickHoneyConsumption(long gameTick) {
        long offset = parent.getBlockPos().hashCode();
        if ((gameTick + offset) % HONEY_CONSUME_INTERVAL == 0) {
            consumeHoney();
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
