/**
 * ============================================================
 * [StorageRelayBlockEntity.java]
 * Description: BlockEntity pour le Storage Relay - noeud relais du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | AbstractNetworkNodeBlockEntity| Base reseau          | Edit mode, nodes, chests, sync |
 * | ApicaBlockEntities        | Type du BlockEntity  | Constructeur                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageRelayBlock.java (creation et interaction)
 * - StorageControllerRenderer.java (rendu mode edition - via INetworkNode)
 * - StorageRelayRenderer.java (rendu mode edition)
 * - StorageEvents.java (interception clics)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

/**
 * Noeud relais du reseau de stockage.
 * Etend la portee du controller en servant de point de passage
 * pour les abeilles de livraison et l'enregistrement de coffres.
 *
 * Herite de AbstractNetworkNodeBlockEntity: mode edition, noeuds connectes,
 * gestion coffres, synchronisation client, NBT commun.
 */
public class StorageRelayBlockEntity extends AbstractNetworkNodeBlockEntity {

    public StorageRelayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ApicaBlockEntities.STORAGE_RELAY.get(), pos, blockState);
    }

    // === Server Tick ===

    public static void serverTick(StorageRelayBlockEntity relay) {
        relay.tickEditMode();
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        isSaving = true;
        try {
            super.saveAdditional(tag, registries);
            saveCommon(tag);
        } finally {
            isSaving = false;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadCommon(tag);
    }

    // === Lifecycle ===

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            // [FIX] Pendant le shutdown, NE PAS appeler level.getBlockEntity().
            // Si un noeud a deja ete remove du chunk, getBlockEntity() le RE-CREE
            // (EntityCreationType.IMMEDIATE), ce qui re-dirtie le chunk → boucle infinie.
            if (com.chapeau.apica.common.block.storage.StorageEvents.isShuttingDown()) return;

            // [FIX] Silent disconnect: disconnectNode() appelait syncToClient() sur chaque noeud
            // Cela causait des modifications monde en cascade pendant le world unload → hang "saving world"
            for (BlockPos nodePos : new ArrayList<>(getConnectedNodes())) {
                if (!level.hasChunkAt(nodePos)) continue;
                BlockEntity be = level.getBlockEntity(nodePos);
                if (be instanceof AbstractNetworkNodeBlockEntity node) {
                    node.disconnectNodeSilent(worldPosition);
                }
            }

            // Retirer les blocs possedes par ce relay du registre central
            findControllerAndUnregister();
        }
    }

    /**
     * Cherche le controller du reseau via BFS et retire tous les blocs
     * possedes par ce relay du registre central.
     */
    private void findControllerAndUnregister() {
        if (level == null) return;

        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        visited.add(worldPosition);
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>(getConnectedNodes());

        while (!queue.isEmpty()) {
            BlockPos nodePos = queue.poll();
            if (!visited.add(nodePos)) continue;
            if (!level.hasChunkAt(nodePos)) continue;

            BlockEntity be = level.getBlockEntity(nodePos);
            if (be instanceof StorageControllerBlockEntity controller) {
                controller.getNetworkRegistry().unregisterAllByOwner(worldPosition);
                // [FIX] Pas de controller.setChanged() ici: pendant le world unload,
                // re-dirtier le chunk du controller cause une boucle infinie dans saveAllChunks.
                // StorageEvents gere le cleanup lors du block break en gameplay normal.
                return;
            }
            if (be instanceof INetworkNode node) {
                for (BlockPos neighbor : node.getConnectedNodes()) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }
}
