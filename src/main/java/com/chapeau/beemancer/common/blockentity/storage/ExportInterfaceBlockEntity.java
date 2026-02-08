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
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.InterfaceTask;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

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
        super(BeemancerBlockEntities.EXPORT_INTERFACE.get(), pos, state);
    }

    // === Scan Logic ===

    @Override
    protected void doScan(Container adjacent, StorageControllerBlockEntity controller) {
        cleanupDeliveredTasks();
        int beeCapacity = controller.getBeeCapacity();

        if (filters.isEmpty()) {
            doScanNoFilter(adjacent, controller, beeCapacity);
        } else {
            for (InterfaceFilter filter : filters) {
                doScanWithFilter(adjacent, controller, filter, beeCapacity);
            }
        }

        publishTodoTasks(controller);
    }

    /**
     * Sans filtre: exporter tous les items des globalSelectedSlots.
     */
    private void doScanNoFilter(Container adjacent, StorageControllerBlockEntity controller,
                                 int beeCapacity) {
        int[] slots = getGlobalOperableSlots(adjacent);
        createExportTasks(adjacent, slots, 0, beeCapacity, null);
    }

    /**
     * Avec filtre: exporter les items qui matchent, en respectant la quantite.
     */
    private void doScanWithFilter(Container adjacent, StorageControllerBlockEntity controller,
                                   InterfaceFilter filter, int beeCapacity) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int keepQty = filter.getQuantity();
        createExportTasks(adjacent, slots, keepQty, beeCapacity, filter);
    }

    /**
     * Cree des InterfaceTasks pour chaque type d'item a exporter.
     * Split en chunks de beeCapacity.
     *
     * @param filter si non-null, seuls les items matchant ce filtre sont exportes
     * @param keepQty 0=tout exporter, N=garder N items
     */
    private void createExportTasks(Container adjacent, int[] slots,
                                    int keepQty, int beeCapacity,
                                    InterfaceFilter filter) {
        Map<String, ItemStack> templatesByKey = new LinkedHashMap<>();
        Map<String, Integer> totalCountsByKey = new LinkedHashMap<>();

        for (int slot : slots) {
            ItemStack stack = adjacent.getItem(slot);
            if (stack.isEmpty()) continue;

            if (filter != null && !filter.matches(stack, false)) continue;

            String key = itemKey(stack);
            templatesByKey.putIfAbsent(key, stack.copyWithCount(1));
            totalCountsByKey.merge(key, stack.getCount(), Integer::sum);
        }

        for (Map.Entry<String, ItemStack> entry : templatesByKey.entrySet()) {
            ItemStack template = entry.getValue();
            int totalCount = totalCountsByKey.get(entry.getKey());

            int lockedCount = getLockedCount(template);
            int exportable = totalCount - keepQty - lockedCount;

            if (exportable <= 0) {
                cancelExcessTodoTasks(template);
                continue;
            }

            int todoCount = getTodoCount(template);
            int toCreate = exportable - todoCount;

            if (toCreate <= 0) continue;

            int itemBeeCapacity = Math.min(beeCapacity, template.getMaxStackSize());
            while (toCreate > 0) {
                int chunk = Math.min(toCreate, itemBeeCapacity);
                tasks.add(new InterfaceTask(InterfaceTask.TaskType.EXPORT, template, chunk));
                toCreate -= chunk;
            }
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
