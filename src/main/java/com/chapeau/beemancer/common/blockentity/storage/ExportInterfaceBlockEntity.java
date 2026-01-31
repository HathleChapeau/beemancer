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

/**
 * Export Interface: scanne l'inventaire adjacent et exporte les items
 * correspondant aux filtres (ou tous si aucun filtre) vers le reseau.
 *
 * Les items sont retires immediatement de l'inventaire adjacent et
 * une tache DEPOSIT est creee pour les transporter vers un coffre du reseau.
 *
 * minKeep: nombre d'items a conserver dans l'inventaire adjacent (defaut 0).
 */
public class ExportInterfaceBlockEntity extends NetworkInterfaceBlockEntity {

    private int minKeep = 0;

    public ExportInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.EXPORT_INTERFACE.get(), pos, state);
    }

    public int getMinKeep() {
        return minKeep;
    }

    public void setMinKeep(int minKeep) {
        this.minKeep = Math.max(0, minKeep);
        setChanged();
        syncToClient();
    }

    // === Scan Logic ===

    @Override
    protected void doScan(Container adjacent, StorageControllerBlockEntity controller) {
        int[] slots = getOperableSlots(adjacent);
        int maxQuantity = ControllerStats.getQuantity(controller.getEssenceSlots());

        for (int slot : slots) {
            ItemStack stack = adjacent.getItem(slot);
            if (stack.isEmpty()) continue;

            // Verifier le filtre: sans filtre = exporte tout (checkEmpty=true)
            if (!matchesFilter(stack, true)) continue;

            String key = itemKey(stack);
            if (pendingTasks.containsKey(key)) continue;

            int exportable = stack.getCount() - minKeep;
            if (exportable <= 0) continue;

            int toExport = Math.min(exportable, maxQuantity);

            BlockPos chestPos = controller.findSlotForItem(stack);
            if (chestPos == null) continue;

            // Retirer les items immediatement de l'inventaire adjacent
            ItemStack extracted = stack.copy();
            extracted.setCount(toExport);
            stack.shrink(toExport);
            if (stack.isEmpty()) {
                adjacent.setItem(slot, ItemStack.EMPTY);
            }
            adjacent.setChanged();

            DeliveryTask task = new DeliveryTask(
                extracted, toExport, chestPos, worldPosition,
                DeliveryTask.DeliveryType.DEPOSIT
            );
            controller.addDeliveryTask(task);
            pendingTasks.put(key, task.getTaskId());
        }
    }

    // === NBT Hooks ===

    @Override
    protected void saveExtra(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("MinKeep", minKeep);
    }

    @Override
    protected void loadExtra(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("MinKeep")) {
            minKeep = tag.getInt("MinKeep");
        }
    }
}
