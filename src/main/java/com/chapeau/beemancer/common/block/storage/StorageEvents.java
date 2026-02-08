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
import com.chapeau.beemancer.common.blockentity.storage.StorageHiveBlockEntity;
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
import net.neoforged.neoforge.event.level.BlockEvent;
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
        BlockEntity be = level.getBlockEntity(clickedPos);

        // Terminal: toggle link/unlink (en edit mode, pas besoin de shift)
        if (be instanceof StorageTerminalBlockEntity terminal) {
            handleTerminalToggle(player, level, node, terminal, clickedPos, event);
            return;
        }

        // Storage Hive: toggle link/unlink (controller edit mode only)
        if (be instanceof StorageHiveBlockEntity hive) {
            handleHiveToggle(player, level, node, hive, clickedPos, event);
            return;
        }

        // Interface: toggle link/unlink (en edit mode, pas besoin de shift)
        if (be instanceof NetworkInterfaceBlockEntity iface) {
            handleInterfaceToggle(player, level, node, iface, clickedPos, event);
            return;
        }

        if (player.isShiftKeyDown()) {
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
     * Toggle un terminal: si deja lie, delie; sinon, lie au reseau.
     */
    private static void handleTerminalToggle(Player player, Level level, INetworkNode node,
                                              StorageTerminalBlockEntity terminal, BlockPos clickedPos,
                                              PlayerInteractEvent.RightClickBlock event) {
        BlockPos controllerPos = findControllerInNetwork(node, level);
        if (controllerPos == null) return;

        BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
        if (!(ctrlBe instanceof StorageControllerBlockEntity controller)) return;

        StorageNetworkRegistry registry = controller.getNetworkRegistry();

        if (terminal.getControllerPos() != null) {
            // Deja lie: delink
            registry.unregisterBlock(clickedPos);
            terminal.unlinkController();
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_terminal.unlinked"),
                    true);
        } else {
            // Pas lie: link
            registry.registerBlock(
                    clickedPos, node.getNodePos(), StorageNetworkRegistry.NetworkBlockType.TERMINAL);
            terminal.linkToController(controllerPos);
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_terminal.linked_manually"),
                    true);
        }

        controller.setChanged();
        controller.syncNodeToClient();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Toggle une interface: si deja liee, delie; sinon, lie au reseau.
     */
    private static void handleInterfaceToggle(Player player, Level level, INetworkNode node,
                                               NetworkInterfaceBlockEntity iface, BlockPos clickedPos,
                                               PlayerInteractEvent.RightClickBlock event) {
        BlockPos controllerPos = findControllerInNetwork(node, level);
        if (controllerPos == null) return;

        BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
        if (!(ctrlBe instanceof StorageControllerBlockEntity controller)) return;

        StorageNetworkRegistry registry = controller.getNetworkRegistry();

        if (iface.getControllerPos() != null) {
            // Deja liee: delink
            registry.unregisterBlock(clickedPos);
            iface.unlinkController();
            player.displayClientMessage(
                    Component.translatable("message.beemancer.network_interface.unlinked"),
                    true);
        } else {
            // Pas liee: link
            registry.registerBlock(
                    clickedPos, node.getNodePos(), StorageNetworkRegistry.NetworkBlockType.INTERFACE);
            iface.linkToController(controllerPos);
            player.displayClientMessage(
                    Component.translatable("message.beemancer.network_interface.linked_manually"),
                    true);
        }

        controller.setChanged();
        controller.syncNodeToClient();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Toggle une Storage Hive: lien direct au controller uniquement (pas relay).
     * Max 4 hives par controller.
     */
    private static void handleHiveToggle(Player player, Level level, INetworkNode node,
                                          StorageHiveBlockEntity hive, BlockPos clickedPos,
                                          PlayerInteractEvent.RightClickBlock event) {
        // Les hives ne peuvent etre liees qu'en mode edition du controller
        if (!(node instanceof StorageControllerBlockEntity controller)) {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_hive.controller_only"),
                    true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        StorageNetworkRegistry registry = controller.getNetworkRegistry();

        if (hive.getControllerPos() != null) {
            // Deja liee: unlink (unlinkHive notifie la hive automatiquement)
            controller.unlinkHive(clickedPos);
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_hive.unlinked"),
                    true);
        } else {
            // Verifier le max de 4 hives
            if (registry.getHiveCount() >= StorageControllerBlockEntity.MAX_LINKED_HIVES) {
                player.displayClientMessage(
                        Component.translatable("message.beemancer.storage_hive.max_reached"),
                        true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }

            // Link (linkHive est self-contained, notifie la hive automatiquement)
            controller.linkHive(clickedPos);
            player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_hive.linked"),
                    true);
        }

        controller.setChanged();
        controller.syncNodeToClient();
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

                // Enregistrer les nouveaux coffres dans le registre (transfere si deja possede)
                for (BlockPos added : newChests) {
                    if (!oldChests.contains(added)) {
                        registry.registerBlock(added, node.getNodePos(),
                                StorageNetworkRegistry.NetworkBlockType.CHEST);
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

    /**
     * Quand un bloc est casse en mode edition, le retirer du registre reseau
     * et de la liste de coffres du noeud si applicable.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();
        BlockPos brokenPos = event.getPos();

        if (level.isClientSide()) return;
        if (!StorageEditModeHandler.isEditing(player, level)) return;

        BlockPos nodePos = StorageEditModeHandler.getEditingNode(player.getUUID());
        if (nodePos == null) return;

        BlockEntity nodeBe = level.getBlockEntity(nodePos);
        if (!(nodeBe instanceof INetworkNode node)) return;

        BlockPos controllerPos = findControllerInNetwork(node, level);
        if (controllerPos == null) return;

        BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
        if (!(ctrlBe instanceof StorageControllerBlockEntity controller)) return;

        StorageNetworkRegistry registry = controller.getNetworkRegistry();
        StorageNetworkRegistry.NetworkEntry entry = registry.getEntry(brokenPos);
        if (entry == null) return;

        // Retirer du registre central
        registry.unregisterBlock(brokenPos);

        // Si c'est un coffre, retirer aussi de la liste du noeud proprietaire
        if (entry.type() == StorageNetworkRegistry.NetworkBlockType.CHEST) {
            BlockPos ownerPos = entry.ownerNode();
            if (ownerPos != null && level.isLoaded(ownerPos)) {
                BlockEntity ownerBe = level.getBlockEntity(ownerPos);
                if (ownerBe instanceof INetworkNode ownerNode) {
                    ownerNode.getChestManager().getRegisteredChestsMutable().remove(brokenPos);
                }
            }
        }

        controller.setChanged();
        controller.syncNodeToClient();
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
