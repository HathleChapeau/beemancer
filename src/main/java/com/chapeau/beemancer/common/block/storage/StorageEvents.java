/**
 * ============================================================
 * [StorageEvents.java]
 * Description: Gestionnaire d'événements pour le système de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageEditModeHandler          | État édition           | Vérification mode     |
 * | INetworkNode                    | Interface noeud        | Enregistrement coffres|
 * | StorageHelper                   | Vérification coffres   | isStorageContainer    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement événements)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.INetworkNode;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import com.chapeau.beemancer.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Gère les événements liés au système de stockage.
 *
 * Intercepte les clics droits sur les coffres quand un joueur
 * est en mode édition d'un noeud de réseau (controller ou relay).
 */
public class StorageEvents {

    /**
     * Intercepte les clics droits sur les blocs pour gérer le mode édition.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos clickedPos = event.getPos();

        if (level.isClientSide()) return;

        if (!StorageEditModeHandler.isEditing(player, level)) return;

        BlockPos nodePos = StorageEditModeHandler.getEditingNode(player.getUUID());
        if (nodePos == null) return;

        BlockEntity nodeBe = level.getBlockEntity(nodePos);
        if (!(nodeBe instanceof INetworkNode node)) return;

        BlockState clickedState = level.getBlockState(clickedPos);

        // Shift+clic sur un terminal = lier au controller (seulement si on edite un controller)
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(clickedPos);
            if (be instanceof StorageTerminalBlockEntity terminal) {
                // Chercher le controller racine dans le reseau
                BlockPos controllerPos = findControllerInNetwork(node, level);
                if (controllerPos != null) {
                    terminal.linkToController(controllerPos);
                    player.displayClientMessage(
                        Component.translatable("message.beemancer.storage_terminal.linked_manually"),
                        true
                    );
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }

            // Shift+clic sur un autre noeud = enregistrer mutuellement
            if (be instanceof INetworkNode otherNode && !clickedPos.equals(nodePos)) {
                if (node.getChestManager().isInRange(clickedPos)) {
                    node.connectNode(clickedPos);
                    otherNode.connectNode(nodePos);
                    player.displayClientMessage(
                        Component.translatable("message.beemancer.storage_relay.node_linked"),
                        true
                    );
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                } else {
                    player.displayClientMessage(
                        Component.translatable("message.beemancer.storage_relay.out_of_range"),
                        true
                    );
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
        }

        // Vérifier si c'est un conteneur de stockage supporté
        if (!StorageHelper.isStorageContainer(clickedState)) {
            return;
        }

        // Enregistrer/désenregistrer le coffre via le noeud
        boolean wasRegistered = node.getRegisteredChests().contains(clickedPos);
        boolean success = node.toggleChest(clickedPos);

        if (success) {
            if (wasRegistered) {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_controller.chest_removed"),
                    true
                );
            } else {
                int count = node.getRegisteredChests().size();
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_controller.chests_registered", count),
                    true
                );
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Cherche le controller racine dans le réseau de noeuds.
     * Remonte les connexions pour trouver le StorageControllerBlockEntity.
     */
    private static BlockPos findControllerInNetwork(INetworkNode startNode, Level level) {
        if (startNode instanceof com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity) {
            return startNode.getNodePos();
        }
        // BFS pour trouver le controller
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        visited.add(startNode.getNodePos());
        for (BlockPos connected : startNode.getConnectedNodes()) {
            queue.add(connected);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (visited.contains(pos)) continue;
            visited.add(pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity) {
                return pos;
            }
            if (be instanceof INetworkNode node) {
                for (BlockPos connected : node.getConnectedNodes()) {
                    if (!visited.contains(connected)) {
                        queue.add(connected);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Nettoie le mode édition quand un joueur se déconnecte.
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        StorageEditModeHandler.stopEditing(event.getEntity().getUUID());
    }

    /**
     * Nettoie tous les modes édition quand le serveur s'arrête.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        StorageEditModeHandler.clearAll();
    }
}
