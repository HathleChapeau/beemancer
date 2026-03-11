/**
 * ============================================================
 * [AbstractInterfaceTaskHandler.java]
 * Description: Classe parent pour la gestion unifiée des tâches IMPORT/EXPORT
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageControllerBlockEntity   | Accès réseau         | Registry, chests, items   |
 * | NetworkInterfaceBlockEntity    | Interface source     | Position, adjacent        |
 * | InterfaceTask                  | Tâche à traiter      | Template, count, type     |
 * | DeliveryTask                   | Tâche de livraison   | Création pour la bee      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ImportTaskHandler.java
 * - ExportTaskHandler.java
 * - StorageControllerBlockEntity.java (via factory)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage.task;

import com.chapeau.apica.common.block.storage.DeliveryTask;
import com.chapeau.apica.common.block.storage.InterfaceTask;
import com.chapeau.apica.common.blockentity.storage.DeliveryContainerOps;
import com.chapeau.apica.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageControllerBlockEntity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe abstraite définissant le comportement commun pour la gestion des tâches d'interface.
 * Les sous-classes implémentent la logique spécifique pour IMPORT et EXPORT.
 *
 * Pattern Template Method:
 * - assignTask() définit le flux commun
 * - findSourcePos() et findDestPos() sont implémentées par les sous-classes
 */
public abstract class AbstractInterfaceTaskHandler {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractInterfaceTaskHandler.class);

    protected final StorageControllerBlockEntity controller;
    protected final DeliveryContainerOps containerOps;

    protected AbstractInterfaceTaskHandler(StorageControllerBlockEntity controller,
                                           DeliveryContainerOps containerOps) {
        this.controller = controller;
        this.containerOps = containerOps;
    }

    /**
     * Assigne une bee à une InterfaceTask.
     * Flux commun: trouver source, trouver dest, valider, créer DeliveryTask.
     *
     * @param iface l'interface qui a créé la tâche
     * @param task la tâche à assigner
     * @return true si la tâche a été assignée avec succès
     */
    public boolean assignTask(NetworkInterfaceBlockEntity iface, InterfaceTask task) {
        if (controller.getLevel() == null) return false;

        // Trouver les positions source et destination (implémentation spécifique)
        BlockPos sourcePos = findSourcePos(iface, task);
        BlockPos destPos = findDestPos(iface, task);

        // Validation commune
        if (sourcePos == null || destPos == null) {
            LOGGER.debug("[TaskHandler] {} task {}: source or dest is null (source={}, dest={})",
                getTaskTypeName(), task.getTaskId(), sourcePos, destPos);
            return false;
        }

        // Éviter les livraisons vers la même position
        if (sourcePos.equals(destPos)) {
            LOGGER.debug("[TaskHandler] {} task {}: source equals dest ({})",
                getTaskTypeName(), task.getTaskId(), sourcePos);
            return false;
        }

        // Éviter les livraisons depuis/vers le coffre adjacent de l'interface
        if (sourcePos.equals(iface.getAdjacentPos()) && destPos.equals(iface.getAdjacentPos())) {
            LOGGER.debug("[TaskHandler] {} task {}: both source and dest are adjacent chest",
                getTaskTypeName(), task.getTaskId());
            return false;
        }

        // Créer la DeliveryTask
        DeliveryTask deliveryTask = createDeliveryTask(iface, task, sourcePos, destPos);

        // Ajouter au delivery manager et verrouiller l'InterfaceTask
        controller.getDeliveryManager().addDeliveryTask(deliveryTask);
        task.lockTask(deliveryTask.getTaskId(), controller.getLevel().getGameTime());

        LOGGER.debug("[TaskHandler] {} task {} assigned: {} → {}",
            getTaskTypeName(), task.getTaskId(), sourcePos, destPos);

        return true;
    }

    /**
     * Crée une DeliveryTask à partir des informations de l'InterfaceTask.
     * Comportement commun pour IMPORT et EXPORT.
     */
    protected DeliveryTask createDeliveryTask(NetworkInterfaceBlockEntity iface,
                                               InterfaceTask task,
                                               BlockPos sourcePos,
                                               BlockPos destPos) {
        return DeliveryTask.builder(
                task.getTemplate(),
                task.getCount(),
                sourcePos,
                destPos,
                DeliveryTask.TaskOrigin.AUTOMATION
            )
            .requesterPos(iface.getBlockPos())
            .interfaceTaskId(task.getTaskId())
            .interfacePos(iface.getBlockPos())
            .build();
    }

    /**
     * Trouve la position source pour la tâche.
     * Implémentation spécifique selon le type (IMPORT/EXPORT).
     *
     * @return position source, ou null si non trouvée
     */
    @Nullable
    protected abstract BlockPos findSourcePos(NetworkInterfaceBlockEntity iface, InterfaceTask task);

    /**
     * Trouve la position destination pour la tâche.
     * Implémentation spécifique selon le type (IMPORT/EXPORT).
     *
     * @return position destination, ou null si non trouvée
     */
    @Nullable
    protected abstract BlockPos findDestPos(NetworkInterfaceBlockEntity iface, InterfaceTask task);

    /**
     * Retourne le nom du type de tâche pour les logs.
     */
    protected abstract String getTaskTypeName();
}
