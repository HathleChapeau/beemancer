/**
 * ============================================================
 * [ItemPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport d'items
 * ============================================================
 * 
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire les items du bloc connecté
 * - Mode normal: pousse les items vers les blocs connectés
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.ItemPipeBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ItemPipeBlockEntity extends BlockEntity {
    private static final int BUFFER_SIZE = 4;
    private static final int TRANSFER_AMOUNT = 4;

    private final ItemStackHandler buffer = new ItemStackHandler(BUFFER_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int transferCooldown = 0;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ITEM_PIPE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
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
            }
        }
    }

    private void extractFromHandler(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots() && !isBufferFull(); i++) {
            ItemStack extracted = handler.extractItem(i, TRANSFER_AMOUNT, true);
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

        for (Direction dir : Direction.values()) {
            if (!ItemPipeBlock.isConnected(state, dir)) continue;
            if (ItemPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            var cap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            
            if (cap != null) {
                pushToHandler(cap);
            }

            if (isBufferEmpty()) break;
        }
    }

    private void pushToHandler(IItemHandler handler) {
        for (int bufferSlot = 0; bufferSlot < buffer.getSlots(); bufferSlot++) {
            ItemStack stack = buffer.getStackInSlot(bufferSlot);
            if (stack.isEmpty()) continue;

            ItemStack toInsert = stack.copyWithCount(Math.min(stack.getCount(), TRANSFER_AMOUNT));
            ItemStack remaining = insertIntoHandler(handler, toInsert);
            
            int inserted = toInsert.getCount() - remaining.getCount();
            if (inserted > 0) {
                buffer.extractItem(bufferSlot, inserted, false);
            }
        }
    }

    private ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }
        return remaining;
    }

    private ItemStack insertIntoBuffer(ItemStack stack, boolean simulate) {
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        }
    }
}
