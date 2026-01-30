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
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Gère l'enregistrement des coffres du réseau de stockage.
 * Supporte le flood fill BFS pour enregistrer les coffres adjacents.
 */
public class StorageChestManager {
    private final StorageControllerBlockEntity parent;
    private final Set<BlockPos> registeredChests = new HashSet<>();

    private static final int MAX_RANGE = 24;

    public StorageChestManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    /**
     * Tente d'enregistrer ou de retirer un coffre.
     * Si le coffre est déjà enregistré, le retire.
     * Sinon, enregistre le coffre et ses adjacents (flood fill).
     *
     * @return true si l'opération a réussi
     */
    public boolean toggleChest(BlockPos chestPos) {
        if (parent.getLevel() == null) return false;

        if (!isInRange(chestPos)) return false;
        if (!isChest(chestPos)) return false;

        if (registeredChests.contains(chestPos)) {
            registeredChests.remove(chestPos);
            parent.setChanged();
            parent.syncToClient();
            return true;
        } else {
            registerChestWithNeighbors(chestPos);
            return true;
        }
    }

    /**
     * Flood fill pour enregistrer un coffre et tous ses adjacents.
     */
    private void registerChestWithNeighbors(BlockPos startPos) {
        if (parent.getLevel() == null) return;

        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> checked = new HashSet<>();
        toCheck.add(startPos);

        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.poll();

            if (checked.contains(current)) continue;
            checked.add(current);

            if (!isChest(current)) continue;
            if (registeredChests.contains(current)) continue;
            if (!isInRange(current)) continue;

            registeredChests.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!checked.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        parent.setChanged();
        parent.syncToClient();
    }

    /**
     * Vérifie si une position est un coffre.
     */
    public boolean isChest(BlockPos pos) {
        if (parent.getLevel() == null) return false;
        BlockState state = parent.getLevel().getBlockState(pos);
        return StorageHelper.isStorageContainer(state);
    }

    /**
     * Vérifie si une position est dans le rayon d'action.
     */
    public boolean isInRange(BlockPos pos) {
        double distance = Math.sqrt(parent.getBlockPos().distSqr(pos));
        return distance <= MAX_RANGE;
    }

    public Set<BlockPos> getRegisteredChests() {
        return Collections.unmodifiableSet(registeredChests);
    }

    public int getRegisteredChestCount() {
        return registeredChests.size();
    }

    /**
     * Accès mutable pour refreshAggregatedItems (nettoyage des coffres invalides).
     */
    public Set<BlockPos> getRegisteredChestsMutable() {
        return registeredChests;
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
