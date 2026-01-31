/**
 * ============================================================
 * [StorageNetworkRegistry.java]
 * Description: Registre central du reseau de stockage avec propriete exclusive
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance     | Raison                | Utilisation           |
 * |----------------|----------------------|-----------------------|
 * | BlockPos       | Position monde       | Cle du registre       |
 * | CompoundTag    | Persistance NBT      | Save/Load             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (propriete)
 * - StorageEvents.java (enregistrement)
 * - StorageControllerRenderer.java (rendu)
 * - StorageRelayRenderer.java (rendu)
 * - StorageItemAggregator.java (liste des coffres)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registre central vivant sur le controller.
 * Chaque bloc du reseau (coffre, terminal, interface) est enregistre
 * avec son noeud proprietaire (controller ou relay).
 *
 * Regle d'exclusivite: un bloc ne peut appartenir qu'a un seul noeud.
 * Seuls les relays peuvent etre connectes a plusieurs noeuds (relay-relay).
 */
public class StorageNetworkRegistry {

    public enum NetworkBlockType { CHEST, TERMINAL, INTERFACE }

    public record NetworkEntry(BlockPos ownerNode, NetworkBlockType type) {}

    private final Map<BlockPos, NetworkEntry> registry = new HashMap<>();

    /**
     * Enregistre un bloc dans le reseau avec propriete exclusive.
     * @return true si enregistre, false si deja possede par un autre noeud
     */
    public boolean registerBlock(BlockPos blockPos, BlockPos ownerNode, NetworkBlockType type) {
        NetworkEntry existing = registry.get(blockPos);
        if (existing != null && !existing.ownerNode().equals(ownerNode)) {
            return false;
        }
        registry.put(blockPos, new NetworkEntry(ownerNode, type));
        return true;
    }

    /**
     * Retire un bloc du registre.
     * @return true si le bloc existait
     */
    public boolean unregisterBlock(BlockPos blockPos) {
        return registry.remove(blockPos) != null;
    }

    /**
     * Retire tous les blocs possedes par un noeud (quand un relay est detruit).
     */
    public void unregisterAllByOwner(BlockPos ownerNode) {
        registry.entrySet().removeIf(e -> e.getValue().ownerNode().equals(ownerNode));
    }

    public boolean isRegistered(BlockPos blockPos) {
        return registry.containsKey(blockPos);
    }

    @Nullable
    public BlockPos getOwner(BlockPos blockPos) {
        NetworkEntry entry = registry.get(blockPos);
        return entry != null ? entry.ownerNode() : null;
    }

    @Nullable
    public NetworkBlockType getType(BlockPos blockPos) {
        NetworkEntry entry = registry.get(blockPos);
        return entry != null ? entry.type() : null;
    }

    public Set<BlockPos> getBlocksByOwner(BlockPos ownerNode) {
        return registry.entrySet().stream()
            .filter(e -> e.getValue().ownerNode().equals(ownerNode))
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<BlockPos> getBlocksByType(NetworkBlockType type) {
        return registry.entrySet().stream()
            .filter(e -> e.getValue().type() == type)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<BlockPos> getAllChests() { return getBlocksByType(NetworkBlockType.CHEST); }
    public Set<BlockPos> getAllTerminals() { return getBlocksByType(NetworkBlockType.TERMINAL); }
    public Set<BlockPos> getAllInterfaces() { return getBlocksByType(NetworkBlockType.INTERFACE); }

    public int getChestCount() {
        return (int) registry.values().stream()
            .filter(e -> e.type() == NetworkBlockType.CHEST).count();
    }

    public Map<BlockPos, NetworkEntry> getAll() {
        return Collections.unmodifiableMap(registry);
    }

    // === NBT ===

    public void save(CompoundTag parentTag) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, NetworkEntry> entry : registry.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.put("Pos", NbtUtils.writeBlockPos(entry.getKey()));
            tag.put("Owner", NbtUtils.writeBlockPos(entry.getValue().ownerNode()));
            tag.putString("Type", entry.getValue().type().name());
            list.add(tag);
        }
        parentTag.put("NetworkRegistry", list);
    }

    public void load(CompoundTag parentTag) {
        registry.clear();
        if (!parentTag.contains("NetworkRegistry")) return;
        ListTag list = parentTag.getList("NetworkRegistry", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Optional<BlockPos> pos = NbtUtils.readBlockPos(tag, "Pos");
            Optional<BlockPos> owner = NbtUtils.readBlockPos(tag, "Owner");
            if (pos.isPresent() && owner.isPresent()) {
                try {
                    NetworkBlockType type = NetworkBlockType.valueOf(tag.getString("Type"));
                    registry.put(pos.get(), new NetworkEntry(owner.get(), type));
                } catch (IllegalArgumentException ignored) { }
            }
        }
    }
}
