/**
 * ============================================================
 * [InterfaceTaskHandlerFactory.java]
 * Description: Factory pour obtenir le handler approprié selon le type de tâche
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | AbstractInterfaceTaskHandler   | Type retourné        | Polymorphisme             |
 * | ImportTaskHandler              | Handler IMPORT       | Création instance         |
 * | ExportTaskHandler              | Handler EXPORT       | Création instance         |
 * | InterfaceTask.TaskType         | Type de tâche        | Switch selection          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage.task;

import com.chapeau.apica.common.block.storage.InterfaceTask;
import com.chapeau.apica.common.blockentity.storage.DeliveryContainerOps;
import com.chapeau.apica.common.blockentity.storage.StorageControllerBlockEntity;

/**
 * Factory pour créer le handler approprié selon le type de tâche.
 * Permet d'utiliser le polymorphisme pour traiter IMPORT et EXPORT de manière uniforme.
 *
 * Usage:
 * <pre>
 * AbstractInterfaceTaskHandler handler = InterfaceTaskHandlerFactory.getHandler(
 *     task.getType(), controller, containerOps
 * );
 * handler.assignTask(iface, task);
 * </pre>
 */
public final class InterfaceTaskHandlerFactory {

    private InterfaceTaskHandlerFactory() {
        // Utility class
    }

    /**
     * Retourne le handler approprié pour le type de tâche donné.
     *
     * @param taskType type de la tâche (IMPORT ou EXPORT)
     * @param controller le controller du réseau
     * @param containerOps opérations sur les conteneurs
     * @return le handler approprié
     */
    public static AbstractInterfaceTaskHandler getHandler(
            InterfaceTask.TaskType taskType,
            StorageControllerBlockEntity controller,
            DeliveryContainerOps containerOps) {

        return switch (taskType) {
            case IMPORT -> new ImportTaskHandler(controller, containerOps);
            case EXPORT -> new ExportTaskHandler(controller, containerOps);
        };
    }
}
