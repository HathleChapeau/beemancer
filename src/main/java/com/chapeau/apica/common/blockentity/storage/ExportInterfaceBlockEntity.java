/**
 * ============================================================
 * [ExportInterfaceBlockEntity.java]
 * Description: Interface d'export - exporte les items de l'inventaire adjacent vers le reseau
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceBlockEntity   | Parent abstrait      | Filtres, controller, scan      |
 * | StorageControllerBlockEntity  | Controller lie       | Assignation bees, depot        |
 * | InterfaceTask                 | Tache unitaire       | Export par chunks bee-sized     |
 * | InterfaceFilter               | Filtre individuel    | Logique per-filter             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ExportInterfaceBlock.java (creation BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.InterfaceTask;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Export Interface: scanne l'inventaire adjacent et cree des InterfaceTasks
 * pour qu'une abeille aille chercher les items et les depose dans le reseau.
 *
 * Comportement quantite:
 * - quantite=0: exporter tout
 * - quantite=N: exporter jusqu'a ne garder que N items
 *
 * Sans filtre: exporter tous les items des globalSelectedSlots.
 *
 * Utilise InterfaceTask (NEEDED/LOCKED/DELIVERED) pour un suivi precis
 * avec adaptation dynamique du count.
 */
public class ExportInterfaceBlockEntity extends NetworkInterfaceBlockEntity {

    public ExportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.EXPORT_INTERFACE.get(), pos, state);
    }

    // === Scan Logic ===

    @Override
    protected void doScan(IItemHandler adjacent, StorageControllerBlockEntity controller) {
        taskManager.cleanupDeliveredTasks();
        int beeCapacity = controller.getBeeCapacity();

        Set<String> activeItemKeys = new HashSet<>();

        if (filterManager.isEmpty()) {
            doScanNoFilter(adjacent, controller, beeCapacity, activeItemKeys);
        } else {
            for (InterfaceFilter filter : filterManager.getFilters()) {
                doScanWithFilter(adjacent, controller, filter, beeCapacity, activeItemKeys);
            }
        }

        taskManager.cleanupOrphanedTasks(activeItemKeys);
        taskManager.publishTodoTasks(controller);
    }

    /**
     * Sans filtre: exporter tous les items des globalSelectedSlots.
     */
    private void doScanNoFilter(IItemHandler adjacent, StorageControllerBlockEntity controller,
                                 int beeCapacity, Set<String> activeItemKeys) {
        int[] slots = getGlobalOperableSlots(adjacent);
        reconcileExportItems(adjacent, controller, slots, 0, beeCapacity, null, activeItemKeys);
    }

    /**
     * Avec filtre: exporter les items qui matchent, en respectant la quantite.
     */
    private void doScanWithFilter(IItemHandler adjacent, StorageControllerBlockEntity controller,
                                   InterfaceFilter filter, int beeCapacity,
                                   Set<String> activeItemKeys) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int keepQty = filter.getQuantity();
        reconcileExportItems(adjacent, controller, slots, keepQty, beeCapacity, filter, activeItemKeys);
    }

    /**
     * Reconcilie les InterfaceTasks pour chaque type d'item a exporter.
     * Met a jour les tasks existantes (LOCKED et NEEDED) au lieu de creer
     * de nouvelles tasks a chaque scan.
     *
     * Verifie que le reseau a de l'espace avant de creer des tasks.
     *
     * @param filter si non-null, seuls les items matchant ce filtre sont exportes
     * @param keepQty 0=tout exporter, N=garder N items
     * @param activeItemKeys set des itemKeys actifs (rempli par cette methode)
     */
    private void reconcileExportItems(IItemHandler adjacent, StorageControllerBlockEntity controller,
                                       int[] slots, int keepQty, int beeCapacity,
                                       InterfaceFilter filter,
                                       Set<String> activeItemKeys) {
        Map<String, ItemStack> templatesByKey = new LinkedHashMap<>();
        Map<String, Integer> totalCountsByKey = new LinkedHashMap<>();

        for (int slot : slots) {
            if (slot < 0 || slot >= adjacent.getSlots()) continue;
            ItemStack stack = adjacent.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            if (filter != null && !filter.matches(stack, false)) continue;

            String key = itemKey(stack);
            templatesByKey.putIfAbsent(key, stack.copyWithCount(1));
            totalCountsByKey.merge(key, stack.getCount(), Integer::sum);
        }

        // [FIX] Position du coffre adjacent: ne peut pas etre une destination d'export
        // (on ne peut pas exporter vers le coffre d'ou on exporte)
        BlockPos adjacentPos = getAdjacentPos();

        for (Map.Entry<String, ItemStack> entry : templatesByKey.entrySet()) {
            String key = entry.getKey();
            ItemStack template = entry.getValue();
            int totalCount = totalCountsByKey.get(key);
            activeItemKeys.add(key);

            // [FIX] Verifier que le reseau a de l'espace pour cet item
            // ET que la destination n'est pas le coffre adjacent (self-export interdit)
            BlockPos dest = controller.getItemAggregator().findSlotForItem(template);
            if (dest == null || dest.equals(adjacentPos)) {
                taskManager.reconcileTasksForItem(template, 0, beeCapacity,
                    InterfaceTask.TaskType.EXPORT);
                continue;
            }

            // NOTE: Ne pas soustraire inTransit ici! reconcileTasksForItem() distribue
            // la demande totale (exportable) entre les tasks LOCKED et NEEDED.
            // Soustraire inTransit causerait une double soustraction: les LOCKED auraient
            // leur count mis à 0 et seraient annulées alors qu'elles sont en vol.
            int exportable = Math.max(0, totalCount - keepQty);

            taskManager.reconcileTasksForItem(template, exportable, beeCapacity,
                InterfaceTask.TaskType.EXPORT);
        }
    }

    // === NBT Hooks ===

    @Override
    protected void saveExtra(CompoundTag tag, HolderLookup.Provider registries) {
        // Pas de donnees extra specifiques a l'export
    }

    @Override
    protected void loadExtra(CompoundTag tag, HolderLookup.Provider registries) {
        // Pas de donnees extra specifiques a l'export
    }
}
