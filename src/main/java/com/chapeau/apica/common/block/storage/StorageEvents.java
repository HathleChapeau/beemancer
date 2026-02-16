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
 * - Apica.java (enregistrement evenements)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.storage;

import com.chapeau.apica.common.blockentity.storage.INetworkNode;
import com.chapeau.apica.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageNetworkRegistry;
import com.chapeau.apica.core.multiblock.MultiblockEvents;
import com.chapeau.apica.core.util.StorageHelper;
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
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        if (!StorageEditModeHandler.isEditing(player, level)) return;

        BlockPos nodePos = StorageEditModeHandler.getEditingNode(player.getUUID());
        if (nodePos == null) return;

        BlockEntity nodeBe = level.getBlockEntity(nodePos);
        if (!(nodeBe instanceof INetworkNode node)) return;

        BlockState clickedState = level.getBlockState(clickedPos);
        BlockEntity be = level.getBlockEntity(clickedPos);

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

        // Verifier si c'est un conteneur de stockage supporte (capability IItemHandler ou whitelist legacy)
        if (!StorageHelper.hasItemHandlerCapability(level, clickedPos, null)
                && !StorageHelper.isStorageContainer(clickedState)) {
            return;
        }

        // Enregistrer/desenregistrer le coffre via le noeud + registre
        handleChestToggle(player, level, node, clickedPos, event);
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
                    Component.translatable("message.apica.network_interface.unlinked"),
                    true);
        } else {
            // Pas liee: link
            registry.registerBlock(
                    clickedPos, node.getNodePos(), StorageNetworkRegistry.NetworkBlockType.INTERFACE);
            iface.linkToController(controllerPos);
            player.displayClientMessage(
                    Component.translatable("message.apica.network_interface.linked_manually"),
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
                    Component.translatable("message.apica.storage_relay.node_linked"),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.apica.storage_relay.out_of_range"),
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

        // Calculer la position canonique pour les doubles chests (toujours LEFT)
        BlockPos canonical = StorageHelper.getCanonicalChestPos(level, clickedPos);

        // [BM] Deduplication: empecher l'enregistrement d'un coffre deja dans un autre reseau
        // Verifier a la fois clickedPos et la canonique (double chest: l'autre moitie pourrait etre enregistree)
        if (!node.getRegisteredChests().contains(canonical) && controllerPos != null) {
            BlockPos otherOwner = findOtherNetworkOwner(level, canonical, controllerPos);
            if (otherOwner != null) {
                player.displayClientMessage(
                        Component.translatable("message.apica.chest_already_registered"), true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
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

            // Utiliser la canonique pour verifier si c'etait un toggle on/off
            boolean wasRegistered = oldChests.contains(canonical);
            if (wasRegistered) {
                player.displayClientMessage(
                        Component.translatable("message.apica.storage_controller.chest_removed"),
                        true);
            } else {
                int count = node.getRegisteredChests().size();
                player.displayClientMessage(
                        Component.translatable("message.apica.storage_controller.chests_registered", count),
                        true);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * [BM] Verifie si un coffre est deja enregistre dans un autre reseau.
     * Verifie a la fois la position brute et la canonique (double chests).
     * @return la position du controller proprietaire, ou null si libre
     */
    @javax.annotation.Nullable
    private static BlockPos findOtherNetworkOwner(Level level, BlockPos chestPos, BlockPos excludeCtrl) {
        BlockPos canonical = StorageHelper.getCanonicalChestPos(level, chestPos);
        for (BlockPos ctrlPos : MultiblockEvents.getActiveControllers()) {
            if (ctrlPos.equals(excludeCtrl)) continue;
            if (!level.isLoaded(ctrlPos)) continue;
            BlockEntity be = level.getBlockEntity(ctrlPos);
            if (be instanceof StorageControllerBlockEntity otherCtrl && otherCtrl.isFormed()) {
                if (otherCtrl.getNetworkRegistry().isRegistered(chestPos)) return ctrlPos;
                if (!canonical.equals(chestPos) && otherCtrl.getNetworkRegistry().isRegistered(canonical)) {
                    return ctrlPos;
                }
            }
        }
        return null;
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

        // Chercher l'entree directe ou via la canonique (double chest: seule LEFT est enregistree)
        StorageNetworkRegistry.NetworkEntry entry = registry.getEntry(brokenPos);
        BlockPos registeredPos = brokenPos;
        if (entry == null) {
            BlockPos canonical = StorageHelper.getCanonicalChestPos(level, brokenPos);
            if (!canonical.equals(brokenPos)) {
                entry = registry.getEntry(canonical);
                registeredPos = canonical;
            }
        }
        if (entry == null) return;

        // Retirer du registre central
        registry.unregisterBlock(registeredPos);

        // Si c'est un coffre, retirer aussi de la liste du noeud proprietaire
        if (entry.type() == StorageNetworkRegistry.NetworkBlockType.CHEST) {
            BlockPos ownerPos = entry.ownerNode();
            if (ownerPos != null && level.isLoaded(ownerPos)) {
                BlockEntity ownerBe = level.getBlockEntity(ownerPos);
                if (ownerBe instanceof INetworkNode ownerNode) {
                    ownerNode.getChestManager().removeChest(registeredPos);
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

    // === Shutdown Guard ===
    // Flag global pour empecher les modifications monde (setChanged, syncToClient, level.setBlock)
    // pendant le shutdown du serveur. Pendant saveAllChunks(), re-dirtier un chunk deja sauve
    // cause une boucle infinie -> hang "saving world".
    private static volatile boolean shuttingDown = false;

    public static boolean isShuttingDown() {
        return shuttingDown;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageEvents.class);

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        shuttingDown = false;
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        shuttingDown = true;
        LOGGER.info("[Apica] Server stopping: shutdown guard ENABLED — all storage ticks disabled");
        StorageEditModeHandler.clearAll();
    }
}
