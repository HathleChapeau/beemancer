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

            if (AbstractPipeBlock.isExtracting(state, dir)) {
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
     * Trouve la prochaine destination valide en round-robin global.
     * Pre-valide que la machine accepte l'item (simulation d'insertion).
     * Retourne l'endpoint + la route, ou null si aucune destination valide.
     */
    @Nullable
    public RouteResult findDestination(BlockPos sourcePipePos, ItemStack stack, ServerLevel level) {
        List<PipeEndpoint> insertEndpoints = endpoints.stream()
            .filter(e -> e.type() == PipeEndpoint.EndpointType.INSERT)
            .toList();

        if (insertEndpoints.isEmpty()) return null;

        int startIndex = roundRobinIndex % insertEndpoints.size();

        for (int i = 0; i < insertEndpoints.size(); i++) {
            int idx = (startIndex + i) % insertEndpoints.size();
            PipeEndpoint candidate = insertEndpoints.get(idx);

            // Pre-validation : la machine accepte-t-elle cet item ?
            if (!level.hasChunkAt(candidate.machinePos())) continue;
            IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, candidate.machinePos(), candidate.face().getOpposite());
            if (handler == null) continue;

            if (!canInsertAny(handler, stack)) continue;

            // Calcul de la route
            List<BlockPos> route = routeCache.getCachedRoute(sourcePipePos, candidate.pipePos());
            if (route == null) {
                route = graph.bfsPath(sourcePipePos, candidate.pipePos());
                if (route != null) {
                    routeCache.putRoute(sourcePipePos, candidate.pipePos(), route);
                }
            }
            if (route == null) continue;

            // Destination trouvée — avancer le round-robin
            roundRobinIndex = (idx + 1) % insertEndpoints.size();
            return new RouteResult(candidate, route);
        }

        return null;
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
