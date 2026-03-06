/**
 * ============================================================
 * [PipeNetwork.java]
 * Description: Réseau connexe de pipes avec graphe, endpoints et routage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | PipeGraph           | Structure graphe     | Topologie du réseau            |
 * | PipeRouteCache      | Cache routes         | Optimisation BFS               |
 * | PipeEndpoint        | Points d'entrée      | Machines connectées            |
 * | AbstractPipeBlock   | Blockstate connexions| Détection extract/insert       |
 * | ItemPipeBlock       | Type de pipe         | Vérification type              |
 * | Capabilities        | NeoForge items       | Détection IItemHandler         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ItemPipeNetworkManager.java (gestion des réseaux)
 * - ItemPipeBlockEntity.java (requêtes de routage)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.pipe;

import com.chapeau.apica.common.block.alchemy.AbstractPipeBlock;
import com.chapeau.apica.common.block.alchemy.ItemPipeBlock;
import com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.apica.common.data.ItemFilterData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un réseau connexe de pipes d'items.
 * Contient le graphe topologique, les endpoints (machines connectées),
 * un round-robin global pour la distribution, et un cache de routes BFS.
 */
public class PipeNetwork {
    private final UUID id;
    private final PipeGraph graph;
    private final PipeRouteCache routeCache = new PipeRouteCache();
    private final List<PipeEndpoint> endpoints = new ArrayList<>();
    private int roundRobinIndex = 0;

    public PipeNetwork(UUID id) {
        this.id = id;
        this.graph = new PipeGraph();
    }

    public PipeNetwork(UUID id, PipeGraph graph) {
        this.id = id;
        this.graph = graph;
    }

    public UUID getId() { return id; }
    public PipeGraph getGraph() { return graph; }
    public int size() { return graph.size(); }
    public List<PipeEndpoint> getEndpoints() { return endpoints; }

    public void addPipe(BlockPos pos) {
        graph.addNode(pos);
    }

    public void removePipe(BlockPos pos) {
        graph.removeNode(pos);
        endpoints.removeIf(e -> e.pipePos().equals(pos));
        routeCache.invalidateAll();
    }

    /**
     * Ajoute les arêtes entre un pipe et ses voisins déjà dans le graphe.
     */
    public void connectPipeToNeighbors(BlockPos pos, ServerLevel level) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ItemPipeBlock)) return;

        for (Direction dir : Direction.values()) {
            if (!AbstractPipeBlock.isConnected(state, dir)) continue;
            BlockPos neighbor = pos.relative(dir);
            if (graph.contains(neighbor)) {
                graph.addEdge(pos, neighbor);
            }
        }
        routeCache.invalidateAll();
    }

    /**
     * Recalcule les endpoints (machines connectées) pour un pipe donné.
     * Scanne les 6 faces pour trouver les IItemHandler non-pipe.
     */
    public void refreshEndpoint(BlockPos pipePos, ServerLevel level) {
        endpoints.removeIf(e -> e.pipePos().equals(pipePos));
        BlockState state = level.getBlockState(pipePos);
        if (!(state.getBlock() instanceof ItemPipeBlock)) return;

        for (Direction dir : Direction.values()) {
            if (!AbstractPipeBlock.isConnected(state, dir)) continue;
            BlockPos neighborPos = pipePos.relative(dir);

            // Ignorer les autres pipes du même type
            if (level.getBlockState(neighborPos).getBlock() instanceof ItemPipeBlock) continue;

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (handler == null) continue;

            boolean extracting = level.getBlockEntity(pipePos) instanceof
                com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity pipe && pipe.isExtracting(dir);
            if (extracting) {
                endpoints.add(new PipeEndpoint(pipePos, dir, neighborPos, PipeEndpoint.EndpointType.EXTRACT));
            } else {
                endpoints.add(new PipeEndpoint(pipePos, dir, neighborPos, PipeEndpoint.EndpointType.INSERT));
            }
        }
    }

    /**
     * Recalcule tous les endpoints du réseau.
     */
    public void rebuildAllEndpoints(ServerLevel level) {
        endpoints.clear();
        for (BlockPos pos : graph.getAllNodes()) {
            if (level.hasChunkAt(pos)) {
                refreshEndpoint(pos, level);
            }
        }
    }

    /**
     * Trouve la prochaine destination valide avec support des filtres.
     * Les pipes avec filtre qui refuse l'item sont non-traversables.
     * Parmi les routes valides, prefere celles passant par des pipes de priority plus haute.
     * @param excludeMachinePos position de la machine source a exclure (evite les boucles)
     */
    @Nullable
    public RouteResult findDestination(BlockPos sourcePipePos, ItemStack stack, ServerLevel level,
                                        @Nullable BlockPos excludeMachinePos) {
        List<PipeEndpoint> insertEndpoints = endpoints.stream()
            .filter(e -> e.type() == PipeEndpoint.EndpointType.INSERT)
            .filter(e -> excludeMachinePos == null || !e.machinePos().equals(excludeMachinePos))
            .toList();

        if (insertEndpoints.isEmpty()) return null;

        // Collecter toutes les routes valides avec leur priority
        List<ScoredRoute> validRoutes = new ArrayList<>();
        for (PipeEndpoint candidate : insertEndpoints) {
            if (!level.hasChunkAt(candidate.machinePos())) continue;
            IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, candidate.machinePos(), candidate.face().getOpposite());
            if (handler == null) continue;
            if (!canInsertAny(handler, stack)) continue;

            // Verifier que l'item peut traverser le pipe source lui-meme
            if (!canItemTraverse(sourcePipePos, stack, level)) continue;

            // BFS item-aware : skip pipes dont le filtre refuse l'item
            List<BlockPos> route = graph.bfsPathFiltered(sourcePipePos, candidate.pipePos(),
                pos -> canItemTraverse(pos, stack, level));
            if (route == null) continue;

            int maxPriority = getMaxPriorityOnRoute(route, level);
            validRoutes.add(new ScoredRoute(candidate, route, maxPriority));
        }

        if (validRoutes.isEmpty()) return null;

        // Trier par priority desc
        validRoutes.sort(Comparator.comparingInt(ScoredRoute::priority).reversed());
        int topPriority = validRoutes.get(0).priority();
        List<ScoredRoute> topRoutes = validRoutes.stream()
            .filter(r -> r.priority() == topPriority).toList();

        // Round-robin dans le groupe le plus prioritaire
        int idx = roundRobinIndex % topRoutes.size();
        roundRobinIndex = (idx + 1) % topRoutes.size();
        ScoredRoute chosen = topRoutes.get(idx);
        return new RouteResult(chosen.endpoint(), chosen.route());
    }

    /**
     * Verifie si un item peut traverser un pipe (en tenant compte du filtre).
     */
    private boolean canItemTraverse(BlockPos pos, ItemStack stack, ServerLevel level) {
        if (!level.hasChunkAt(pos)) return true;
        if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            ItemFilterData filter = pipe.getFilter();
            if (filter != null) {
                return filter.matches(stack);
            }
        }
        return true;
    }

    /**
     * Retourne la priority maximale sur une route (0 si aucun filtre).
     */
    private int getMaxPriorityOnRoute(List<BlockPos> route, ServerLevel level) {
        int maxPriority = 0;
        for (BlockPos pos : route) {
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
                ItemFilterData filter = pipe.getFilter();
                if (filter != null && filter.getPriority() > maxPriority) {
                    maxPriority = filter.getPriority();
                }
            }
        }
        return maxPriority;
    }

    private boolean canInsertAny(IItemHandler handler, ItemStack stack) {
        ItemStack testStack = stack.copyWithCount(1);
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.insertItem(i, testStack, true).isEmpty()) return true;
        }
        return false;
    }

    public void invalidateRouteCache() {
        routeCache.invalidateAll();
    }

    /**
     * Résultat d'un routage : endpoint destination + route à suivre.
     */
    public record RouteResult(PipeEndpoint endpoint, List<BlockPos> route) {}

    /**
     * Route avec score de priority pour le tri.
     */
    private record ScoredRoute(PipeEndpoint endpoint, List<BlockPos> route, int priority) {}

    // --- Sérialisation NBT ---

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putInt("RoundRobin", roundRobinIndex);

        ListTag pipesTag = new ListTag();
        for (BlockPos pos : graph.getAllNodes()) {
            CompoundTag posTag = new CompoundTag();
            posTag.put("Pos", NbtUtils.writeBlockPos(pos));
            pipesTag.add(posTag);
        }
        tag.put("Pipes", pipesTag);

        return tag;
    }

    public static PipeNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID id = tag.getUUID("Id");
        int roundRobin = tag.getInt("RoundRobin");

        Set<BlockPos> positions = new HashSet<>();
        ListTag pipesTag = tag.getList("Pipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < pipesTag.size(); i++) {
            NbtUtils.readBlockPos(pipesTag.getCompound(i), "Pos").ifPresent(positions::add);
        }

        PipeNetwork network = new PipeNetwork(id);
        for (BlockPos pos : positions) {
            network.graph.addNode(pos);
        }
        network.roundRobinIndex = roundRobin;
        return network;
    }
}
