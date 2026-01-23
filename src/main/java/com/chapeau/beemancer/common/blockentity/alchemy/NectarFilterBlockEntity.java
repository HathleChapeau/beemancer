/**
 * ============================================================
 * [NectarFilterBlockEntity.java]
 * Description: Filtre le Royal Jelly en Royal Jelly purifié
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

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

public class NectarFilterBlockEntity extends BlockEntity {
    private static final int FILTER_RATE = 5; // mB per tick
    private static final int CAPACITY = 4000;

    private final FluidTank inputTank = new FluidTank(CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank outputTank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    public NectarFilterBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.NECTAR_FILTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NectarFilterBlockEntity be) {
        // Filter: Royal Jelly input -> Royal Jelly output (purified, same fluid for now)
        if (be.inputTank.getFluidAmount() >= FILTER_RATE) {
            FluidStack drained = be.inputTank.drain(FILTER_RATE, IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty()) {
                int filled = be.outputTank.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                if (filled > 0) {
                    be.inputTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    be.outputTank.fill(new FluidStack(drained.getFluid(), filled), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        // Auto-output to below
        if (be.outputTank.getFluidAmount() > 0) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (cap != null) {
                FluidStack toTransfer = be.outputTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    be.outputTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    public FluidTank getInputTank() { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Output", outputTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputTank.readFromNBT(registries, tag.getCompound("Input"));
        outputTank.readFromNBT(registries, tag.getCompound("Output"));
    }
}
