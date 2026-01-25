/**
 * ============================================================
 * [StorageEditModeHandler.java]
 * Description: Gère le mode édition du Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageControllerBlockEntity    | BlockEntity            | Vérification état     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlock.java (toggle mode)
 * - BeemancerEvents.java (interception clics)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire du mode édition pour le Storage Controller.
 *
 * Trace quel controller chaque joueur est en train d'éditer,
 * permettant d'intercepter les clics sur les coffres.
 */
public class StorageEditModeHandler {

    // Map joueur -> position du controller en édition
    private static final Map<UUID, BlockPos> editingControllers = new HashMap<>();

    /**
     * Enregistre qu'un joueur commence à éditer un controller.
     */
    public static void startEditing(UUID playerId, BlockPos controllerPos) {
        editingControllers.put(playerId, controllerPos);
    }

    /**
     * Termine l'édition pour un joueur.
     */
    public static void stopEditing(UUID playerId) {
        editingControllers.remove(playerId);
    }

    /**
     * Vérifie si un joueur est en mode édition et retourne la position du controller.
     */
    public static BlockPos getEditingController(UUID playerId) {
        return editingControllers.get(playerId);
    }

    /**
     * Vérifie si un joueur est en mode édition d'un controller valide.
     */
    public static boolean isEditing(Player player, Level level) {
        BlockPos controllerPos = editingControllers.get(player.getUUID());
        if (controllerPos == null) return false;

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            // Controller n'existe plus
            stopEditing(player.getUUID());
            return false;
        }

        return controller.canEdit(player.getUUID());
    }

    /**
     * Nettoie les références pour un monde qui se décharge.
     */
    public static void clearAll() {
        editingControllers.clear();
    }
}
