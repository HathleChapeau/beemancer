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
 * | StorageControllerBlockEntity  | Controller lie       | Taches de livraison            |
 * | ControllerStats               | Quantite max         | Limite par tache               |
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

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Import Interface: scanne l'inventaire adjacent et cree des taches EXTRACT
 * pour le remplir avec les items filtres.
 *
 * Comportement quantite:
 * - quantite=0: importer jusqu'a remplir les slots
 * - quantite=N: importer jusqu'a avoir N items de ce type
 *
 * Sans filtre: importer tous les items depuis le reseau vers les globalSelectedSlots.
 *
 * Implemente IDeliveryEndpoint pour recevoir les items livres par les abeilles
 * et les inserer dans l'inventaire adjacent.
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
        // Phase 1: merger dans les stacks existants du meme type
        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            ItemStack existing = adjacent.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toTransfer = Math.min(space, remaining.getCount());
                if (toTransfer > 0) {
                    existing.grow(toTransfer);
                    remaining.shrink(toTransfer);
                }
            }
        }

        // Phase 2: remplir les slots vides
        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            if (adjacent.getItem(slot).isEmpty()) {
                int toPlace = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                adjacent.setItem(slot, remaining.copyWithCount(toPlace));
                remaining.shrink(toPlace);
            }
        }

        if (remaining.getCount() != remaining.getMaxStackSize()) {
            adjacent.setChanged();
        }

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
     * Sans filtre: rien a importer specifiquement (pas de cible connue).
     * On ne peut pas "tout importer" sans savoir quoi importer.
     */
    private void doScanNoFilter(Container adjacent, StorageControllerBlockEntity controller,
                                 int maxQuantity) {
        // Sans filtre, l'import n'a rien a faire car il ne sait pas quoi chercher
    }

    /**
     * Avec filtre: chercher les items qui matchent et creer des taches d'import.
     */
    private void doScanWithFilter(Container adjacent, StorageControllerBlockEntity controller,
                                   InterfaceFilter filter, int maxQuantity) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int targetQty = filter.getQuantity();

        if (filter.getMode() == InterfaceFilter.FilterMode.ITEM) {
            scanItemMode(adjacent, controller, filter, slots, maxQuantity, targetQty);
        } else {
            scanTextMode(adjacent, controller, filter, slots, maxQuantity, targetQty);
        }
    }

    private void scanItemMode(Container adjacent, StorageControllerBlockEntity controller,
                               InterfaceFilter filter, int[] slots, int maxQuantity, int targetQty) {
        for (int i = 0; i < InterfaceFilter.SLOTS_PER_FILTER; i++) {
            ItemStack filterItem = filter.getItem(i);
            if (filterItem.isEmpty()) continue;

            String key = itemKey(filterItem);
            int pendingCount = getPendingCount(key);

            int currentCount = countInSlots(adjacent, filterItem, slots);

            int needed;
            if (targetQty == 0) {
                int capacity = calculateCapacity(adjacent, filterItem, slots);
                needed = capacity - currentCount - pendingCount;
            } else {
                needed = targetQty - currentCount - pendingCount;
            }

            if (needed <= 0) continue;
            needed = Math.min(needed, maxQuantity);

            BlockPos chestPos = controller.findChestWithItem(filterItem, 1);
            if (chestPos == null) continue;

            // Loop prevention: ne pas creer de tache si la source est le meme bloc que la destination
            if (chestPos.equals(getAdjacentPos())) continue;

            DeliveryTask task = new DeliveryTask(
                filterItem, needed, chestPos, worldPosition,
                DeliveryTask.DeliveryType.EXTRACT, DeliveryTask.TaskOrigin.AUTOMATION
            );
            controller.addDeliveryTask(task);
            pendingTasks.put(key, new PendingTaskInfo(task.getTaskId(), needed));
        }
    }

    private void scanTextMode(Container adjacent, StorageControllerBlockEntity controller,
                               InterfaceFilter filter, int[] slots, int maxQuantity, int targetQty) {
        List<ItemStack> networkItems = controller.getAggregatedItems();
        for (ItemStack networkItem : networkItems) {
            if (!filter.matches(networkItem, false)) continue;

            String key = itemKey(networkItem);
            int pendingCount = getPendingCount(key);

            int currentCount = countInSlots(adjacent, networkItem, slots);

            int needed;
            if (targetQty == 0) {
                int capacity = calculateCapacity(adjacent, networkItem, slots);
                needed = capacity - currentCount - pendingCount;
            } else {
                needed = targetQty - currentCount - pendingCount;
            }

            if (needed <= 0) continue;
            needed = Math.min(needed, maxQuantity);

            BlockPos chestPos = controller.findChestWithItem(networkItem, 1);
            if (chestPos == null) continue;

            // Loop prevention: ne pas creer de tache si la source est le meme bloc que la destination
            if (chestPos.equals(getAdjacentPos())) continue;

            DeliveryTask task = new DeliveryTask(
                networkItem, needed, chestPos, worldPosition,
                DeliveryTask.DeliveryType.EXTRACT, DeliveryTask.TaskOrigin.AUTOMATION
            );
            controller.addDeliveryTask(task);
            pendingTasks.put(key, new PendingTaskInfo(task.getTaskId(), needed));
        }
    }

    private int countInSlots(Container container, ItemStack template, int[] slots) {
        int count = 0;
        for (int slot : slots) {
            ItemStack existing = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(existing, template)) {
                count += existing.getCount();
            }
        }
        return count;
    }

    /**
     * Calcule la capacite totale pour un item dans les slots donnes.
     * Retourne le nombre total d'items de ce type que les slots peuvent contenir
     * (slots vides = maxStackSize, slots avec le meme item = maxStackSize,
     * slots avec un item different = 0).
     * Utilise avec needed = totalCapacity - currentCount.
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
