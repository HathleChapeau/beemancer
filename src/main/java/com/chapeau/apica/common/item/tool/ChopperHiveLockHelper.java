/**
 * ============================================================
 * [ChopperHiveLockHelper.java]
 * Description: Etat client-side du verrouillage glow du Chopper Hive
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
 * - ChopperHiveItem.java (toggle lock)
 * - ChopperHivePreviewRenderer.java (lecture etat lock + timing particules)
 * - ChopperHiveItemRenderer.java (detection phase chopping)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Stocke l'etat de verrouillage du glow du Chopper Hive cote client.
 * Separe de l'item pour eviter les problemes de state server/client.
 * Conserve le gameTime du lock pour synchroniser les animations et particules.
 */
public final class ChopperHiveLockHelper {

    private static boolean locked = false;
    private static List<BlockPos> lockedPositions = List.of();

    /** gameTime (level.getGameTime()) au moment du lock, pour timer les phases. */
    private static long lockGameTime = -1;

    /**
     * Toggle le verrouillage. Si on lock, les positions actuelles du renderer
     * seront figees. Si on unlock, on repasse en mode live.
     */
    public static void toggleLock() {
        locked = !locked;
        if (!locked) {
            lockedPositions = List.of();
            lockGameTime = -1;
        }
    }

    /** Verrouille avec les positions donnees. */
    public static void lockWith(List<BlockPos> positions, long gameTime) {
        locked = true;
        lockedPositions = List.copyOf(positions);
        lockGameTime = gameTime;
    }

    /** Reset complet (quand l'item n'est plus en main). */
    public static void reset() {
        locked = false;
        lockedPositions = List.of();
        lockGameTime = -1;
    }

    public static boolean isLocked() {
        return locked;
    }

    public static List<BlockPos> getLockedPositions() {
        return lockedPositions;
    }

    /** Retourne le gameTime auquel le lock a commence, ou -1 si pas locke. */
    public static long getLockGameTime() {
        return lockGameTime;
    }

    private ChopperHiveLockHelper() {
    }
}
