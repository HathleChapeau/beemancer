/**
 * ============================================================
 * [ItemPipeNetworkManager.java]
 * Description: Gestionnaire SavedData de tous les réseaux de pipes d'items par dimension
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | PipeNetwork         | Réseau connexe       | Gestion des réseaux            |
 * | PipeGraph           | Structure graphe     | Reconstruction et splits       |
 * | AbstractPipeBlock   | Détection connexions | Scanner les voisins            |
 * | ItemPipeBlock       | Type de pipe         | Vérification type bloc         |
 * | SavedData           | Persistance          | Sauvegarde par dimension       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ItemPipeBlock.java (onPlace, onRemove)
 * - ItemPipeBlockEntity.java (getNetworkAt pour routage)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.pipe;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.alchemy.AbstractPipeBlock;
import com.chapeau.apica.common.block.alchemy.ItemPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Singleton SavedData par ServerLevel qui gère tous les réseaux de pipes d'items.
 * Responsabilités : création, merge, split, destruction de réseaux,
 * et mapping position → réseau pour lookup rapide.
 */
public class ItemPipeNetworkManager extends SavedData {
    private static final String DATA_NAME = Apica.MOD_ID + "_item_pipe_networks";

    private final Map<UUID, PipeNetwork> networks = new HashMap<>();
    private final Map<BlockPos, UUID> positionToNetwork = new HashMap<>();

    // --- Obtention du singleton ---

    public static ItemPipeNetworkManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(ItemPipeNetworkManager::new, ItemPipeNetworkManager::load),
            DATA_NAME
        );
    }

    // --- Requêtes ---

    @Nullable
    public PipeNetwork getNetworkAt(BlockPos pos) {
        UUID id = positionToNetwork.get(pos);
        return id != null ? networks.get(id) : null;
    }

    // --- Cycle de vie ---

    /**
     * Appelé quand un pipe est placé dans le monde.
     * Rejoint un réseau existant, en crée un nouveau, ou merge plusieurs réseaux.
     */
    public void onPipeAdded(BlockPos pos, ServerLevel level) {
        if (positionToNetwork.containsKey(pos)) return;

        // Collecter les réseaux des voisins connectés
        Set<UUID> neighborNetworkIds = new HashSet<>();
        BlockState state = level.getBlockState(pos);

        for (Direction dir : Direction.values()) {
            if (!AbstractPipeBlock.isConnected(state, dir)) continue;
            BlockPos neighbor = pos.relative(dir);
            UUID neighborNetId = positionToNetwork.get(neighbor);
            if (neighborNetId != null) {
                neighborNetworkIds.add(neighborNetId);
            }
        }

        if (neighborNetworkIds.isEmpty()) {
            // Aucun voisin : créer un nouveau réseau
            PipeNetwork network = new PipeNetwork(UUID.randomUUID());
            network.addPipe(pos);
            network.refreshEndpoint(pos, level);
            registerNetwork(network, pos);
        } else if (neighborNetworkIds.size() == 1) {
            // Un seul réseau voisin : rejoindre
            UUID netId = neighborNetworkIds.iterator().next();
            PipeNetwork network = networks.get(netId);
            network.addPipe(pos);
            network.connectPipeToNeighbors(pos, level);
            network.refreshEndpoint(pos, level);
            positionToNetwork.put(pos, netId);
            refreshNeighborEndpoints(pos, network, level);
        } else {
            // Plusieurs réseaux : merge
            mergeNetworks(neighborNetworkIds, pos, level);
        }

        setDirty();
    }

    /**
     * Appelé quand un pipe est retiré du monde.
     * Retire du réseau, et split si nécessaire.
     */
    public void onPipeRemoved(BlockPos pos, ServerLevel level) {
        UUID netId = positionToNetwork.remove(pos);
        if (netId == null) return;

        PipeNetwork network = networks.get(netId);
        if (network == null) return;

        network.removePipe(pos);

        if (network.size() == 0) {
            networks.remove(netId);
        } else {
            // Vérifier si le réseau est splitté
            List<Set<BlockPos>> components = network.getGraph().findConnectedComponents();
            if (components.size() > 1) {
                splitNetwork(netId, components, level);
            } else {
                network.rebuildAllEndpoints(level);
            }
        }

        // Rafraîchir les endpoints des voisins (la machine de ce pipe n'est plus accessible)
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            UUID neighborNetId = positionToNetwork.get(neighbor);
            if (neighborNetId != null) {
                PipeNetwork neighborNet = networks.get(neighborNetId);
                if (neighborNet != null) {
                    neighborNet.refreshEndpoint(neighbor, level);
                }
            }
        }

        setDirty();
    }

    /**
     * Appelé quand une connexion est togglee (disconnect/reconnect/extract toggle).
     * Invalidate le cache et recalcule les endpoints et arêtes.
     */
    public void onConnectionChanged(BlockPos pos, ServerLevel level) {
        UUID netId = positionToNetwork.get(pos);

        // Re-scanner les connexions complètes (le pipe peut avoir gagné ou perdu des voisins)
        // D'abord retirer cette pipe du réseau actuel
        if (netId != null) {
            positionToNetwork.remove(pos);
            PipeNetwork network = networks.get(netId);
            if (network != null) {
                network.removePipe(pos);
                if (network.size() == 0) {
                    networks.remove(netId);
                } else {
                    List<Set<BlockPos>> components = network.getGraph().findConnectedComponents();
                    if (components.size() > 1) {
                        splitNetwork(netId, components, level);
                    } else {
                        network.rebuildAllEndpoints(level);
                    }
                }
            }
        }

        // Puis la re-ajouter (elle trouvera ses nouveaux voisins)
        onPipeAdded(pos, level);
    }

    // --- Merge ---

    private void mergeNetworks(Set<UUID> networkIds, BlockPos newPipePos, ServerLevel level) {
        // Choisir le plus grand réseau comme survivant
        UUID survivorId = null;
        int maxSize = -1;
        for (UUID id : networkIds) {
            PipeNetwork net = networks.get(id);
            if (net != null && net.size() > maxSize) {
                maxSize = net.size();
                survivorId = id;
            }
        }
        if (survivorId == null) return;

        PipeNetwork survivor = networks.get(survivorId);

        // Absorber les autres réseaux
        for (UUID id : networkIds) {
            if (id.equals(survivorId)) continue;
            PipeNetwork absorbed = networks.remove(id);
            if (absorbed == null) continue;

            for (BlockPos pos : absorbed.getGraph().getAllNodes()) {
                survivor.addPipe(pos);
                positionToNetwork.put(pos, survivorId);
            }
        }

        // Ajouter la nouvelle pipe
        survivor.addPipe(newPipePos);
        positionToNetwork.put(newPipePos, survivorId);

        // Reconstruire le graphe complet depuis le monde
        rebuildGraphFromWorld(survivor, level);
        survivor.rebuildAllEndpoints(level);
    }

    // --- Split ---

    private void splitNetwork(UUID originalId, List<Set<BlockPos>> components, ServerLevel level) {
        // La plus grande composante garde le réseau original
        int largestIdx = 0;
        for (int i = 1; i < components.size(); i++) {
            if (components.get(i).size() > components.get(largestIdx).size()) {
                largestIdx = i;
            }
        }

        PipeNetwork original = networks.get(originalId);

        for (int i = 0; i < components.size(); i++) {
            Set<BlockPos> component = components.get(i);
            if (i == largestIdx) {
                // Reconstruire le graphe du réseau original avec seulement cette composante
                PipeGraph newGraph = PipeGraph.buildFromWorld(component, level);
                PipeNetwork rebuilt = new PipeNetwork(originalId, newGraph);
                rebuilt.rebuildAllEndpoints(level);
                networks.put(originalId, rebuilt);
                for (BlockPos pos : component) {
                    positionToNetwork.put(pos, originalId);
                }
            } else {
                // Créer un nouveau réseau pour cette composante
                UUID newId = UUID.randomUUID();
                PipeGraph newGraph = PipeGraph.buildFromWorld(component, level);
                PipeNetwork newNetwork = new PipeNetwork(newId, newGraph);
                newNetwork.rebuildAllEndpoints(level);
                networks.put(newId, newNetwork);
                for (BlockPos pos : component) {
                    positionToNetwork.put(pos, newId);
                }
            }
        }
    }

    // --- Helpers ---

    private void registerNetwork(PipeNetwork network, BlockPos pos) {
        networks.put(network.getId(), network);
        positionToNetwork.put(pos, network.getId());
    }

    private void refreshNeighborEndpoints(BlockPos changedPos, PipeNetwork network, ServerLevel level) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = changedPos.relative(dir);
            if (network.getGraph().contains(neighbor) && level.hasChunkAt(neighbor)) {
                network.refreshEndpoint(neighbor, level);
            }
        }
    }

    private void rebuildGraphFromWorld(PipeNetwork network, ServerLevel level) {
        Set<BlockPos> allPositions = new HashSet<>(network.getGraph().getAllNodes());
        PipeGraph rebuilt = PipeGraph.buildFromWorld(allPositions, level);
        // Remplacer le graphe interne — on utilise addEdge car les noeuds sont déjà dans le graphe
        // Nettoyage : reconstruire depuis zéro
        for (BlockPos pos : allPositions) {
            network.getGraph().removeNode(pos);
        }
        for (BlockPos pos : rebuilt.getAllNodes()) {
            network.getGraph().addNode(pos);
            for (BlockPos neighbor : rebuilt.getNeighbors(pos)) {
                network.getGraph().addEdge(pos, neighbor);
            }
        }
    }

    // --- Sérialisation SavedData ---

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag networkList = new ListTag();
        for (PipeNetwork network : networks.values()) {
            networkList.add(network.save(registries));
        }
        tag.put("Networks", networkList);
        return tag;
    }

    private static ItemPipeNetworkManager load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemPipeNetworkManager manager = new ItemPipeNetworkManager();
        ListTag networkList = tag.getList("Networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < networkList.size(); i++) {
            PipeNetwork network = PipeNetwork.load(networkList.getCompound(i), registries);
            manager.networks.put(network.getId(), network);
            for (BlockPos pos : network.getGraph().getAllNodes()) {
                manager.positionToNetwork.put(pos, network.getId());
            }
        }
        return manager;
    }

    /**
     * Reconstruit les graphes et endpoints de tous les réseaux depuis le monde.
     * Appelé au chargement pour recréer les arêtes et les endpoints.
     */
    public void rebuildAllFromWorld(ServerLevel level) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, PipeNetwork> entry : networks.entrySet()) {
            PipeNetwork network = entry.getValue();
            Set<BlockPos> positions = new HashSet<>(network.getGraph().getAllNodes());

            // Valider que les pipes existent encore dans le monde
            positions.removeIf(pos ->
                !level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof ItemPipeBlock)
            );

            if (positions.isEmpty()) {
                toRemove.add(entry.getKey());
                continue;
            }

            // Reconstruire le graphe depuis le monde
            PipeGraph rebuilt = PipeGraph.buildFromWorld(positions, level);
            for (BlockPos pos : new HashSet<>(network.getGraph().getAllNodes())) {
                network.getGraph().removeNode(pos);
            }
            for (BlockPos pos : rebuilt.getAllNodes()) {
                network.getGraph().addNode(pos);
                for (BlockPos neighbor : rebuilt.getNeighbors(pos)) {
                    network.getGraph().addEdge(pos, neighbor);
                }
            }
            network.rebuildAllEndpoints(level);
        }

        for (UUID id : toRemove) {
            networks.remove(id);
            positionToNetwork.values().removeIf(netId -> netId.equals(id));
        }
    }
}
