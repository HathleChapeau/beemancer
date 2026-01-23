/**
 * ============================================================
 * [InfuserBlockEntity.java]
 * Description: Infuse du Nectar avec Pollen élémentaire pour créer des Nectars spécialisés
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.InfuserBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class InfuserBlockEntity extends BlockEntity {
    private static final int INFUSE_RATE = 10; // mB per tick
    private static final int NECTAR_PER_POLLEN = 1000; // mB of elemental nectar per pollen

    private ItemStack currentPollen = ItemStack.EMPTY;
    private int infusedAmount = 0;

    private final FluidTank nectarTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank outputTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    public InfuserBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.INFUSER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfuserBlockEntity be) {
        boolean wasInfusing = state.getValue(InfuserBlock.INFUSING);
        boolean isInfusing = false;

        if (be.canInfuse()) {
            FluidStack drained = be.nectarTank.drain(INFUSE_RATE, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                Fluid outputFluid = be.getOutputFluidForPollen();
                if (outputFluid != null) {
                    be.outputTank.fill(new FluidStack(outputFluid, drained.getAmount()),
                        IFluidHandler.FluidAction.EXECUTE);
                    be.infusedAmount += drained.getAmount();
                    isInfusing = true;

                    // Consume pollen after producing enough nectar
                    if (be.infusedAmount >= NECTAR_PER_POLLEN) {
                        be.infusedAmount -= NECTAR_PER_POLLEN;
                        be.currentPollen = ItemStack.EMPTY;
                    }
                }
            }
        }

        // Auto-output below
        if (be.outputTank.getFluidAmount() > 0) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (cap != null) {
                FluidStack toTransfer = be.outputTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) be.outputTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        if (wasInfusing != isInfusing) {
            level.setBlock(pos, state.setValue(InfuserBlock.INFUSING, isInfusing), 3);
        }
    }

    private boolean canInfuse() {
        return !currentPollen.isEmpty() &&
               nectarTank.getFluidAmount() > 0 &&
               outputTank.getFluidAmount() < outputTank.getCapacity();
    }

    @Nullable
    private Fluid getOutputFluidForPollen() {
        if (currentPollen.is(BeemancerItems.FIRE_POLLEN.get())) {
            return BeemancerFluids.FIRE_NECTAR_SOURCE.get();
        } else if (currentPollen.is(BeemancerItems.FROST_POLLEN.get())) {
            return BeemancerFluids.FROST_NECTAR_SOURCE.get();
        } else if (currentPollen.is(BeemancerItems.STORM_POLLEN.get())) {
            return BeemancerFluids.STORM_NECTAR_SOURCE.get();
        }
        return null;
    }

    public boolean insertPollen(ItemStack pollen) {
        if (currentPollen.isEmpty()) {
            currentPollen = pollen.copy();
            setChanged();
            return true;
        }
        return false;
    }

    public FluidTank getNectarTank() { return nectarTank; }
    public FluidTank getOutputTank() { return outputTank; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Nectar", nectarTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Output", outputTank.writeToNBT(registries, new CompoundTag()));
        if (!currentPollen.isEmpty()) {
            tag.put("Pollen", currentPollen.save(registries));
        }
        tag.putInt("Infused", infusedAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        nectarTank.readFromNBT(registries, tag.getCompound("Nectar"));
        outputTank.readFromNBT(registries, tag.getCompound("Output"));
        if (tag.contains("Pollen")) {
            currentPollen = ItemStack.parse(registries, tag.getCompound("Pollen")).orElse(ItemStack.EMPTY);
        }
        infusedAmount = tag.getInt("Infused");
    }
}
