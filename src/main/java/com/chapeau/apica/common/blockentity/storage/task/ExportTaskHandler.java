/**
 * ============================================================
 * [ExportTaskHandler.java]
 * Description: Gestion des tâches EXPORT (coffre adjacent → réseau)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | AbstractInterfaceTaskHandler   | Classe parent        | Logique commune           |
 * | StorageItemAggregator          | Agrégation items     | findSlotForItem           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InterfaceTaskHandlerFactory.java
 * - StorageControllerBlockEntity.java
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage.task;

import com.chapeau.apica.common.block.storage.InterfaceTask;
import com.chapeau.apica.common.blockentity.storage.DeliveryContainerOps;
import com.chapeau.apica.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageControllerBlockEntity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Handler pour les tâches EXPORT.
 *
 * Flux EXPORT:
 * - Source: coffre adjacent de l'interface (hors réseau)
 * - Destination: coffre du réseau avec de l'espace
 *
 * La bee extrait du coffre adjacent et livre au réseau.
 */
public class ExportTaskHandler extends AbstractInterfaceTaskHandler {

    public ExportTaskHandler(StorageControllerBlockEntity controller,
                             DeliveryContainerOps containerOps) {
        super(controller, containerOps);
    }

    /**
     * EXPORT: source = coffre adjacent de l'interface.
     * C'est le coffre physiquement à côté de l'interface, hors du réseau.
     */
    @Override
    @Nullable
    protected BlockPos findSourcePos(NetworkInterfaceBlockEntity iface, InterfaceTask task) {
        BlockPos adjacentPos = iface.getAdjacentPos();

        if (adjacentPos == null) {
            LOGGER.debug("[Export] Interface {} has no adjacent chest", iface.getBlockPos());
            return null;
        }

        return adjacentPos;
    }

    /**
     * EXPORT: destination = coffre du réseau avec de l'espace.
     * Cherche un coffre dans le réseau qui peut recevoir l'item.
     */
    @Override
    @Nullable
    protected BlockPos findDestPos(NetworkInterfaceBlockEntity iface, InterfaceTask task) {
        BlockPos dest = controller.getItemAggregator().findSlotForItem(task.getTemplate());

        // Ne pas déposer dans le coffre adjacent de cette interface (boucle infinie)
        if (dest != null && dest.equals(iface.getAdjacentPos())) {
            LOGGER.debug("[Export] Skipping adjacent chest {} as dest", dest);
            return null;
        }

        return dest;
    }

    @Override
    protected String getTaskTypeName() {
        return "EXPORT";
    }
}
