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
 * pour le remplir avec les items filtres, jusqu'a maxCount.
 *
 * Implemente IDeliveryEndpoint pour recevoir les items livres par les abeilles
 * et les inserer dans l'inventaire adjacent.
 */
public class ImportInterfaceBlockEntity extends NetworkInterfaceBlockEntity implements IDeliveryEndpoint {

    private int maxCount = 64;

    public ImportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.IMPORT_INTERFACE.get(), pos, state);
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = Math.max(1, Math.min(maxCount, 64 * 9));
        setChanged();
        syncToClient();
    }

    // === IDeliveryEndpoint ===

    @Override
    public ItemStack receiveDeliveredItems(ItemStack items) {
        Container adjacent = getAdjacentInventory();
        if (adjacent == null) return items;

        ItemStack remaining = items.copy();
        int[] slots = getOperableSlots(adjacent);

        // Essayer d'inserer dans les slots operables
        for (int slot : slots) {
            if (remaining.isEmpty()) break;

            ItemStack existing = adjacent.getItem(slot);
            if (existing.isEmpty()) {
                int toPlace = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                adjacent.setItem(slot, remaining.copyWithCount(toPlace));
                remaining.shrink(toPlace);
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toTransfer = Math.min(space, remaining.getCount());
                if (toTransfer > 0) {
                    existing.grow(toTransfer);
                    remaining.shrink(toTransfer);
                }
            }
        }

        if (!remaining.equals(items)) {
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
        if (filterMode == FilterMode.ITEM) {
            doScanItemMode(adjacent, controller);
        } else {
            doScanTextMode(adjacent, controller);
        }
    }

    private void doScanItemMode(Container adjacent, StorageControllerBlockEntity controller) {
        int[] slots = getOperableSlots(adjacent);
        int maxQuantity = ControllerStats.getQuantity(controller.getEssenceSlots());

        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack filter = filterSlots.getStackInSlot(i);
            if (filter.isEmpty()) continue;

            String key = itemKey(filter);
            if (pendingTasks.containsKey(key)) continue;

            // Compter combien il y a deja dans l'inventaire adjacent
            int currentCount = countInSlots(adjacent, filter, slots);
            if (currentCount >= maxCount) continue;

            int needed = Math.min(maxCount - currentCount, maxQuantity);
            if (needed <= 0) continue;

            BlockPos chestPos = controller.findChestWithItem(filter, 1);
            if (chestPos == null) continue;

            DeliveryTask task = new DeliveryTask(
                filter, needed, chestPos, worldPosition,
                DeliveryTask.DeliveryType.EXTRACT
            );
            controller.addDeliveryTask(task);
            pendingTasks.put(key, task.getTaskId());
        }
    }

    private void doScanTextMode(Container adjacent, StorageControllerBlockEntity controller) {
        int[] slots = getOperableSlots(adjacent);
        int maxQuantity = ControllerStats.getQuantity(controller.getEssenceSlots());

        // Scanner les items agreges du reseau pour trouver ceux qui matchent les filtres texte
        List<ItemStack> networkItems = controller.getAggregatedItems();
        for (ItemStack networkItem : networkItems) {
            if (!matchesFilter(networkItem, false)) continue;

            String key = itemKey(networkItem);
            if (pendingTasks.containsKey(key)) continue;

            int currentCount = countInSlots(adjacent, networkItem, slots);
            if (currentCount >= maxCount) continue;

            int needed = Math.min(maxCount - currentCount, maxQuantity);
            if (needed <= 0) continue;

            BlockPos chestPos = controller.findChestWithItem(networkItem, 1);
            if (chestPos == null) continue;

            DeliveryTask task = new DeliveryTask(
                networkItem, needed, chestPos, worldPosition,
                DeliveryTask.DeliveryType.EXTRACT
            );
            controller.addDeliveryTask(task);
            pendingTasks.put(key, task.getTaskId());
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

    // === NBT Hooks ===

    @Override
    protected void saveExtra(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("MaxCount", maxCount);
    }

    @Override
    protected void loadExtra(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("MaxCount")) {
            maxCount = tag.getInt("MaxCount");
        }
    }
}
