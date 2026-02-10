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
 * | PartCraftingPaperData         | Donnees Part Paper   | Craft mode: items a exporter   |
 * | CrafterBlockEntity            | Crafter lie          | Output slots, export craft     |
 * | DeliveryTask                  | Livraison bee        | Export craft source=crafter    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ExportInterfaceBlock.java (creation BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.InterfaceTask;
import com.chapeau.beemancer.common.data.PartCraftingPaperData;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

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
        super(BeemancerBlockEntities.EXPORT_INTERFACE.get(), pos, state);
    }

    // === Scan Logic ===

    @Override
    protected void doScan(Container adjacent, StorageControllerBlockEntity controller) {
        cleanupDeliveredTasks();
        int beeCapacity = controller.getBeeCapacity();

        if (craftMode) {
            doScanCraftMode(adjacent, controller, beeCapacity);
            return;
        }

        Set<String> activeItemKeys = new HashSet<>();

        if (filters.isEmpty()) {
            doScanNoFilter(adjacent, beeCapacity, activeItemKeys);
        } else {
            for (InterfaceFilter filter : filters) {
                doScanWithFilter(adjacent, filter, beeCapacity, activeItemKeys);
            }
        }

        cleanupOrphanedTasks(activeItemKeys);
        publishTodoTasks(controller);
    }

    /**
     * Scan en mode craft: utilise le Part Crafting Paper OUTPUT pour definir
     * les items a exporter depuis les output slots du Crafter vers le reseau.
     * Attend que le craft soit complet (items presents dans les output slots).
     */
    private void doScanCraftMode(Container adjacent, StorageControllerBlockEntity controller,
                                  int beeCapacity) {
        PartCraftingPaperData paperData = getCraftPaperData();
        if (paperData == null || paperData.mode() != PartCraftingPaperData.PartMode.OUTPUT) {
            cleanupOrphanedTasks(new HashSet<>());
            return;
        }

        // Le Crafter doit etre lie au controller
        CrafterBlockEntity crafter = controller.getCrafter();
        if (crafter == null) {
            cleanupOrphanedTasks(new HashSet<>());
            return;
        }

        Set<String> activeItemKeys = new HashSet<>();

        for (ItemStack item : paperData.items()) {
            if (item.isEmpty()) continue;

            String key = itemKey(item);
            activeItemKeys.add(key);

            // Compter les items dans les output slots du Crafter (slots A et B)
            int currentCount = countInCrafterOutput(crafter, item);

            // Exporter tout ce qui est present dans les output slots
            if (currentCount > 0) {
                reconcileTasksForItem(item, currentCount, beeCapacity,
                        InterfaceTask.TaskType.EXPORT);
            }
        }

        cleanupOrphanedTasks(activeItemKeys);
        publishCraftExportTasks(controller, crafter);
    }

    /**
     * Compte les items d'un type donne dans les output slots du Crafter.
     */
    private int countInCrafterOutput(CrafterBlockEntity crafter, ItemStack template) {
        int count = 0;
        net.neoforged.neoforge.items.ItemStackHandler inv = crafter.getInventory();
        for (int slot : new int[]{CrafterBlockEntity.SLOT_OUTPUT_A, CrafterBlockEntity.SLOT_OUTPUT_B}) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(template, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Publie les tasks NEEDED pour l'export craft mode.
     * La source est le Crafter (pas l'inventaire adjacent).
     */
    private void publishCraftExportTasks(StorageControllerBlockEntity controller,
                                          CrafterBlockEntity crafter) {
        for (InterfaceTask task : tasks) {
            if (task.getState() != InterfaceTask.TaskState.NEEDED) continue;
            assignCraftExportBee(controller, crafter, task);
        }
        setChanged();
    }

    /**
     * Assigne une bee pour exporter depuis le Crafter vers le reseau.
     * Source = Crafter position, dest = coffre dans le reseau.
     */
    private void assignCraftExportBee(StorageControllerBlockEntity controller,
                                       CrafterBlockEntity crafter, InterfaceTask task) {
        if (level == null) return;

        BlockPos dest = controller.findSlotForItem(task.getTemplate());
        if (dest == null) return;

        BlockPos crafterPos = crafter.getBlockPos();
        if (dest.equals(crafterPos)) return;

        com.chapeau.beemancer.common.block.storage.DeliveryTask dt =
                new com.chapeau.beemancer.common.block.storage.DeliveryTask(
                    task.getTemplate(), task.getCount(),
                    crafterPos, dest,
                    com.chapeau.beemancer.common.block.storage.DeliveryTask.TaskOrigin.AUTOMATION, false,
                    getBlockPos(), null,
                    task.getTaskId(), getBlockPos()
                );
        controller.getDeliveryManager().addDeliveryTask(dt);
        task.lockTask(dt.getTaskId(), level.getGameTime());
    }

    /**
     * Sans filtre: exporter tous les items des globalSelectedSlots.
     */
    private void doScanNoFilter(Container adjacent, int beeCapacity,
                                 Set<String> activeItemKeys) {
        int[] slots = getGlobalOperableSlots(adjacent);
        reconcileExportItems(adjacent, slots, 0, beeCapacity, null, activeItemKeys);
    }

    /**
     * Avec filtre: exporter les items qui matchent, en respectant la quantite.
     */
    private void doScanWithFilter(Container adjacent, InterfaceFilter filter,
                                   int beeCapacity, Set<String> activeItemKeys) {
        int[] slots = getOperableSlots(adjacent, filter.getSelectedSlots());
        int keepQty = filter.getQuantity();
        reconcileExportItems(adjacent, slots, keepQty, beeCapacity, filter, activeItemKeys);
    }

    /**
     * Reconcilie les InterfaceTasks pour chaque type d'item a exporter.
     * Met a jour les tasks existantes (LOCKED et NEEDED) au lieu de creer
     * de nouvelles tasks a chaque scan.
     *
     * @param filter si non-null, seuls les items matchant ce filtre sont exportes
     * @param keepQty 0=tout exporter, N=garder N items
     * @param activeItemKeys set des itemKeys actifs (rempli par cette methode)
     */
    private void reconcileExportItems(Container adjacent, int[] slots,
                                       int keepQty, int beeCapacity,
                                       InterfaceFilter filter,
                                       Set<String> activeItemKeys) {
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
            String key = entry.getKey();
            ItemStack template = entry.getValue();
            int totalCount = totalCountsByKey.get(key);
            activeItemKeys.add(key);

            // Le nombre total desire a exporter (sans deduire locked/needed existants)
            int exportable = Math.max(0, totalCount - keepQty);

            reconcileTasksForItem(template, exportable, beeCapacity,
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
