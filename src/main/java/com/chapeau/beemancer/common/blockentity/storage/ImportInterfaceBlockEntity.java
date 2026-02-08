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
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

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
        Container adjacent = getAdjacentInventory();
        if (adjacent == null) return items;

        ItemStack remaining = items.copy();

        // Determiner les slots cibles en trouvant le filtre correspondant a l'item livre
        int[] slots;
        if (filters.isEmpty()) {
            slots = getGlobalOperableSlots(adjacent);
        } else {
            InterfaceFilter matchingFilter = null;
            for (InterfaceFilter filter : filters) {
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

    private ItemStack insertIntoSlots(Container adjacent, ItemStack remaining, int[] slots) {
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
    protected void doScan(Container adjacent, StorageControllerBlockEntity controller) {
        cleanupDeliveredTasks();
        int beeCapacity = controller.getBeeCapacity();

        if (filters.isEmpty()) return; // Import sans filtre = rien a importer

        for (InterfaceFilter filter : filters) {
            doScanWithFilter(adjacent, controller, filter, beeCapacity);
        }

        publishTodoTasks(controller);
    }

    /**
     * Avec filtre: chercher les items qui matchent et creer des InterfaceTasks d'import.
     */
    private void doScanWithFilter(Container adjacent, StorageControllerBlockEntity controller,
                                   InterfaceFilter filter, int beeCapacity) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int targetQty = filter.getQuantity();

        if (filter.getMode() == InterfaceFilter.FilterMode.ITEM) {
            scanItemMode(adjacent, filter, slots, beeCapacity, targetQty);
        } else {
            scanTextMode(adjacent, controller, filter, slots, beeCapacity, targetQty);
        }
    }

    /**
     * Mode ITEM: itere les slots du filtre pour trouver les items a importer.
     */
    private void scanItemMode(Container adjacent, InterfaceFilter filter,
                               int[] slots, int beeCapacity, int targetQty) {
        for (int i = 0; i < InterfaceFilter.SLOTS_PER_FILTER; i++) {
            ItemStack filterItem = filter.getItem(i);
            if (filterItem.isEmpty()) continue;

            createImportTasksForItem(adjacent, filterItem, slots, beeCapacity, targetQty);
        }
    }

    /**
     * Mode TEXT: cherche dans le reseau les items matchant le filtre texte.
     */
    private void scanTextMode(Container adjacent, StorageControllerBlockEntity controller,
                               InterfaceFilter filter, int[] slots, int beeCapacity, int targetQty) {
        List<ItemStack> networkItems = controller.getAggregatedItems();

        for (ItemStack networkItem : networkItems) {
            if (!filter.matches(networkItem, false)) continue;

            createImportTasksForItem(adjacent, networkItem, slots, beeCapacity, targetQty);
        }
    }

    /**
     * Cree des InterfaceTasks pour un item specifique a importer.
     * Calcule le needed en tenant compte des tasks deja en cours.
     *
     * @param targetQty 0=remplir les slots, N=importer jusqu'a N items
     */
    private void createImportTasksForItem(Container adjacent, ItemStack filterItem,
                                           int[] slots, int beeCapacity, int targetQty) {
        int currentCount = countInSlots(adjacent, filterItem, slots);
        int lockedCount = getLockedCount(filterItem);
        int deliveredCount = getDeliveredCount(filterItem);

        int needed;
        if (targetQty == 0) {
            int capacity = calculateCapacity(adjacent, filterItem, slots);
            needed = capacity - currentCount - lockedCount - deliveredCount;
        } else {
            needed = targetQty - currentCount - lockedCount - deliveredCount;
        }

        if (needed <= 0) {
            cancelExcessTodoTasks(filterItem);
            return;
        }

        int todoCount = getTodoCount(filterItem);
        int toCreate = needed - todoCount;

        if (toCreate <= 0) return;

        int itemBeeCapacity = Math.min(beeCapacity, filterItem.getMaxStackSize());
        while (toCreate > 0) {
            int chunk = Math.min(toCreate, itemBeeCapacity);
            tasks.add(new InterfaceTask(InterfaceTask.TaskType.IMPORT, filterItem, chunk));
            toCreate -= chunk;
        }
    }

    // === Helpers ===

    private int countInSlots(Container container, ItemStack template, int[] slots) {
        return ContainerHelper.countItem(container, template, slots);
    }

    /**
     * Calcule la capacite totale pour un item dans les slots donnes.
     * Slots vides = maxStackSize, slots avec le meme item = maxStackSize,
     * slots avec un item different = 0.
     */
    private int calculateCapacity(Container container, ItemStack template, int[] slots) {
        int capacity = 0;
        for (int slot : slots) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                capacity += template.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, template)) {
                capacity += existing.getMaxStackSize();
            }
        }
        return capacity;
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
