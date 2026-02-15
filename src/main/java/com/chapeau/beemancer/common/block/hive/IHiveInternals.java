/**
 * ============================================================
 * [IHiveInternals.java]
 * Description: Interface package-private pour le lifecycle manager
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | HiveBeeSlot                    | Donnees slot         | Acces slots abeilles      |
 * | HiveFlowerPool                 | Pool fleurs          | Acces pool partagé        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HiveBeeLifecycleManager.java (back-reference)
 * - MagicHiveBlockEntity.java (implements)
 * - HiveMultiblockBlockEntity.java (implements)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Interface package-private qu'une ruche expose au HiveBeeLifecycleManager.
 * Permet au manager de fonctionner avec MagicHiveBlockEntity et HiveMultiblockBlockEntity.
 */
interface IHiveInternals {

    NonNullList<ItemStack> getItems();

    HiveBeeSlot[] getBeeSlots();

    HiveFlowerPool getFlowerPool();

    void returnAssignedFlower(int slot);

    void triggerFlowerScan();

    boolean hasFlowersForSlot(int slot);

    void setChanged();

    Level getLevel();

    BlockPos getBlockPos();

    /**
     * Limite de stack pour les slots de sortie.
     * MagicHive: 16, Multiblock: 64.
     */
    int getOutputSlotLimit();

    /**
     * Determine si l'abeille dans le slot peut etre relachee.
     * MagicHive: check canBeeForage (fleurs, jour/nuit, temperature, pas antibreeding).
     * Multiblock: check !breedingMode.
     */
    boolean shouldReleaseBee(int slot);

    /**
     * Determine si le breeding doit etre tente a l'entree d'une abeille.
     * MagicHive: !antibreedingMode.
     * Multiblock: toujours true.
     */
    boolean shouldBreedOnEntry();

    /**
     * Detecte si une autre ruche (IHiveInternals) se trouve dans le rayon donne.
     * Utilisee pour bloquer la sortie des abeilles si ruches trop proches.
     */
    default boolean hasNearbyHive(int radius) {
        Level level = getLevel();
        if (level == null) return false;
        BlockPos pos = getBlockPos();
        for (BlockPos check : BlockPos.betweenClosed(
                pos.offset(-radius, -radius, -radius),
                pos.offset(radius, radius, radius))) {
            if (check.equals(pos)) continue;
            if (level.getBlockEntity(check) instanceof IHiveInternals) return true;
        }
        return false;
    }
}
