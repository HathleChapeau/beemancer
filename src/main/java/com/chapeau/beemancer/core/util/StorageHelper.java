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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Helper pour le système de stockage.
 */
public final class StorageHelper {

    private StorageHelper() {}

    /**
     * Vérifie si un BlockState est un conteneur de stockage supporté (whitelist legacy).
     * Supporte: Chest, Trapped Chest, Barrel
     */
    public static boolean isStorageContainer(BlockState state) {
        if (state == null) return false;
        return state.getBlock() instanceof ChestBlock ||
               state.is(Blocks.BARREL);
    }

    /**
     * Verifie si une position expose un IItemHandler via la capability NeoForge.
     * Detecte automatiquement tous les conteneurs moddés et vanilla.
     */
    public static boolean hasItemHandlerCapability(Level level, BlockPos pos, @Nullable Direction direction) {
        if (level == null || !level.hasChunkAt(pos)) return false;
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null, null, direction);
        return handler != null;
    }

    /**
     * Recupere le IItemHandler a une position, ou null si absent.
     */
    @Nullable
    public static IItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction direction) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null, null, direction);
    }
}
