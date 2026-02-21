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
package com.chapeau.apica.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
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

    /**
     * Retourne la position de l'autre moitie d'un double chest, ou null si le coffre est simple.
     * Utilise la propriete ChestBlock.TYPE (LEFT/RIGHT) et la direction du coffre
     * pour determiner la position du bloc connecte.
     */
    @Nullable
    public static BlockPos getDoubleChestOtherHalf(Level level, BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return null;

        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE) return null;

        Direction facing = state.getValue(ChestBlock.FACING);
        Direction otherDir = (type == ChestType.LEFT)
            ? facing.getClockWise()
            : facing.getCounterClockWise();
        return pos.relative(otherDir);
    }

    /**
     * Retourne la position canonique d'un coffre (pour les doubles, toujours la moitie LEFT).
     * Si le coffre est simple, retourne la position telle quelle.
     * Cela garantit qu'un double chest n'est enregistre qu'une seule fois dans le reseau.
     *
     * Direction vanilla: LEFT.getConnectedDirection() = facing.getClockWise() → pointe vers RIGHT
     *                    RIGHT.getConnectedDirection() = facing.getCounterClockWise() → pointe vers LEFT
     */
    public static BlockPos getCanonicalChestPos(Level level, BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return pos;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return pos;

        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE) return pos;
        if (type == ChestType.LEFT) return pos;

        // Ce coffre est RIGHT: calculer la position LEFT
        // RIGHT → LEFT = facing.getCounterClockWise() (direction opposee a clockwise)
        Direction facing = state.getValue(ChestBlock.FACING);
        Direction toLeftHalf = facing.getCounterClockWise();
        return pos.relative(toLeftHalf);
    }

    /**
     * Verifie si la position donnee est un double chest.
     */
    public static boolean isDoubleChest(Level level, BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return false;
        return state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
    }
}
