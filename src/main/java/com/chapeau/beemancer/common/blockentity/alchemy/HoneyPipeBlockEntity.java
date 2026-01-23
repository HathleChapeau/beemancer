/**
 * ============================================================
 * [HoneyPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport de fluide
 * ============================================================
 * 
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire le fluide du bloc connecté
 * - Mode normal: pousse le fluide vers les blocs connectés
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
    private static final int BUFFER_CAPACITY = 500;
    private static final int TRANSFER_RATE = 100;

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

        // Process extraction from neighbors marked for extraction
        be.processExtractions(level, pos, state);

        // Push to neighbors (not in extraction mode)
        be.pushToNeighbors(level, pos, state);

        be.transferCooldown = 2;
    }

    private void processExtractions(Level level, BlockPos pos, BlockState state) {
        if (buffer.getFluidAmount() >= buffer.getCapacity()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            if (!HoneyPipeBlock.isConnected(state, dir)) continue;
            if (!HoneyPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            
            // Don't extract from other pipes
            if (level.getBlockEntity(neighborPos) instanceof HoneyPipeBlockEntity) continue;

            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            if (cap != null) {
                FluidStack toDrain = cap.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE);
                if (!toDrain.isEmpty() && buffer.isFluidValid(toDrain)) {
                    int canFill = buffer.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
                    if (canFill > 0) {
                        FluidStack drained = cap.drain(canFill, IFluidHandler.FluidAction.EXECUTE);
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
            if (!HoneyPipeBlock.isConnected(state, dir)) continue;
            // Don't push to directions in extraction mode
            if (HoneyPipeBlock.isExtracting(state, dir)) continue;

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
