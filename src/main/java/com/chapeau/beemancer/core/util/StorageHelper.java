/**
 * ============================================================
 * [StorageHelper.java]
 * Description: Utilitaires pour le système de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | Blocks              | Vérification types   | isStorageContainer    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlock.java
 * - StorageControllerBlockEntity.java
 * - StorageEvents.java
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Helper pour le système de stockage.
 */
public final class StorageHelper {

    private StorageHelper() {}

    /**
     * Vérifie si un BlockState est un conteneur de stockage supporté.
     * Supporte: Chest, Trapped Chest, Barrel
     */
    public static boolean isStorageContainer(BlockState state) {
        if (state == null) return false;
        return state.getBlock() instanceof ChestBlock ||
               state.is(Blocks.BARREL);
    }
}
