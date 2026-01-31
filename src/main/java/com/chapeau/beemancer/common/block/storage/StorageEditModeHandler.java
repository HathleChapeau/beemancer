/**
 * ============================================================
 * [StorageEditModeHandler.java]
 * Description: Gère le mode édition des noeuds du réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | INetworkNode                    | Interface noeud        | Vérification état     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlock.java (toggle mode)
 * - StorageRelayBlock.java (toggle mode)
 * - StorageEvents.java (interception clics)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.INetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire du mode édition pour les noeuds du réseau de stockage.
 *
 * Trace quel noeud (controller ou relay) chaque joueur est en train d'éditer,
 * permettant d'intercepter les clics sur les coffres.
 */
public class StorageEditModeHandler {

    // Map joueur -> position du noeud en édition
    private static final Map<UUID, BlockPos> editingNodes = new HashMap<>();

    /**
     * Enregistre qu'un joueur commence à éditer un noeud.
     */
    public static void startEditing(UUID playerId, BlockPos nodePos) {
        editingNodes.put(playerId, nodePos);
    }

    /**
     * Termine l'édition pour un joueur.
     */
    public static void stopEditing(UUID playerId) {
        editingNodes.remove(playerId);
    }

    /**
     * Retourne la position du noeud en cours d'édition par un joueur.
     */
    public static BlockPos getEditingNode(UUID playerId) {
        return editingNodes.get(playerId);
    }

    /**
     * Retrocompatibilite: alias pour getEditingNode.
     */
    public static BlockPos getEditingController(UUID playerId) {
        return getEditingNode(playerId);
    }

    /**
     * Vérifie si un joueur est en mode édition d'un noeud valide.
     */
    public static boolean isEditing(Player player, Level level) {
        BlockPos nodePos = editingNodes.get(player.getUUID());
        if (nodePos == null) return false;

        BlockEntity be = level.getBlockEntity(nodePos);
        if (!(be instanceof INetworkNode node)) {
            stopEditing(player.getUUID());
            return false;
        }

        return node.canEdit(player.getUUID());
    }

    /**
     * Retourne le INetworkNode en cours d'édition par un joueur, ou null.
     */
    public static INetworkNode getEditingNetworkNode(Player player, Level level) {
        BlockPos nodePos = editingNodes.get(player.getUUID());
        if (nodePos == null) return null;

        BlockEntity be = level.getBlockEntity(nodePos);
        if (be instanceof INetworkNode node && node.canEdit(player.getUUID())) {
            return node;
        }
        return null;
    }

    /**
     * Nettoie les références pour un monde qui se décharge.
     */
    public static void clearAll() {
        editingNodes.clear();
    }
}
