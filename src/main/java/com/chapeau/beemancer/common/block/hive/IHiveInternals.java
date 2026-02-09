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
}
