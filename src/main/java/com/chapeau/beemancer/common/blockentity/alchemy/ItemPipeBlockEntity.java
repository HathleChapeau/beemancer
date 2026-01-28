/**
 * ============================================================
 * [ItemPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport d'items
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Chaque item a un ID unique, quantité, position précédente, timestamp
 * - Les pipes enregistrent les 8 derniers IDs pour éviter les boucles
 * - Round-robin par direction (pas par item) pour distribution équitable
 * - Si aucune destination: attend 1 sec puis drop
 * - Capacités: T1=4, T2=8, T3=16, T4=32 stacks
 * - Throughput: nombre de STACKS par tick (pas items)
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.ItemPipeBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.*;

public class ItemPipeBlockEntity extends BlockEntity {
    // --- TIER CONFIG ---
    public static final int TIER1_SLOTS = 4;
    public static final int TIER1_THROUGHPUT = 4; // stacks per tick

    public static final int TIER2_SLOTS = 8;
    public static final int TIER2_THROUGHPUT = 8;

    public static final int TIER3_SLOTS = 16;
    public static final int TIER3_THROUGHPUT = 16;

    public static final int TIER4_SLOTS = 32;
    public static final int TIER4_THROUGHPUT = 32;

    private static final int MAX_REMEMBERED_IDS = 8;
    private static final int STUCK_TIMEOUT_TICKS = 20; // 1 seconde avant drop

    private final int maxSlots;
    private final int throughput;

    // Items en transit dans cette pipe
    private final List<PipeItem> items = new ArrayList<>();

    // 8 derniers IDs reçus pour éviter les boucles
    private final Deque<UUID> rememberedIds = new ArrayDeque<>(MAX_REMEMBERED_IDS);

    // Round-robin par direction (0-5 pour les 6 directions)
    private int directionIndex = 0;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        this(BeemancerBlockEntities.ITEM_PIPE.get(), pos, state, TIER1_SLOTS, TIER1_THROUGHPUT);
    }

    public ItemPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               int maxSlots, int throughput) {
        super(type, pos, state);
        this.maxSlots = maxSlots;
        this.throughput = throughput;
    }

    // Factory methods for tiered versions
    public static ItemPipeBlockEntity createTier2(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(BeemancerBlockEntities.ITEM_PIPE_TIER2.get(), pos, state,
            TIER2_SLOTS, TIER2_THROUGHPUT);
    }

    public static ItemPipeBlockEntity createTier3(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(BeemancerBlockEntities.ITEM_PIPE_TIER3.get(), pos, state,
            TIER3_SLOTS, TIER3_THROUGHPUT);
    }

    public static ItemPipeBlockEntity createTier4(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(BeemancerBlockEntities.ITEM_PIPE_TIER4.get(), pos, state,
            TIER4_SLOTS, TIER4_THROUGHPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        // Extraire des blocs en mode extraction
        be.processExtractions(level, pos, state);

        // Traiter les items en transit
        be.processItems(level, pos, state);
    }

    private void processExtractions(Level level, BlockPos pos, BlockState state) {
        if (isFull()) {
            return;
        }

        int extracted = 0;
        for (Direction dir : Direction.values()) {
            if (extracted >= throughput) break;
            if (!ItemPipeBlock.isConnected(state, dir)) continue;
            if (!ItemPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);

            // Ne pas extraire d'autres pipes
            if (level.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity) continue;

            var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (cap != null) {
                int stacksExtracted = extractFromHandler(level, cap);
                extracted += stacksExtracted;
            }
        }
    }

    private int extractFromHandler(Level level, IItemHandler handler) {
        int stacksExtracted = 0;

        for (int i = 0; i < handler.getSlots() && !isFull(); i++) {
            ItemStack extracted = handler.extractItem(i, 64, true);

            if (!extracted.isEmpty()) {
                ItemStack actualExtracted = handler.extractItem(i, extracted.getCount(), false);
                if (!actualExtracted.isEmpty()) {
                    // Créer un nouvel item avec ID unique
                    PipeItem pipeItem = new PipeItem(
                        UUID.randomUUID(),
                        actualExtracted.copy(),
                        null, // Pas de pipe précédente (extraction externe)
                        level.getGameTime()
                    );
                    items.add(pipeItem);
                    stacksExtracted++;
                    setChanged();
                }
            }
        }

        return stacksExtracted;
    }

    private void processItems(Level level, BlockPos pos, BlockState state) {
        if (items.isEmpty()) {
            return;
        }

        long currentTime = level.getGameTime();
        List<PipeItem> toRemove = new ArrayList<>();
        int stacksProcessed = 0;

        // Copier la liste pour éviter ConcurrentModificationException
        List<PipeItem> itemsCopy = new ArrayList<>(items);

        for (PipeItem item : itemsCopy) {
            if (stacksProcessed >= throughput) break;

            // Trouver une destination valide avec round-robin par direction
            PipeDestination dest = findNextValidDestination(level, pos, state, item);

            if (dest != null) {
                // Transférer l'item
                TransferResult result = transferItem(level, pos, dest, item);

                if (result == TransferResult.FULL_TRANSFER) {
                    toRemove.add(item);
                    stacksProcessed++;
                } else if (result == TransferResult.PARTIAL_TRANSFER) {
                    // L'item a été partiellement inséré, on garde le reste
                    // Ne pas incrémenter stacksProcessed car on n'a pas fini
                    stacksProcessed++;
                }
                // FAILED = rien n'a été transféré, on essaiera au prochain tick
            } else {
                // Pas de destination disponible
                long waitTime = currentTime - item.arrivalTime;

                if (waitTime >= STUCK_TIMEOUT_TICKS) {
                    // Drop l'item sur le sol
                    dropItem(level, pos, item.stack);
                    toRemove.add(item);
                }
                // Sinon: attendre la prochaine frame
            }
        }

        // Retirer les items transférés/droppés
        for (PipeItem item : toRemove) {
            items.remove(item);
        }

        if (!toRemove.isEmpty()) {
            setChanged();
        }
    }

    /**
     * Trouve la prochaine destination valide en utilisant le round-robin par direction.
     */
    private PipeDestination findNextValidDestination(Level level, BlockPos pos, BlockState state, PipeItem item) {
        Direction[] directions = Direction.values();
        int startIndex = directionIndex;

        // Essayer chaque direction en commençant par l'index actuel
        for (int i = 0; i < 6; i++) {
            int idx = (startIndex + i) % 6;
            Direction dir = directions[idx];

            if (!ItemPipeBlock.isConnected(state, dir)) continue;
            if (ItemPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);

            // Ignorer la pipe précédente
            if (neighborPos.equals(item.previousPos)) continue;

            BlockEntity neighborBe = level.getBlockEntity(neighborPos);

            if (neighborBe instanceof ItemPipeBlockEntity neighborPipe) {
                // Ignorer les pipes pleines
                if (neighborPipe.isFull()) continue;

                // Ignorer les pipes qui ont déjà vu cet ID
                if (neighborPipe.hasSeenId(item.id)) continue;

                // Avancer le round-robin pour la prochaine fois
                directionIndex = (idx + 1) % 6;
                return new PipeDestination(dir, neighborPos, true);
            } else {
                // Conteneur d'items (coffre, etc.)
                var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
                if (cap != null && canInsertIntoHandler(cap, item.stack)) {
                    // Avancer le round-robin
                    directionIndex = (idx + 1) % 6;
                    return new PipeDestination(dir, neighborPos, false);
                }
            }
        }

        return null;
    }

    private enum TransferResult {
        FULL_TRANSFER,    // Item entièrement transféré
        PARTIAL_TRANSFER, // Item partiellement transféré (reste dans la pipe)
        FAILED            // Rien n'a été transféré
    }

    private TransferResult transferItem(Level level, BlockPos myPos, PipeDestination dest, PipeItem item) {
        if (dest.isPipe) {
            BlockEntity neighborBe = level.getBlockEntity(dest.pos);
            if (neighborBe instanceof ItemPipeBlockEntity neighborPipe) {
                // Garder l'arrivalTime original pour le timeout global
                boolean received = neighborPipe.receiveItem(item.id, item.stack, myPos, item.arrivalTime);
                return received ? TransferResult.FULL_TRANSFER : TransferResult.FAILED;
            }
        } else {
            var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, dest.pos, dest.direction.getOpposite());
            if (cap != null) {
                ItemStack remaining = insertIntoHandler(cap, item.stack);
                if (remaining.isEmpty()) {
                    return TransferResult.FULL_TRANSFER;
                } else if (remaining.getCount() < item.stack.getCount()) {
                    // Partiellement inséré - mettre à jour le stack restant
                    item.stack = remaining;
                    // Réinitialiser le timeout car on a fait du progrès
                    item.arrivalTime = level.getGameTime();
                    return TransferResult.PARTIAL_TRANSFER;
                }
            }
        }
        return TransferResult.FAILED;
    }

    /**
     * Reçoit un item d'une autre pipe.
     * Garde l'arrivalTime original pour le timeout global du réseau.
     */
    public boolean receiveItem(UUID id, ItemStack stack, BlockPos fromPos, long originalArrivalTime) {
        if (isFull()) {
            return false;
        }

        // Enregistrer l'ID
        rememberIdOnly(id);

        // Ajouter l'item avec l'arrivalTime original
        PipeItem pipeItem = new PipeItem(id, stack.copy(), fromPos, originalArrivalTime);
        items.add(pipeItem);
        setChanged();

        return true;
    }

    /**
     * Enregistre un ID (FIFO, max 8).
     */
    private void rememberIdOnly(UUID id) {
        // Éviter les doublons
        if (rememberedIds.contains(id)) {
            return;
        }

        if (rememberedIds.size() >= MAX_REMEMBERED_IDS) {
            rememberedIds.pollFirst();
        }
        rememberedIds.addLast(id);
    }

    /**
     * Vérifie si un ID a déjà été vu par cette pipe.
     */
    public boolean hasSeenId(UUID id) {
        return rememberedIds.contains(id);
    }

    private boolean canInsertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack testStack = stack.copyWithCount(1);
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack remaining = handler.insertItem(i, testStack, true);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }
        return remaining;
    }

    private void dropItem(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty() && !level.isClientSide()) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            ItemEntity entity = new ItemEntity(level, x, y, z, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

    public boolean isFull() {
        return items.size() >= maxSlots;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getItemCount() {
        return items.size();
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public List<PipeItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Sauvegarder les items
        ListTag itemsTag = new ListTag();
        for (PipeItem item : items) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putUUID("Id", item.id);
            itemTag.put("Stack", item.stack.save(registries));
            if (item.previousPos != null) {
                itemTag.putInt("PrevX", item.previousPos.getX());
                itemTag.putInt("PrevY", item.previousPos.getY());
                itemTag.putInt("PrevZ", item.previousPos.getZ());
            }
            itemTag.putLong("Arrival", item.arrivalTime);
            itemsTag.add(itemTag);
        }
        tag.put("Items", itemsTag);

        // Sauvegarder les IDs mémorisés
        ListTag idsTag = new ListTag();
        for (UUID id : rememberedIds) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("Id", id);
            idsTag.add(idTag);
        }
        tag.put("RememberedIds", idsTag);

        tag.putInt("DirIndex", directionIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Charger les items
        items.clear();
        if (tag.contains("Items", Tag.TAG_LIST)) {
            ListTag itemsTag = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < itemsTag.size(); i++) {
                CompoundTag itemTag = itemsTag.getCompound(i);
                UUID id = itemTag.getUUID("Id");
                ItemStack stack = ItemStack.parse(registries, itemTag.getCompound("Stack")).orElse(ItemStack.EMPTY);
                BlockPos prevPos = null;
                if (itemTag.contains("PrevX")) {
                    prevPos = new BlockPos(
                        itemTag.getInt("PrevX"),
                        itemTag.getInt("PrevY"),
                        itemTag.getInt("PrevZ")
                    );
                }
                long arrival = itemTag.getLong("Arrival");

                if (!stack.isEmpty()) {
                    items.add(new PipeItem(id, stack, prevPos, arrival));
                }
            }
        }

        // Charger les IDs mémorisés
        rememberedIds.clear();
        if (tag.contains("RememberedIds", Tag.TAG_LIST)) {
            ListTag idsTag = tag.getList("RememberedIds", Tag.TAG_COMPOUND);
            for (int i = 0; i < idsTag.size(); i++) {
                CompoundTag idTag = idsTag.getCompound(i);
                rememberedIds.addLast(idTag.getUUID("Id"));
            }
        }

        directionIndex = tag.getInt("DirIndex");
    }

    /**
     * Représente un item en transit dans la pipe.
     */
    public static class PipeItem {
        public final UUID id;
        public ItemStack stack;
        public final BlockPos previousPos;
        public long arrivalTime; // Mutable pour reset sur insertion partielle

        public PipeItem(UUID id, ItemStack stack, BlockPos previousPos, long arrivalTime) {
            this.id = id;
            this.stack = stack;
            this.previousPos = previousPos;
            this.arrivalTime = arrivalTime;
        }
    }

    private record PipeDestination(Direction direction, BlockPos pos, boolean isPipe) {}
}
