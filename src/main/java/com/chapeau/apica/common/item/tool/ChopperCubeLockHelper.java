/**
 * ============================================================
 * [ChopperCubeLockHelper.java]
 * Description: Etat client-side du verrouillage glow du Chopper Cube
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ChopperCubeItem.java (toggle lock)
 * - ChopperCubePreviewRenderer.java (lecture etat lock)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Stocke l'etat de verrouillage du glow du Chopper Cube cote client.
 * Separe de l'item pour eviter les problemes de state server/client.
 */
public final class ChopperCubeLockHelper {

    private static boolean locked = false;
    private static List<BlockPos> lockedPositions = List.of();

    /**
     * Toggle le verrouillage. Si on lock, les positions actuelles du renderer
     * seront figees. Si on unlock, on repasse en mode live.
     */
    public static void toggleLock() {
        locked = !locked;
        if (!locked) {
            lockedPositions = List.of();
        }
    }

    /** Verrouille avec les positions donnees. */
    public static void lockWith(List<BlockPos> positions) {
        locked = true;
        lockedPositions = List.copyOf(positions);
    }

    /** Reset complet (quand l'item n'est plus en main). */
    public static void reset() {
        locked = false;
        lockedPositions = List.of();
    }

    public static boolean isLocked() {
        return locked;
    }

    public static List<BlockPos> getLockedPositions() {
        return lockedPositions;
    }

    private ChopperCubeLockHelper() {
    }
}
