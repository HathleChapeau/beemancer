/**
 * ============================================================
 * [ItemPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport d'items
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire les items du bloc connecte
 * - Mode normal: pousse les items vers les pipes connectees
 * - Round-robin entre les pipes disponibles
 * - Evite les va-et-vient en trackant l'origine des items
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.ItemPipeBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemPipeBlockEntity extends BlockEntity {
    // --- TIER CONFIG ---
    public static final int TIER1_BUFFER = 4;
    public static final int TIER1_TRANSFER = 4;

    public static final int TIER2_BUFFER = 8;
    public static final int TIER2_TRANSFER = 8;

    public static final int TIER3_BUFFER = 16;
    public static final int TIER3_TRANSFER = 16;

    public static final int TIER4_BUFFER = 32;
    public static final int TIER4_TRANSFER = 32;

    private final int transferAmount;
    private final ItemStackHandler buffer;

    private int transferCooldown = 0;
    private int roundRobinIndex = 0;

    // Position de la pipe d'ou viennent les items actuels (pour eviter les va-et-vient)
    @Nullable
    private BlockPos sourcePos = null;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        this(BeemancerBlockEntities.ITEM_PIPE.get(), pos, state, TIER1_BUFFER, TIER1_TRANSFER);
    }

    public ItemPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               int bufferSize, int transferAmount) {
        super(type, pos, state);
        this.transferAmount = transferAmount;
        this.buffer = new ItemStackHandler(bufferSize) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    // Factory methods for tiered versions
    public static ItemPipeBlockEntity createTier2(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(BeemancerBlockEntities.ITEM_PIPE_TIER2.get(), pos, state,
            TIER2_BUFFER, TIER2_TRANSFER);
    }

    public static ItemPipeBlockEntity createTier3(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(BeemancerBlockEntities.ITEM_PIPE_TIER3.get(), pos, state,
            TIER3_BUFFER, TIER3_TRANSFER);
    }

    public static ItemPipeBlockEntity createTier4(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(BeemancerBlockEntities.ITEM_PIPE_TIER4.get(), pos, state,
            TIER4_BUFFER, TIER4_TRANSFER);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }

        // Reset source tracking si buffer est vide
        if (be.isBufferEmpty()) {
            be.sourcePos = null;
        }

        // Process extractions
        be.processExtractions(level, pos, state);

        // Push to neighbors
        be.pushToNeighbors(level, pos, state);

        be.transferCooldown = 8; // Slower than fluid pipes
    }

    private void processExtractions(Level level, BlockPos pos, BlockState state) {
        if (isBufferFull()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            if (!ItemPipeBlock.isConnected(state, dir)) continue;
            if (!ItemPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);

            // Don't extract from other item pipes
            if (level.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity) continue;

            var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (cap != null) {
                extractFromHandler(cap);
                // Mark source as non-pipe (null = from machine)
                sourcePos = null;
            }
        }
    }

    private void extractFromHandler(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots() && !isBufferFull(); i++) {
            ItemStack extracted = handler.extractItem(i, transferAmount, true);
            if (!extracted.isEmpty()) {
                ItemStack remaining = insertIntoBuffer(extracted, true);
                int toExtract = extracted.getCount() - remaining.getCount();
                if (toExtract > 0) {
                    ItemStack actualExtracted = handler.extractItem(i, toExtract, false);
                    insertIntoBuffer(actualExtracted, false);
                }
            }
        }
    }

    private void pushToNeighbors(Level level, BlockPos pos, BlockState state) {
        if (isBufferEmpty()) {
            return;
        }

        // Construire la liste des destinations disponibles
        List<PipeDestination> availableDestinations = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (!ItemPipeBlock.isConnected(state, dir)) continue;
            if (ItemPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);

            // Skip si c'est la source (evite va-et-vient)
            if (neighborPos.equals(sourcePos)) continue;

            // Check si c'est une pipe
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof ItemPipeBlockEntity neighborPipe) {
                // Skip si la pipe voisine est pleine
                if (neighborPipe.isBufferFull()) continue;

                availableDestinations.add(new PipeDestination(dir, neighborPos, true));
            } else {
                // C'est un bloc normal avec capacite d'items
                var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
                if (cap != null) {
                    // Verifier si on peut inserer quelque chose
                    if (canInsertAnyIntoHandler(cap)) {
                        availableDestinations.add(new PipeDestination(dir, neighborPos, false));
                    }
                }
            }
        }

        if (availableDestinations.isEmpty()) return;

        // Round-robin: choisir la prochaine destination
        roundRobinIndex = roundRobinIndex % availableDestinations.size();
        PipeDestination dest = availableDestinations.get(roundRobinIndex);
        roundRobinIndex++;

        // Transferer vers la destination choisie
        if (dest.isPipe) {
            BlockEntity neighborBe = level.getBlockEntity(dest.pos);
            if (neighborBe instanceof ItemPipeBlockEntity neighborPipe) {
                transferToPipe(neighborPipe, pos);
            }
        } else {
            var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, dest.pos, dest.direction.getOpposite());
            if (cap != null) {
                pushToHandler(cap);
            }
        }
    }

    private void transferToPipe(ItemPipeBlockEntity neighborPipe, BlockPos myPos) {
        for (int bufferSlot = 0; bufferSlot < buffer.getSlots(); bufferSlot++) {
            ItemStack stack = buffer.getStackInSlot(bufferSlot);
            if (stack.isEmpty()) continue;

            ItemStack toInsert = stack.copyWithCount(Math.min(stack.getCount(), transferAmount));
            ItemStack remaining = neighborPipe.insertIntoBuffer(toInsert, false);

            int inserted = toInsert.getCount() - remaining.getCount();
            if (inserted > 0) {
                buffer.extractItem(bufferSlot, inserted, false);
                // Marquer notre position comme source pour la pipe voisine
                neighborPipe.sourcePos = myPos;
                break; // Un seul transfert par tick
            }
        }
    }

    private void pushToHandler(IItemHandler handler) {
        for (int bufferSlot = 0; bufferSlot < buffer.getSlots(); bufferSlot++) {
            ItemStack stack = buffer.getStackInSlot(bufferSlot);
            if (stack.isEmpty()) continue;

            ItemStack toInsert = stack.copyWithCount(Math.min(stack.getCount(), transferAmount));
            ItemStack remaining = insertIntoHandler(handler, toInsert);

            int inserted = toInsert.getCount() - remaining.getCount();
            if (inserted > 0) {
                buffer.extractItem(bufferSlot, inserted, false);
                break; // Un seul transfert par tick
            }
        }
    }

    private boolean canInsertAnyIntoHandler(IItemHandler handler) {
        for (int bufferSlot = 0; bufferSlot < buffer.getSlots(); bufferSlot++) {
            ItemStack stack = buffer.getStackInSlot(bufferSlot);
            if (stack.isEmpty()) continue;

            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack remaining = handler.insertItem(i, stack.copyWithCount(1), true);
                if (remaining.isEmpty()) return true;
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

    public ItemStack insertIntoBuffer(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
            remaining = buffer.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    private boolean isBufferFull() {
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private boolean isBufferEmpty() {
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public ItemStackHandler getBuffer() {
        return buffer;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Buffer", buffer.serializeNBT(registries));
        tag.putInt("RoundRobin", roundRobinIndex);
        if (sourcePos != null) {
            tag.put("SourcePos", NbtUtils.writeBlockPos(sourcePos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        }
        roundRobinIndex = tag.getInt("RoundRobin");
        if (tag.contains("SourcePos")) {
            sourcePos = NbtUtils.readBlockPos(tag, "SourcePos").orElse(null);
        }
    }

    private record PipeDestination(Direction direction, BlockPos pos, boolean isPipe) {}
}
