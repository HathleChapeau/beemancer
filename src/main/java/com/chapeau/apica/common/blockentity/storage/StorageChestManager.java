/**
 * ============================================================
 * [StorageChestManager.java]
 * Description: Gestion des coffres enregistrés du réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerBlockEntity  | Parent BlockEntity   | Back-reference pour level/pos  |
 * | StorageHelper                 | Vérification coffres | isStorageContainer             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (délégation)
 * - StorageItemAggregator.java (getRegisteredChests)
 * - StorageDeliveryManager.java (getRegisteredChests)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Gère l'enregistrement des coffres du réseau de stockage.
 * Supporte le flood fill BFS pour enregistrer les coffres adjacents.
 * Fonctionne avec tout INetworkNode (controller ou relay).
 */
public class StorageChestManager {
    private static final int MAX_FLOOD_FILL_PER_CLICK = 64;

    private final INetworkNode parent;
    private final Set<BlockPos> registeredChests = new HashSet<>();

    public StorageChestManager(INetworkNode parent) {
        this.parent = parent;
    }

    /**
     * Tente d'enregistrer ou de retirer un coffre.
     * Si le coffre est déjà enregistré, le retire (+ l'autre moitie si double chest).
     * Sinon, enregistre le coffre et ses adjacents (flood fill).
     * Pour les doubles chests, seule la position canonique (LEFT) est enregistree.
     *
     * @return true si l'opération a réussi
     */
    public boolean toggleChest(BlockPos chestPos) {
        Level level = parent.getNodeLevel();
        if (level == null) return false;

        if (!isInRange(chestPos)) return false;
        if (!isChest(chestPos)) return false;

        // Utiliser la position canonique pour les doubles chests
        BlockPos canonical = StorageHelper.getCanonicalChestPos(level, chestPos);

        if (registeredChests.contains(canonical)) {
            registeredChests.remove(canonical);
            parent.markDirty();
            parent.syncNodeToClient();
            return true;
        } else {
            registerChestWithNeighbors(chestPos);
            return true;
        }
    }

    /**
     * Flood fill pour enregistrer un coffre et tous ses adjacents.
     * Pour les doubles chests, seule la position canonique (LEFT) est enregistree.
     * Le BFS explore les DEUX moities pour decouvrir tous les voisins adjacents.
     * Un Set de canoniques deja enregistrees empeche les doublons sans bloquer l'exploration.
     */
    private void registerChestWithNeighbors(BlockPos startPos) {
        Level level = parent.getNodeLevel();
        if (level == null) return;

        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> checked = new HashSet<>();
        Set<BlockPos> registeredCanonicals = new HashSet<>();
        toCheck.add(startPos);
        int newlyRegistered = 0;

        while (!toCheck.isEmpty()) {
            if (newlyRegistered >= MAX_FLOOD_FILL_PER_CLICK) break;

            BlockPos current = toCheck.poll();

            if (checked.contains(current)) continue;
            checked.add(current);

            if (!isChest(current)) continue;
            if (!isInRange(current)) continue;

            // Position canonique: pour un double chest, toujours LEFT
            BlockPos canonical = StorageHelper.getCanonicalChestPos(level, current);
            if (!registeredChests.contains(canonical) && !registeredCanonicals.contains(canonical)) {
                registeredChests.add(canonical);
                registeredCanonicals.add(canonical);
                newlyRegistered++;
            }

            // NE PAS marquer l'autre moitie comme "checked" — on doit explorer ses voisins
            // Le dedup se fait via registeredCanonicals, pas via checked

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!checked.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        parent.markDirty();
        parent.syncNodeToClient();
    }

    /**
     * Vérifie si une position est un conteneur de stockage valide.
     * Utilise la capability IItemHandler (NeoForge) en priorite,
     * avec fallback sur la whitelist legacy (Chest, Barrel).
     */
    public boolean isChest(BlockPos pos) {
        if (parent.getNodeLevel() == null) return false;
        if (!parent.getNodeLevel().hasChunkAt(pos)) return false;
        if (StorageHelper.hasItemHandlerCapability(parent.getNodeLevel(), pos, null)) {
            return true;
        }
        return StorageHelper.isStorageContainer(parent.getNodeLevel().getBlockState(pos));
    }

    /**
     * Vérifie si une position est dans le rayon d'action.
     */
    public boolean isInRange(BlockPos pos) {
        double distance = Math.sqrt(parent.getNodePos().distSqr(pos));
        return distance <= parent.getRange();
    }

    public Set<BlockPos> getRegisteredChests() {
        return Collections.unmodifiableSet(registeredChests);
    }

    public int getRegisteredChestCount() {
        return registeredChests.size();
    }

    /**
     * Retire un coffre du registre (utilise par StorageEvents quand un coffre est casse).
     * Essaie aussi la position canonique (au cas ou le coffre etait un double).
     *
     * @return true si le coffre existait
     */
    public boolean removeChest(BlockPos pos) {
        boolean removed = registeredChests.remove(pos);
        // Si le coffre n'etait pas enregistre directement, verifier la position canonique
        if (!removed && parent.getNodeLevel() != null) {
            BlockPos canonical = StorageHelper.getCanonicalChestPos(parent.getNodeLevel(), pos);
            removed = registeredChests.remove(canonical);
        }
        if (removed) {
            parent.markDirty();
        }
        return removed;
    }

    // === NBT ===

    public void save(CompoundTag tag) {
        ListTag chestsTag = new ListTag();
        for (BlockPos pos : registeredChests) {
            CompoundTag posTag = new CompoundTag();
            posTag.put("Pos", NbtUtils.writeBlockPos(pos));
            chestsTag.add(posTag);
        }
        tag.put("RegisteredChests", chestsTag);
    }

    public void load(CompoundTag tag) {
        registeredChests.clear();
        ListTag chestsTag = tag.getList("RegisteredChests", Tag.TAG_COMPOUND);
        for (int i = 0; i < chestsTag.size(); i++) {
            NbtUtils.readBlockPos(chestsTag.getCompound(i), "Pos").ifPresent(registeredChests::add);
        }
    }
}
