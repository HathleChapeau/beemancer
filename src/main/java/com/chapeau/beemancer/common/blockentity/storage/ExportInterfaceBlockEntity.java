/**
 * ============================================================
 * [ExportInterfaceBlockEntity.java]
 * Description: Interface d'export - exporte les items de l'inventaire adjacent vers un coffre du reseau
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceBlockEntity   | Parent abstrait      | Filtres, controller, scan      |
 * | StorageControllerBlockEntity  | Controller lie       | Taches de livraison, depot     |
 * | ControllerStats               | Quantite max         | Limite par tache               |
 * | InterfaceFilter               | Filtre individuel    | Logique per-filter             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ExportInterfaceBlock.java (creation BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
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
 * Export Interface: scanne l'inventaire adjacent et cree des taches EXTRACT
 * pour qu'une abeille aille physiquement chercher les items, puis les depose
 * dans un coffre du reseau ayant de la place.
 *
 * Comportement quantite:
 * - quantite=0: exporter tout
 * - quantite=N: exporter jusqu'a ne garder que N items
 *
 * Sans filtre: exporter tous les items des globalSelectedSlots.
 *
 * L'abeille fait le trajet complet:
 * controller → adjacent (extraction) → controller → coffre destination (depot) → controller
 */
public class ExportInterfaceBlockEntity extends NetworkInterfaceBlockEntity {

    public ExportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.EXPORT_INTERFACE.get(), pos, state);
    }

    // === Scan Logic ===

    @Override
    protected void doScan(Container adjacent, StorageControllerBlockEntity controller) {
        int maxQuantity = ControllerStats.getQuantity(controller.getEssenceSlots());

        if (filters.isEmpty()) {
            doScanNoFilter(adjacent, controller, maxQuantity);
        } else {
            for (InterfaceFilter filter : filters) {
                doScanWithFilter(adjacent, controller, filter, maxQuantity);
            }
        }
    }

    /**
     * Sans filtre: exporter tous les items des globalSelectedSlots.
     */
    private void doScanNoFilter(Container adjacent, StorageControllerBlockEntity controller,
                                 int maxQuantity) {
        int[] slots = getGlobalOperableSlots(adjacent);
        createExportTasks(adjacent, controller, slots, 0, maxQuantity, null);
    }

    /**
     * Avec filtre: exporter les items qui matchent, en respectant la quantite.
     */
    private void doScanWithFilter(Container adjacent, StorageControllerBlockEntity controller,
                                   InterfaceFilter filter, int maxQuantity) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int keepQty = filter.getQuantity();
        createExportTasks(adjacent, controller, slots, keepQty, maxQuantity, filter);
    }

    /**
     * Cree des taches EXTRACT pour chaque type d'item a exporter.
     * L'abeille va a l'inventaire adjacent, prend les items, puis vole
     * jusqu'au coffre destination dans le reseau pour les deposer.
     *
     * @param filter si non-null, seuls les items matchant ce filtre sont exportes
     * @param keepQty 0=tout exporter, N=garder N items
     */
    private void createExportTasks(Container adjacent, StorageControllerBlockEntity controller,
                                    int[] slots, int keepQty, int maxQuantity,
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

        BlockPos adjacentPos = getAdjacentPos();

        for (Map.Entry<String, ItemStack> entry : templatesByKey.entrySet()) {
            String key = entry.getKey();
            ItemStack template = entry.getValue();

            if (pendingTasks.containsKey(key)) continue;

            int totalCount = totalCountsByKey.get(key);
            int exportable = totalCount - keepQty;
            if (exportable <= 0) continue;

            int toExport = Math.min(exportable, maxQuantity);

            // Trouver un coffre du reseau avec de la place
            BlockPos destChest = controller.findChestWithSpace(template, toExport);
            if (destChest == null) continue;

            // Loop prevention: ne pas exporter vers le bloc adjacent lui-meme
            if (destChest.equals(adjacentPos)) continue;

            // EXTRACT: abeille va a l'adjacent, extrait, puis vole au coffre destination
            DeliveryTask task = new DeliveryTask(
                template, toExport, adjacentPos, destChest,
                DeliveryTask.DeliveryType.EXTRACT
            );
            controller.addDeliveryTask(task);
            pendingTasks.put(key, task.getTaskId());
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
