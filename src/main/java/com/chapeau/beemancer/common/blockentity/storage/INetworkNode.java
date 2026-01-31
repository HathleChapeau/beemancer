/**
 * ============================================================
 * [INetworkNode.java]
 * Description: Interface commune pour les noeuds du reseau de stockage (controller, relay)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance     | Raison                | Utilisation           |
 * |----------------|----------------------|-----------------------|
 * | BlockPos       | Position monde       | Identification noeud  |
 * | Level          | Monde Minecraft      | Acces blocs           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (implementation)
 * - StorageRelayBlockEntity.java (implementation)
 * - StorageChestManager.java (back-reference generique)
 * - StorageEditModeHandler.java (gestion mode edition)
 * - StorageEvents.java (interception clics)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Interface commune pour les noeuds du reseau de stockage.
 * Un noeud peut entrer en mode edition, enregistrer des coffres,
 * et se connecter a d'autres noeuds pour etendre le reseau.
 */
public interface INetworkNode {

    /** Position du noeud dans le monde. */
    BlockPos getNodePos();

    /** Le monde dans lequel se trouve le noeud. */
    @Nullable
    Level getNodeLevel();

    /** Rayon maximal pour enregistrer des coffres et detecter d'autres noeuds. */
    int getRange();

    // === Mode edition ===

    /** Active/desactive le mode edition pour un joueur. Retourne true si active. */
    boolean toggleEditMode(UUID playerId);

    /** Force la sortie du mode edition. */
    void exitEditMode();

    /** Verifie si un joueur peut editer ce noeud. */
    boolean canEdit(UUID playerId);

    /** Retourne true si le noeud est en mode edition. */
    boolean isEditMode();

    /** UUID du joueur en mode edition, ou null. */
    @Nullable
    UUID getEditingPlayer();

    // === Coffres ===

    /** Le gestionnaire de coffres de ce noeud. */
    StorageChestManager getChestManager();

    /** Toggle l'enregistrement d'un coffre. */
    boolean toggleChest(BlockPos chestPos);

    /** Retourne les coffres enregistres par ce noeud. */
    Set<BlockPos> getRegisteredChests();

    // === Reseau (connexion a d'autres noeuds) ===

    /** Les positions des noeuds connectes a celui-ci. */
    Set<BlockPos> getConnectedNodes();

    /** Connecte un autre noeud a celui-ci. */
    void connectNode(BlockPos nodePos);

    /** Deconnecte un noeud. */
    void disconnectNode(BlockPos nodePos);

    // === Sync ===

    /** Marque le block entity comme modifie. */
    void markDirty();

    /** Synchronise les donnees vers le client. */
    void syncNodeToClient();
}
