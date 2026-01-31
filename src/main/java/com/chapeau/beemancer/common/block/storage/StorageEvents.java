/**
 * ============================================================
 * [StorageEvents.java]
 * Description: Gestionnaire d'evenements pour le systeme de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageEditModeHandler        | Etat edition         | Verification mode              |
 * | INetworkNode                  | Interface noeud      | Enregistrement coffres         |
 * | StorageHelper                 | Verification coffres | isStorageContainer             |
 * | StorageNetworkRegistry        | Registre central     | Propriete exclusive            |
 * | StorageControllerBlockEntity  | Controller reseau    | Acces registre                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Beemancer.java (enregistrement evenements)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.INetworkNode;
import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageNetworkRegistry;
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Gere les evenements lies au systeme de stockage.
 *
 * Intercepte les clics droits sur les coffres quand un joueur
 * est en mode edition d'un noeud de reseau (controller ou relay).
 *
 * Enforce la propriete exclusive: un bloc ne peut etre lie qu'a un seul noeud.
 */
public class StorageEvents {

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

        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(clickedPos);

            // Shift+clic sur un terminal = lier au reseau via le noeud courant
            if (be instanceof StorageTerminalBlockEntity terminal) {
                handleTerminalLink(player, level, node, terminal, clickedPos, event);
                return;
            }

            // Shift+clic sur une interface reseau = lier au reseau via le noeud courant
            if (be instanceof NetworkInterfaceBlockEntity iface) {
                handleInterfaceLink(player, level, node, iface, clickedPos, event);
                return;
            }

            // Shift+clic sur un autre noeud = enregistrer mutuellement
            if (be instanceof INetworkNode otherNode && !clickedPos.equals(nodePos)) {
                handleNodeLink(player, node, otherNode, nodePos, clickedPos, event);
                return;
            }
        }

        // Verifier si c'est un conteneur de stockage supporte
        if (!StorageHelper.isStorageContainer(clickedState)) {
            return;
        }

        // Enregistrer/desenregistrer le coffre via le noeud + registre
        handleChestToggle(player, level, node, clickedPos, event);
    }

    /**
     * Lie un terminal au reseau avec propriete exclusive.
     */
    private static void handleTerminalLink(Player player, Level level, INetworkNode node,
                                            StorageTerminalBlockEntity terminal, BlockPos clickedPos,
                                            PlayerInteractEvent.RightClickBlock event) {
        BlockPos controllerPos = findControllerInNetwork(node, level);
        if (controllerPos == null) return;

        BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
        if (!(ctrlBe instanceof StorageControllerBlockEntity controller)) return;

        // Propriete exclusive via le registre
        boolean registered = controller.getNetworkRegistry().registerBlock(
                clickedPos, node.getNodePos(), StorageNetworkRegistry.NetworkBlockType.TERMINAL);

        if (!registered) {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_network.already_registered"),
                    true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        terminal.linkToController(controllerPos);
        controller.setChanged();
        controller.syncNodeToClient();

        player.displayClientMessage(
                Component.translatable("message.beemancer.storage_terminal.linked_manually"),
                true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Lie une interface au reseau avec propriete exclusive.
     */
    private static void handleInterfaceLink(Player player, Level level, INetworkNode node,
                                             NetworkInterfaceBlockEntity iface, BlockPos clickedPos,
                                             PlayerInteractEvent.RightClickBlock event) {
        BlockPos controllerPos = findControllerInNetwork(node, level);
        if (controllerPos == null) return;

        BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
        if (!(ctrlBe instanceof StorageControllerBlockEntity controller)) return;

        // Propriete exclusive via le registre
        boolean registered = controller.getNetworkRegistry().registerBlock(
                clickedPos, node.getNodePos(), StorageNetworkRegistry.NetworkBlockType.INTERFACE);

        if (!registered) {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_network.already_registered"),
                    true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        iface.linkToController(controllerPos);
        controller.setChanged();
        controller.syncNodeToClient();

        player.displayClientMessage(
                Component.translatable("message.beemancer.network_interface.linked_manually"),
                true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Lie deux noeuds mutuellement.
     */
    private static void handleNodeLink(Player player, INetworkNode node, INetworkNode otherNode,
                                        BlockPos nodePos, BlockPos clickedPos,
                                        PlayerInteractEvent.RightClickBlock event) {
        if (node.getChestManager().isInRange(clickedPos)) {
            node.connectNode(clickedPos);
            otherNode.connectNode(nodePos);
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_relay.node_linked"),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_relay.out_of_range"),
                    true);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Toggle un coffre avec enregistrement dans le registre central.
     */
    private static void handleChestToggle(Player player, Level level, INetworkNode node,
                                           BlockPos clickedPos,
                                           PlayerInteractEvent.RightClickBlock event) {
        // Trouver le controller pour acceder au registre
        BlockPos controllerPos = findControllerInNetwork(node, level);
        StorageControllerBlockEntity controller = null;
        if (controllerPos != null) {
            BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
            if (ctrlBe instanceof StorageControllerBlockEntity ctrl) {
                controller = ctrl;
            }
        }

        Set<BlockPos> oldChests = new HashSet<>(node.getRegisteredChests());
        boolean success = node.toggleChest(clickedPos);

        if (success) {
            Set<BlockPos> newChests = node.getRegisteredChests();

            if (controller != null) {
                StorageNetworkRegistry registry = controller.getNetworkRegistry();

                // Enregistrer les nouveaux coffres dans le registre
                for (BlockPos added : newChests) {
                    if (!oldChests.contains(added)) {
                        boolean ok = registry.registerBlock(added, node.getNodePos(),
                                StorageNetworkRegistry.NetworkBlockType.CHEST);
                        if (!ok) {
                            // Deja possede par un autre noeud — retirer du chestManager local
                            node.getChestManager().getRegisteredChestsMutable().remove(added);
                        }
                    }
                }

                // Retirer les coffres supprimes du registre
                for (BlockPos removed : oldChests) {
                    if (!newChests.contains(removed)) {
                        registry.unregisterBlock(removed);
                    }
                }

                controller.setChanged();
                controller.syncNodeToClient();
            }

            boolean wasRegistered = oldChests.contains(clickedPos);
            if (wasRegistered) {
                player.displayClientMessage(
                        Component.translatable("message.beemancer.storage_controller.chest_removed"),
                        true);
            } else {
                int count = node.getRegisteredChests().size();
                player.displayClientMessage(
                        Component.translatable("message.beemancer.storage_controller.chests_registered", count),
                        true);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Cherche le controller racine dans le reseau de noeuds via BFS.
     */
    private static BlockPos findControllerInNetwork(INetworkNode startNode, Level level) {
        if (startNode instanceof StorageControllerBlockEntity) {
            return startNode.getNodePos();
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        visited.add(startNode.getNodePos());

        for (BlockPos connected : startNode.getConnectedNodes()) {
            queue.add(connected);
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;
            if (!level.isLoaded(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageControllerBlockEntity) {
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

    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        StorageEditModeHandler.stopEditing(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        StorageEditModeHandler.clearAll();
    }
}
