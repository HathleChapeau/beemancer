/**
 * ============================================================
 * [ImportTaskHandler.java]
 * Description: Gestion des tâches IMPORT (réseau → coffre adjacent)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | AbstractInterfaceTaskHandler   | Classe parent        | Logique commune           |
 * | DeliveryContainerOps           | Opérations coffres   | findChestWithItem         |
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
 * Handler pour les tâches IMPORT.
 *
 * Flux IMPORT:
 * - Source: coffre du réseau contenant l'item demandé
 * - Destination: l'interface elle-même (qui dépose dans le coffre adjacent)
 *
 * La bee extrait du réseau et livre à l'interface.
 */
public class ImportTaskHandler extends AbstractInterfaceTaskHandler {

    public ImportTaskHandler(StorageControllerBlockEntity controller,
                             DeliveryContainerOps containerOps) {
        super(controller, containerOps);
    }

    /**
     * IMPORT: source = coffre du réseau avec l'item.
     * Cherche un coffre dans le réseau qui contient l'item demandé.
     */
    @Override
    @Nullable
    protected BlockPos findSourcePos(NetworkInterfaceBlockEntity iface, InterfaceTask task) {
        BlockPos chest = containerOps.findChestWithItem(task.getTemplate(), 1);

        // Ne pas prendre du coffre adjacent de cette interface (boucle infinie)
        if (chest != null && chest.equals(iface.getAdjacentPos())) {
            LOGGER.debug("[Import] Skipping adjacent chest {} as source", chest);
            return null;
        }

        return chest;
    }

    /**
     * IMPORT: destination = l'interface elle-même.
     * L'interface reçoit les items et les dépose dans son coffre adjacent.
     */
    @Override
    @Nullable
    protected BlockPos findDestPos(NetworkInterfaceBlockEntity iface, InterfaceTask task) {
        return iface.getBlockPos();
    }

    @Override
    protected String getTaskTypeName() {
        return "IMPORT";
    }
}
