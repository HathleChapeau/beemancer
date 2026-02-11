/**
 * ============================================================
 * [DeliveryContainerOps.java]
 * Description: Operations de recherche, extraction et depot dans les coffres du reseau
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageControllerBlockEntity   | Parent controller    | Acces coffres reseau      |
 * | ContainerHelper                | Operations container | count/extract/insert      |
 * | StorageTerminalBlockEntity     | Terminal deposit     | extractFromDeposit()      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 * - RequestManager.java (ChestItemInfo)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.core.util.ContainerHelper;
import com.chapeau.beemancer.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Operations de recherche, extraction et depot dans les coffres du reseau de stockage.
 */
public class DeliveryContainerOps {
    private final StorageControllerBlockEntity parent;

    public DeliveryContainerOps(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    /**
     * Info sur un coffre contenant un item: position + quantite disponible.
     */
    public record ChestItemInfo(BlockPos pos, int count) {}

    /**
     * Trouve un coffre contenant l'item demande dans tout le reseau.
     */
    @Nullable
    public BlockPos findChestWithItem(ItemStack template, int minCount) {
        if (parent.getLevel() == null || template.isEmpty()) return null;

        for (BlockPos chestPos : parent.getAllNetworkChests()) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                if (ContainerHelper.countItem(handler, template) >= minCount) {
                    return chestPos;
                }
            }
        }
        return null;
    }

    /**
     * Trouve TOUS les coffres du reseau contenant un item donne, avec leur quantite.
     * Ordonne par quantite decroissante.
     */
    public List<ChestItemInfo> findAllChestsWithItem(ItemStack template) {
        List<ChestItemInfo> result = new ArrayList<>();
        if (parent.getLevel() == null || template.isEmpty()) return result;

        for (BlockPos chestPos : parent.getAllNetworkChests()) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                int count = ContainerHelper.countItem(handler, template);
                if (count > 0) {
                    result.add(new ChestItemInfo(chestPos, count));
                }
            }
        }
        result.sort((a, b) -> Integer.compare(b.count, a.count));
        return result;
    }

    /**
     * Compte le nombre d'items d'un type donne dans un coffre specifique.
     */
    public int countItemInChest(ItemStack template, BlockPos chestPos) {
        if (parent.getLevel() == null || template.isEmpty()) return 0;
        if (!parent.getLevel().hasChunkAt(chestPos)) return 0;
        BlockEntity be = parent.getLevel().getBlockEntity(chestPos);

        if (be instanceof StorageTerminalBlockEntity terminal) {
            return terminal.countInDeposit(template);
        }

        IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
        if (handler == null) return 0;
        return ContainerHelper.countItem(handler, template);
    }

    /**
     * Trouve un coffre du reseau qui a de la place pour l'item donne.
     */
    @Nullable
    public BlockPos findChestWithSpace(ItemStack template, int count) {
        if (parent.getLevel() == null || template.isEmpty()) return null;

        for (BlockPos chestPos : parent.getAllNetworkChests()) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                if (ContainerHelper.availableSpace(handler, template) >= count) {
                    return chestPos;
                }
            }
        }
        return null;
    }

    /**
     * Extrait un item d'un coffre ou terminal pour une livraison.
     */
    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos) {
        if (parent.getLevel() == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;
        if (!parent.getLevel().hasChunkAt(chestPos)) return ItemStack.EMPTY;

        BlockEntity be = parent.getLevel().getBlockEntity(chestPos);

        if (be instanceof StorageTerminalBlockEntity terminal) {
            ItemStack result = terminal.extractFromDeposit(template, count);
            parent.getItemAggregator().setNeedsSync(true);
            return result;
        }

        IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
        if (handler == null) return ItemStack.EMPTY;

        ItemStack result = ContainerHelper.extractItem(handler, template, count);
        parent.getItemAggregator().setNeedsSync(true);
        return result;
    }

    /**
     * Depose un item dans un coffre specifique ou dans n'importe quel coffre du reseau.
     * @return le reste non depose
     */
    public ItemStack depositItemForDelivery(ItemStack stack, @Nullable BlockPos chestPos) {
        if (chestPos != null) {
            return depositIntoChest(stack, chestPos);
        }
        return parent.getItemAggregator().depositItem(stack);
    }

    private ItemStack depositIntoChest(ItemStack stack, BlockPos chestPos) {
        if (parent.getLevel() == null || stack.isEmpty()) return stack;
        if (!parent.getLevel().hasChunkAt(chestPos)) return stack;

        IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
        if (handler == null) return stack;

        ItemStack remaining = ContainerHelper.insertItem(handler, stack);
        parent.getItemAggregator().setNeedsSync(true);
        return remaining;
    }
}
