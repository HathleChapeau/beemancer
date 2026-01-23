/**
 * ============================================================
 * [HoneyPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport de fluide
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.HoneyPipeBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class HoneyPipeBlockEntity extends BlockEntity {
    private static final int BUFFER_CAPACITY = 500; // Small buffer
    private static final int TRANSFER_RATE = 50; // mB per tick (slow passive transfer)

    private final FluidTank buffer = new FluidTank(BUFFER_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get()
                || stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get()
                || stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int transferCooldown = 0;

    public HoneyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.HONEY_PIPE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoneyPipeBlockEntity be) {
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }

        // Try to pull from connected sources
        be.pullFromNeighbors(level, pos, state);

        // Try to push to connected destinations
        be.pushToNeighbors(level, pos, state);

        be.transferCooldown = 2; // Every 2 ticks
    }

    private void pullFromNeighbors(Level level, BlockPos pos, BlockState state) {
        if (buffer.getFluidAmount() >= buffer.getCapacity()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            if (!isConnected(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            
            if (cap != null && !(level.getBlockEntity(neighborPos) instanceof HoneyPipeBlockEntity)) {
                // Only pull from non-pipe sources
                FluidStack toDrain = cap.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE);
                if (!toDrain.isEmpty() && buffer.isFluidValid(toDrain)) {
                    int filled = buffer.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
                    if (filled > 0) {
                        FluidStack drained = cap.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        buffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    private void pushToNeighbors(Level level, BlockPos pos, BlockState state) {
        if (buffer.isEmpty()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            if (!isConnected(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            
            if (cap != null) {
                FluidStack toTransfer = buffer.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE);
                if (!toTransfer.isEmpty()) {
                    int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        buffer.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }

            if (buffer.isEmpty()) break;
        }
    }

    private boolean isConnected(BlockState state, Direction dir) {
        return switch (dir) {
            case NORTH -> state.getValue(HoneyPipeBlock.NORTH);
            case SOUTH -> state.getValue(HoneyPipeBlock.SOUTH);
            case EAST -> state.getValue(HoneyPipeBlock.EAST);
            case WEST -> state.getValue(HoneyPipeBlock.WEST);
            case UP -> state.getValue(HoneyPipeBlock.UP);
            case DOWN -> state.getValue(HoneyPipeBlock.DOWN);
        };
    }

    public FluidTank getBuffer() {
        return buffer;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Buffer", buffer.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Buffer")) {
            buffer.readFromNBT(registries, tag.getCompound("Buffer"));
        }
    }
}
