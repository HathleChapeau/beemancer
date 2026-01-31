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

import java.util.LinkedHashMap;
import java.util.Map;

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

        // Agreger les counts par type d'item pour appliquer minKeep globalement
        Map<String, ItemStack> templatesByKey = new LinkedHashMap<>();
        Map<String, Integer> totalCountsByKey = new LinkedHashMap<>();

        for (int slot : slots) {
            ItemStack stack = adjacent.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!matchesFilter(stack, true)) continue;

            String key = itemKey(stack);
            templatesByKey.putIfAbsent(key, stack.copyWithCount(1));
            totalCountsByKey.merge(key, stack.getCount(), Integer::sum);
        }

        // Pour chaque type d'item, calculer l'exportable global et exporter
        for (Map.Entry<String, ItemStack> entry : templatesByKey.entrySet()) {
            String key = entry.getKey();
            ItemStack template = entry.getValue();

            if (pendingTasks.containsKey(key)) continue;

            int totalCount = totalCountsByKey.get(key);
            int exportable = totalCount - minKeep;
            if (exportable <= 0) continue;

            int toExport = Math.min(exportable, maxQuantity);

            BlockPos chestPos = controller.findSlotForItem(template);
            if (chestPos == null) continue;

            // Retirer les items des slots (dans l'ordre, jusqu'a toExport)
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
        tag.putInt("MinKeep", minKeep);
    }

    @Override
    protected void loadExtra(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("MinKeep")) {
            minKeep = tag.getInt("MinKeep");
        }
    }
}
