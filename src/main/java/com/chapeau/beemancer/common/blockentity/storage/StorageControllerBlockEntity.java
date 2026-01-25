/**
 * ============================================================
 * [StorageControllerBlockEntity.java]
 * Description: BlockEntity pour le Storage Controller - gère le réseau de coffres
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | BeemancerBlockEntities   | Type du BlockEntity    | Constructeur                   |
 * | Level                    | Accès monde            | Flood fill, vérification coffres|
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlock.java (création et interaction)
 * - StorageTerminalBlockEntity.java (récupération items agrégés)
 * - StorageControllerRenderer.java (rendu mode édition)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.StorageEditModeHandler;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Unité centrale du réseau de stockage.
 *
 * Fonctionnalités:
 * - Mode édition: shift+clic droit pour toggle
 * - Enregistrement coffres: clic droit en mode édition (flood fill)
 * - Agrégation items: liste tous items de tous coffres enregistrés
 * - Synchronisation: temps réel si GUI ouverte, périodique sinon
 */
public class StorageControllerBlockEntity extends BlockEntity {

    private static final int MAX_RANGE = 24;
    private static final int SYNC_INTERVAL = 40; // ticks (2 secondes)

    // Coffres enregistrés
    private final Set<BlockPos> registeredChests = new HashSet<>();

    // Mode édition
    private boolean editMode = false;
    private UUID editingPlayer = null;

    // Terminaux liés
    private final Set<BlockPos> linkedTerminals = new HashSet<>();

    // Cache items agrégés
    private List<ItemStack> aggregatedItems = new ArrayList<>();
    private int syncTimer = 0;
    private boolean needsSync = true;

    // Joueurs avec GUI ouverte
    private final Set<UUID> playersViewing = new HashSet<>();

    public StorageControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_CONTROLLER.get(), pos, blockState);
    }

    // === Mode Édition ===

    /**
     * Toggle le mode édition pour un joueur.
     */
    public boolean toggleEditMode(UUID playerId) {
        if (editMode && editingPlayer != null && editingPlayer.equals(playerId)) {
            // Désactiver
            editMode = false;
            editingPlayer = null;
            setChanged();
            syncToClient();
            return false;
        } else if (!editMode) {
            // Activer
            editMode = true;
            editingPlayer = playerId;
            setChanged();
            syncToClient();
            return true;
        }
        return false;
    }

    /**
     * Force la sortie du mode édition.
     */
    public void exitEditMode() {
        if (editMode) {
            if (editingPlayer != null) {
                StorageEditModeHandler.stopEditing(editingPlayer);
            }
            editMode = false;
            editingPlayer = null;
            setChanged();
            syncToClient();
        }
    }

    /**
     * Vérifie si un joueur peut éditer.
     */
    public boolean canEdit(UUID playerId) {
        return editMode && editingPlayer != null && editingPlayer.equals(playerId);
    }

    public boolean isEditMode() {
        return editMode;
    }

    @Nullable
    public UUID getEditingPlayer() {
        return editingPlayer;
    }

    // === Gestion des Coffres ===

    /**
     * Tente d'enregistrer ou de retirer un coffre.
     * Si le coffre est déjà enregistré, le retire.
     * Sinon, enregistre le coffre et ses adjacents (flood fill).
     *
     * @return true si l'opération a réussi
     */
    public boolean toggleChest(BlockPos chestPos) {
        if (level == null) return false;

        // Vérifier la distance
        if (!isInRange(chestPos)) return false;

        // Vérifier si c'est un coffre
        if (!isChest(chestPos)) return false;

        if (registeredChests.contains(chestPos)) {
            // Retirer uniquement ce coffre
            registeredChests.remove(chestPos);
            setChanged();
            syncToClient();
            needsSync = true;
            return true;
        } else {
            // Flood fill pour enregistrer tous les coffres adjacents
            registerChestWithNeighbors(chestPos);
            needsSync = true;
            return true;
        }
    }

    /**
     * Flood fill pour enregistrer un coffre et tous ses adjacents.
     */
    private void registerChestWithNeighbors(BlockPos startPos) {
        if (level == null) return;

        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> checked = new HashSet<>();
        toCheck.add(startPos);

        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.poll();

            if (checked.contains(current)) continue;
            checked.add(current);

            // Vérifications
            if (!isChest(current)) continue;
            if (registeredChests.contains(current)) continue;
            if (!isInRange(current)) continue;

            // Enregistrer ce coffre
            registeredChests.add(current);

            // Ajouter les 6 voisins
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!checked.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        setChanged();
        syncToClient();
    }

    /**
     * Vérifie si une position est un coffre.
     */
    private boolean isChest(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock ||
               state.is(Blocks.CHEST) ||
               state.is(Blocks.TRAPPED_CHEST) ||
               state.is(Blocks.BARREL);
    }

    /**
     * Vérifie si une position est dans le rayon d'action.
     */
    private boolean isInRange(BlockPos pos) {
        double distance = Math.sqrt(worldPosition.distSqr(pos));
        return distance <= MAX_RANGE;
    }

    /**
     * Retourne les coffres enregistrés.
     */
    public Set<BlockPos> getRegisteredChests() {
        return Collections.unmodifiableSet(registeredChests);
    }

    // === Gestion des Terminaux ===

    /**
     * Lie un terminal à ce controller.
     */
    public void linkTerminal(BlockPos terminalPos) {
        linkedTerminals.add(terminalPos);
        setChanged();
    }

    /**
     * Retire un terminal.
     */
    public void unlinkTerminal(BlockPos terminalPos) {
        linkedTerminals.remove(terminalPos);
        setChanged();
    }

    public Set<BlockPos> getLinkedTerminals() {
        return Collections.unmodifiableSet(linkedTerminals);
    }

    // === Agrégation Items ===

    /**
     * Retourne la liste agrégée de tous les items.
     * Les items identiques sont fusionnés avec leur quantité totale.
     */
    public List<ItemStack> getAggregatedItems() {
        return aggregatedItems;
    }

    /**
     * Force le recalcul des items agrégés.
     */
    public void refreshAggregatedItems() {
        if (level == null) return;

        Map<ItemStackKey, Integer> itemCounts = new HashMap<>();

        // Nettoyer les coffres invalides
        registeredChests.removeIf(pos -> !isChest(pos));

        // Parcourir tous les coffres
        for (BlockPos chestPos : registeredChests) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        ItemStackKey key = new ItemStackKey(stack);
                        itemCounts.merge(key, stack.getCount(), Integer::sum);
                    }
                }
            }
        }

        // Convertir en liste
        aggregatedItems = new ArrayList<>();
        for (Map.Entry<ItemStackKey, Integer> entry : itemCounts.entrySet()) {
            ItemStack stack = entry.getKey().toStack();
            stack.setCount(entry.getValue());
            aggregatedItems.add(stack);
        }

        // Trier par nom
        aggregatedItems.sort(Comparator.comparing(
            stack -> stack.getHoverName().getString()
        ));
    }

    /**
     * Trouve un slot pour déposer un item.
     *
     * @return la position du coffre où déposer, ou null si aucun espace
     */
    @Nullable
    public BlockPos findSlotForItem(ItemStack stack) {
        if (level == null) return null;

        // D'abord chercher un coffre avec le même item
        for (BlockPos chestPos : registeredChests) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, stack) &&
                        existing.getCount() < existing.getMaxStackSize()) {
                        return chestPos;
                    }
                }
            }
        }

        // Sinon chercher un slot vide
        for (BlockPos chestPos : registeredChests) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).isEmpty()) {
                        return chestPos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Dépose un item dans le réseau de stockage.
     *
     * @return le reste non déposé (vide si tout a été déposé)
     */
    public ItemStack depositItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return stack;

        ItemStack remaining = stack.copy();

        // D'abord essayer de fusionner avec des stacks existants
        for (BlockPos chestPos : registeredChests) {
            if (remaining.isEmpty()) break;

            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                        int space = existing.getMaxStackSize() - existing.getCount();
                        int toTransfer = Math.min(space, remaining.getCount());
                        if (toTransfer > 0) {
                            existing.grow(toTransfer);
                            remaining.shrink(toTransfer);
                            container.setChanged();
                        }
                    }
                    if (remaining.isEmpty()) break;
                }
            }
        }

        // Ensuite essayer les slots vides
        for (BlockPos chestPos : registeredChests) {
            if (remaining.isEmpty()) break;

            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).isEmpty()) {
                        int toTransfer = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                        ItemStack toPlace = remaining.copy();
                        toPlace.setCount(toTransfer);
                        container.setItem(i, toPlace);
                        remaining.shrink(toTransfer);
                        container.setChanged();
                    }
                    if (remaining.isEmpty()) break;
                }
            }
        }

        needsSync = true;
        return remaining;
    }

    /**
     * Extrait un item du réseau de stockage.
     *
     * @return l'item extrait (peut être moins que demandé)
     */
    public ItemStack extractItem(ItemStack template, int count) {
        if (level == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;

        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (BlockPos chestPos : registeredChests) {
            if (needed <= 0) break;

            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, template)) {
                        int toTake = Math.min(needed, existing.getCount());
                        existing.shrink(toTake);
                        result.grow(toTake);
                        needed -= toTake;
                        container.setChanged();

                        if (existing.isEmpty()) {
                            container.setItem(i, ItemStack.EMPTY);
                        }
                    }
                    if (needed <= 0) break;
                }
            }
        }

        needsSync = true;
        return result;
    }

    // === Tick ===

    public static void serverTick(StorageControllerBlockEntity be) {
        be.syncTimer++;

        // Sync périodique ou si des joueurs regardent
        boolean hasViewers = !be.playersViewing.isEmpty();
        boolean shouldSync = hasViewers || (be.syncTimer >= SYNC_INTERVAL && be.needsSync);

        if (shouldSync) {
            be.refreshAggregatedItems();
            be.syncTimer = 0;
            be.needsSync = false;
        }

        // Vérifier la distance du joueur en mode édition
        if (be.editMode && be.editingPlayer != null && be.level != null) {
            var player = be.level.getServer().getPlayerList().getPlayer(be.editingPlayer);
            if (player == null || player.distanceToSqr(
                    be.worldPosition.getX() + 0.5,
                    be.worldPosition.getY() + 0.5,
                    be.worldPosition.getZ() + 0.5) > MAX_RANGE * MAX_RANGE) {
                be.exitEditMode();
            }
        }
    }

    public void addViewer(UUID playerId) {
        playersViewing.add(playerId);
    }

    public void removeViewer(UUID playerId) {
        playersViewing.remove(playerId);
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Coffres enregistrés
        ListTag chestsTag = new ListTag();
        for (BlockPos pos : registeredChests) {
            chestsTag.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("RegisteredChests", chestsTag);

        // Terminaux liés
        ListTag terminalsTag = new ListTag();
        for (BlockPos pos : linkedTerminals) {
            terminalsTag.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("LinkedTerminals", terminalsTag);

        // Mode édition
        tag.putBoolean("EditMode", editMode);
        if (editingPlayer != null) {
            tag.putUUID("EditingPlayer", editingPlayer);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Coffres enregistrés
        registeredChests.clear();
        ListTag chestsTag = tag.getList("RegisteredChests", Tag.TAG_COMPOUND);
        for (int i = 0; i < chestsTag.size(); i++) {
            NbtUtils.readBlockPos(chestsTag.getCompound(i), "").ifPresent(registeredChests::add);
        }

        // Terminaux liés
        linkedTerminals.clear();
        ListTag terminalsTag = tag.getList("LinkedTerminals", Tag.TAG_COMPOUND);
        for (int i = 0; i < terminalsTag.size(); i++) {
            NbtUtils.readBlockPos(terminalsTag.getCompound(i), "").ifPresent(linkedTerminals::add);
        }

        // Mode édition
        editMode = tag.getBoolean("EditMode");
        if (tag.hasUUID("EditingPlayer")) {
            editingPlayer = tag.getUUID("EditingPlayer");
        } else {
            editingPlayer = null;
        }
    }

    // === Sync Client ===

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === Utilitaires ===

    /**
     * Clé pour identifier des ItemStacks identiques (même item, mêmes composants).
     */
    private static final class ItemStackKey {
        private final ItemStack template;

        ItemStackKey(ItemStack stack) {
            this.template = stack.copyWithCount(1);
        }

        ItemStack toStack() {
            return template.copy();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemStackKey that = (ItemStackKey) o;
            return ItemStack.isSameItemSameComponents(template, that.template);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(template);
        }
    }
}
