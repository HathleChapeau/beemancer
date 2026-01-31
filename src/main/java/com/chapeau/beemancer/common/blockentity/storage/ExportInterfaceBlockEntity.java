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
 * | StorageControllerBlockEntity  | Controller lie       | Taches de livraison            |
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
 * Export Interface: scanne l'inventaire adjacent et exporte les items
 * correspondant aux filtres (ou tous si aucun filtre) vers le reseau.
 *
 * Comportement quantite:
 * - quantite=0: exporter tout
 * - quantite=N: exporter jusqu'a ne garder que N items
 *
 * Sans filtre: exporter tous les items des globalSelectedSlots.
 *
 * Les items sont retires immediatement de l'inventaire adjacent et
 * une tache DEPOSIT est creee pour les transporter vers un coffre du reseau.
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
        exportFromSlots(adjacent, controller, slots, 0, maxQuantity, null);
    }

    /**
     * Avec filtre: exporter les items qui matchent, en respectant la quantite.
     */
    private void doScanWithFilter(Container adjacent, StorageControllerBlockEntity controller,
                                   InterfaceFilter filter, int maxQuantity) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int keepQty = filter.getQuantity();
        exportFromSlots(adjacent, controller, slots, keepQty, maxQuantity, filter);
    }

    /**
     * Logique d'export commune.
     * @param filter si non-null, seuls les items matchant ce filtre sont exportes
     * @param keepQty 0=tout exporter, N=garder N items
     */
    private void exportFromSlots(Container adjacent, StorageControllerBlockEntity controller,
                                  int[] slots, int keepQty, int maxQuantity,
                                  InterfaceFilter filter) {
        // Agreger les counts par type d'item
        Map<String, ItemStack> templatesByKey = new LinkedHashMap<>();
        Map<String, Integer> totalCountsByKey = new LinkedHashMap<>();

        for (int slot : slots) {
            ItemStack stack = adjacent.getItem(slot);
            if (stack.isEmpty()) continue;

            // Si filtre actif, verifier le match
            if (filter != null) {
                if (!filter.matches(stack, false)) continue;
            }

            String key = itemKey(stack);
            templatesByKey.putIfAbsent(key, stack.copyWithCount(1));
            totalCountsByKey.merge(key, stack.getCount(), Integer::sum);
        }

        // Pour chaque type d'item, calculer l'exportable et exporter
        for (Map.Entry<String, ItemStack> entry : templatesByKey.entrySet()) {
            String key = entry.getKey();
            ItemStack template = entry.getValue();

            if (pendingTasks.containsKey(key)) continue;

            int totalCount = totalCountsByKey.get(key);
            int exportable = totalCount - keepQty;
            if (exportable <= 0) continue;

            int toExport = Math.min(exportable, maxQuantity);

            BlockPos chestPos = controller.findSlotForItem(template);
            if (chestPos == null) continue;

            // Retirer les items des slots
            int leftToExtract = toExport;
            for (int slot : slots) {
                if (leftToExtract <= 0) break;
                ItemStack stack = adjacent.getItem(slot);
                if (stack.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(stack, template)) continue;

                int take = Math.min(leftToExtract, stack.getCount());
                stack.shrink(take);
                if (stack.isEmpty()) {
                    adjacent.setItem(slot, ItemStack.EMPTY);
                }
                leftToExtract -= take;
            }
            adjacent.setChanged();

            int actualExported = toExport - leftToExtract;
            if (actualExported <= 0) continue;

            DeliveryTask task = new DeliveryTask(
                template, actualExported, chestPos, worldPosition,
                DeliveryTask.DeliveryType.DEPOSIT
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
