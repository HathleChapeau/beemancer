/**
 * ============================================================
 * [ImportInterfaceBlockEntity.java]
 * Description: Interface d'import - restock l'inventaire adjacent depuis le reseau
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceBlockEntity   | Parent abstrait      | Filtres, controller, scan      |
 * | IDeliveryEndpoint             | Reception livraison  | Abeilles livrent ici           |
 * | StorageControllerBlockEntity  | Controller lie       | Assignation bees, depot        |
 * | InterfaceTask                 | Tache unitaire       | Import par chunks bee-sized    |
 * | InterfaceFilter               | Filtre individuel    | Logique per-filter             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ImportInterfaceBlock.java (creation BlockEntity)
 * - DeliveryPhaseGoal.java (livraison polymorphe via IDeliveryEndpoint)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.InterfaceTask;
import com.chapeau.beemancer.core.util.ContainerHelper;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Import Interface: scanne l'inventaire adjacent et cree des InterfaceTasks
 * pour remplir l'inventaire avec les items filtres depuis le reseau.
 *
 * Comportement quantite:
 * - quantite=0: importer jusqu'a remplir les slots
 * - quantite=N: importer jusqu'a avoir N items de ce type
 *
 * Sans filtre: rien a importer (ne sait pas quoi chercher).
 *
 * Implemente IDeliveryEndpoint pour recevoir les items livres par les abeilles
 * et les inserer dans l'inventaire adjacent.
 *
 * Utilise InterfaceTask (NEEDED/LOCKED/DELIVERED) pour un suivi precis
 * avec adaptation dynamique du count.
 */
public class ImportInterfaceBlockEntity extends NetworkInterfaceBlockEntity implements IDeliveryEndpoint {

    public ImportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.IMPORT_INTERFACE.get(), pos, state);
    }

    // === IDeliveryEndpoint ===

    @Override
    public ItemStack receiveDeliveredItems(ItemStack items) {
        IItemHandler adjacent = getAdjacentItemHandler();
        if (adjacent == null) return items;

        ItemStack remaining = items.copy();

        // Determiner les slots cibles en trouvant le filtre correspondant a l'item livre
        int[] slots;
        if (filterManager.isEmpty()) {
            slots = getGlobalOperableSlots(adjacent);
        } else {
            InterfaceFilter matchingFilter = null;
            for (InterfaceFilter filter : filterManager.getFilters()) {
                if (filter.matches(items, false)) {
                    matchingFilter = filter;
                    break;
                }
            }

            if (matchingFilter != null && !matchingFilter.getSelectedSlots().isEmpty()) {
                slots = getOperableSlots(adjacent, matchingFilter.getSelectedSlots());
            } else {
                slots = getGlobalOperableSlots(adjacent);
            }
        }

        return insertIntoSlots(adjacent, remaining, slots);
    }

    private ItemStack insertIntoSlots(IItemHandler adjacent, ItemStack remaining, int[] slots) {
        remaining = ContainerHelper.insertItem(adjacent, remaining, slots);

        // Si reste non-insere, remettre dans le reseau
        if (!remaining.isEmpty()) {
            StorageControllerBlockEntity controller = getController();
            if (controller != null) {
                remaining = controller.depositItemForDelivery(remaining, null);
            }
        }

        return remaining;
    }

    // === Scan Logic ===

    @Override
    protected void doScan(IItemHandler adjacent, StorageControllerBlockEntity controller) {
        taskManager.cleanupDeliveredTasks();
        int beeCapacity = controller.getBeeCapacity();

        if (filterManager.isEmpty()) {
            taskManager.cleanupOrphanedTasks(new HashSet<>());
            return;
        }

        Set<String> activeItemKeys = new HashSet<>();

        for (InterfaceFilter filter : filterManager.getFilters()) {
            doScanWithFilter(adjacent, controller, filter, beeCapacity, activeItemKeys);
        }

        taskManager.cleanupOrphanedTasks(activeItemKeys);
        taskManager.publishTodoTasks(controller);
    }

    /**
     * Avec filtre: reconcilie les InterfaceTasks d'import pour les items matchant.
     */
    private void doScanWithFilter(IItemHandler adjacent, StorageControllerBlockEntity controller,
                                   InterfaceFilter filter, int beeCapacity,
                                   Set<String> activeItemKeys) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int targetQty = filter.getQuantity();

        if (filter.getMode() == InterfaceFilter.FilterMode.ITEM) {
            scanItemMode(adjacent, filter, slots, beeCapacity, targetQty, activeItemKeys);
        } else {
            scanTextMode(adjacent, controller, filter, slots, beeCapacity, targetQty, activeItemKeys);
        }
    }

    /**
     * Mode ITEM: itere les slots du filtre pour trouver les items a importer.
     */
    private void scanItemMode(IItemHandler adjacent, InterfaceFilter filter,
                               int[] slots, int beeCapacity, int targetQty,
                               Set<String> activeItemKeys) {
        for (int i = 0; i < InterfaceFilter.SLOTS_PER_FILTER; i++) {
            ItemStack filterItem = filter.getItem(i);
            if (filterItem.isEmpty()) continue;

            reconcileImportItem(adjacent, filterItem, slots, beeCapacity, targetQty, activeItemKeys);
        }
    }

    /**
     * Mode TEXT: cherche dans le reseau les items matchant le filtre texte.
     */
    private void scanTextMode(IItemHandler adjacent, StorageControllerBlockEntity controller,
                               InterfaceFilter filter, int[] slots, int beeCapacity,
                               int targetQty, Set<String> activeItemKeys) {
        List<ItemStack> networkItems = controller.getAggregatedItems();

        for (ItemStack networkItem : networkItems) {
            if (!filter.matches(networkItem, false)) continue;

            reconcileImportItem(adjacent, networkItem, slots, beeCapacity, targetQty, activeItemKeys);
        }
    }

    /**
     * Reconcilie les InterfaceTasks pour un item specifique a importer.
     * Calcule le needed reel et delegue a reconcileTasksForItem().
     *
     * @param targetQty 0=remplir les slots, N=importer jusqu'a N items
     */
    private void reconcileImportItem(IItemHandler adjacent, ItemStack filterItem,
                                      int[] slots, int beeCapacity, int targetQty,
                                      Set<String> activeItemKeys) {
        String key = itemKey(filterItem);
        activeItemKeys.add(key);

        int currentCount = ContainerHelper.countItem(adjacent, filterItem, slots);

        int needed;
        if (targetQty == 0) {
            int capacity = ContainerHelper.calculateCapacity(adjacent, filterItem, slots);
            needed = capacity - currentCount;
        } else {
            needed = targetQty - currentCount;
        }

        int inTransit = getLockedCount(filterItem);
        needed = Math.max(0, needed - inTransit);

        taskManager.reconcileTasksForItem(filterItem, needed, beeCapacity,
            InterfaceTask.TaskType.IMPORT);
    }

    // === NBT Hooks ===

    @Override
    protected void saveExtra(CompoundTag tag, HolderLookup.Provider registries) {
        // Pas de donnees extra specifiques a l'import
    }

    @Override
    protected void loadExtra(CompoundTag tag, HolderLookup.Provider registries) {
        // Pas de donnees extra specifiques a l'import
    }
}
